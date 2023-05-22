/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testOnly.controllers

import config.FrontendAppConfig
import controllers.BaseController
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import testOnly.connectors.{CustomAuthConnector, DynamicStubConnector}
import testOnly.forms.StubSchemaForm
import testOnly.models.{Nino, PostedUser, SchemaModel, UserRecord}
import testOnly.utils.{FileUtil, UserRepository}
import testOnly.views.html.StubSchemaView
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utils.{AuthExchange, SessionBuilder}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import testOnly.views.html.LoginPage

@Singleton
class CustomLoginController @Inject()(stubSchemaView: StubSchemaView)
                                     (implicit val appConfig: FrontendAppConfig,
                                      override val config: Configuration,
                                      override val env: Environment,
                                      implicit val mcc: MessagesControllerComponents,
                                      implicit val executionContext: ExecutionContext,
                                      userRepository: UserRepository,
                                      loginPage: LoginPage,
                                      val dynamicStubConnector: DynamicStubConnector,
                                      val customAuthConnector: CustomAuthConnector
                                     ) extends BaseController with AuthRedirects with I18nSupport {

  // Logging page functionality
  val showLogin: Action[AnyContent] = Action.async { implicit request =>

    userRepository.findAll().map(userRecords =>
      Ok(loginPage(routes.CustomLoginController.postLogin, userRecords))
    )

  }

  val postLogin: Action[AnyContent] = Action.async { implicit request =>
    PostedUser.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(s"Invalid form submission: $formWithErrors")),
      (postedUser: PostedUser) => {
        userRepository.findUser(postedUser.nino).flatMap(
          user =>
            customAuthConnector.login(Nino(user.nino), postedUser.isAgent).map {
              case (authExchange, _) =>
                val (bearer, auth) = (authExchange.bearerToken, authExchange.sessionAuthorityUri)
                val redirectURL = if (postedUser.isAgent)
                  s"report-quarterly/income-and-expenses/view/test-only/stub-client/nino/${user.nino}/utr/" + user.utr
                else
                  "report-quarterly/income-and-expenses/view?origin=BTA"
                val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"

                Redirect(homePage)
                  .withSession(
                    SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
                      sessionAuthorityUri = auth)))

              case code =>
                //            Ok(response.body).as("text/html")
                InternalServerError("something went wrong..")

            }
        )
      })
    //      response => response.status match {
    //        case OK =>
    //          if (isAgent.contains("true")) {
    //            val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"
    //            val (bearer, auth, utr) = {
    //              val arr = response.body.split(";")
    //              (arr(0), arr(1), arr(2))
    //            }
    //            Redirect(homePage + utr)
    //              .withSession(
    //                SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
    //                  sessionAuthorityUri = auth)))
    //          } else {
    //            val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"
    //            val (bearer, auth) = {
    //              val arr = response.body.split(";")
    //              (arr(0), arr(1))
    //            }
    //            Redirect(homePage)
    //              .withSession(
    //                SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
    //                  sessionAuthorityUri = auth)))
    //          }
    //        case code =>
    //          Ok(response.body).as("text/html")

  }

  val showCss: Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.showLogin("hmrc-frontend/assets/hmrc-frontend-5.19.0.min.css").map(
      response => response.status match {
        case OK => Ok(response.body).as("text/css")
        case _ => InternalServerError(response.body)
      }
    )
  }

  private def view(form: Form[SchemaModel], showSuccess: Boolean = false, errorMessage: Option[String] = None)(implicit request: Request[AnyContent]) =
    stubSchemaView(
      form,
      testOnly.controllers.routes.StubSchemaController.submit,
      showSuccess,
      errorMessage
    )

}
