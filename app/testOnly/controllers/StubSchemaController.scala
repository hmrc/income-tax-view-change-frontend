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
import play.api.{Configuration, Environment}
import testOnly.connectors.DynamicStubConnector
import testOnly.forms.StubSchemaForm
import testOnly.models.SchemaModel
import testOnly.views.html.StubSchemaView
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utils.{AuthExchange, SessionBuilder}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StubSchemaController @Inject()(stubSchemaView: StubSchemaView)
                                    (implicit val appConfig: FrontendAppConfig,
                                     override val config: Configuration,
                                     override val env: Environment,
                                     implicit val mcc: MessagesControllerComponents,
                                     implicit val executionContext: ExecutionContext,
                                     val dynamicStubConnector: DynamicStubConnector
                                    ) extends BaseController with AuthRedirects with I18nSupport {

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(view(StubSchemaForm.stubSchemaForm)))
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      StubSchemaForm.stubSchemaForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        schema => {
          dynamicStubConnector.addSchema(schema).map(
            response => response.status match {
              case OK => Ok(view(StubSchemaForm.stubSchemaForm, showSuccess = true))
              case _ => InternalServerError(view(StubSchemaForm.stubSchemaForm.fill(schema), errorMessage = Some(response.body)))
            }
          )
        }
      )
  }

  val stubProxy: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SchemaModel](
      json => dynamicStubConnector.addSchema(json).map(
        response => response.status match {
          case OK => Ok(s"The following JSON was added to the stub: \n\n${Json.toJson(json)}")
          case _ => InternalServerError(response.body)
        }
      )
    )
  }

  val deleteAllProxy: Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.deleteAllSchemas().map(
      response => response.status match {
        case OK => Ok("Deleting All Schemas from the Stub...")
        case _ => InternalServerError(response.body)
      }
    )
  }
  // Logging page functionality
  val createLogin: Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.showLogin("login").map(
      response => response.status match {
        case OK => {
          val alteredBody = response.body.replace("/login", "/report-quarterly/income-and-expenses/view/test-only/submitLogin")
          Ok(alteredBody).as("text/html")
        }
        case _ => InternalServerError(response.body)
      }
    )
  }
  val postLogin: Action[AnyContent] = Action.async { implicit request =>
    val nino = request.body.asFormUrlEncoded.map(m => m("nino")).getOrElse(Seq.empty).mkString(" ")
    val isAgent: Option[String] = request.body.asFormUrlEncoded.map(m => m.getOrElse("Agent", Nil)).getOrElse(Seq.empty).headOption
    val redirectURL =
      if (isAgent.contains("true"))
        s"report-quarterly/income-and-expenses/view/test-only/stub-client/nino/${nino}/utr/"
      else
        "report-quarterly/income-and-expenses/view?origin=BTA"

    dynamicStubConnector.postLogin("login", nino, isAgent.getOrElse("false")).map(
      response => response.status match {
        case OK =>
          if (isAgent.contains("true")) {
            val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"
            val (bearer, auth, utr) = {
              val arr = response.body.split(";")
              (arr(0), arr(1), arr(2))
            }
            Redirect(homePage + utr)
              .withSession(
                SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
                  sessionAuthorityUri = auth)))
          } else {
            val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"
            val (bearer, auth) = {
              val arr = response.body.split(";")
              (arr(0), arr(1))
            }
            Redirect(homePage)
              .withSession(
                SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
                  sessionAuthorityUri = auth)))
          }
        case code =>
          Ok(response.body).as("text/html")
      }
    )
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
