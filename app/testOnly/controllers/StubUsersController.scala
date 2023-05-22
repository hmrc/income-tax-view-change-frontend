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

import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import play.twirl.api.HtmlFormat
import testOnly.connectors.DesSimulatorConnector
import testOnly.forms.UserModelForm
import testOnly.models.{UserModel, UserRecord}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import testOnly.views.html.StubUsersView

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import testOnly.utils.UserRepository

class StubUsersController @Inject()(stubUsersView: StubUsersView)
                                   (implicit val appConfig: FrontendAppConfig,
                                    override val config: Configuration,
                                    override val env: Environment,
                                    implicit val mcc: MessagesControllerComponents,
                                    implicit val executionContext: ExecutionContext,
                                    val desSimulatorConnector: DesSimulatorConnector,
                                    userRepository: UserRepository,
                                   ) extends BaseController with AuthRedirects with I18nSupport {

  def show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(view(UserModelForm.userModelForm)))
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    UserModelForm.userModelForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(view(errors))),
      success => desSimulatorConnector.stubUser(success) map {
        case r if r.status == CREATED => Created(view(UserModelForm.userModelForm, Some("User created")))
        case _ => Conflict(view(UserModelForm.userModelForm, Some("User not created")))
      }
    )
  }

  def stubUsers: Action[JsValue] = Action.async(parse.json) { implicit request =>
//    Logger("application").warn("body:" + request.body)
    withJsonBody[UserRecord](
      userRecord => {
        Logger("application").info("userRecord:" + userRecord)
        val um = UserModel.toUserModel(userRecord)
        desSimulatorConnector.stubUser(um).flatMap(
          desResponse => {
            userRepository.addUser(userRecord).flatMap(
              _ => {
                desResponse.status match {
                  case CREATED =>
                    Logger("application").info("adding user to mongo..")
                    Future.successful(Ok(s"The following USER was added to the stub: \n\n${Json.toJson(um)}"))
                  case _ =>
                    Logger("application").error(desResponse.body)
                    Future.successful(InternalServerError(desResponse.body))
                }
              }
            )

          }
        )

      }
    )
  }

  def view(form: Form[UserModel], result: Option[String] = None)(implicit request: Request[AnyContent]): HtmlFormat.Appendable = {
    stubUsersView(form, testOnly.controllers.routes.StubUsersController.submit, result)
  }
}
