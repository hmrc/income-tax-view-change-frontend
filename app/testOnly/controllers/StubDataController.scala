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

import common.config.FrontendAppConfig
import common.controllers.BaseController
import hub.auth.AuthActions
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import testOnly.connectors.DynamicStubConnector
import testOnly.forms.StubDataForm
import testOnly.models.DataModel
import testOnly.views.html.StubDataView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StubDataController @Inject()(authActions: AuthActions,
                                   stubDataView: StubDataView)
                                  (implicit val appConfig: FrontendAppConfig,
                                   val mcc: MessagesControllerComponents,
                                   val executionContext: ExecutionContext,
                                   val dynamicStubConnector: DynamicStubConnector
                                  ) extends BaseController with I18nSupport with Logging {

  def show(isNewContextRoot: Boolean): Action[AnyContent] =
    authActions.retrieveFeatureSwitchesAndCheckContextRootIfReq().async { implicit request =>
      Future.successful(Ok(view(StubDataForm.stubDataForm, isNewContextRoot)))
    }

  def submit(isNewContextRoot: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      StubDataForm.stubDataForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isNewContextRoot))),
        schema => {
          dynamicStubConnector.addData(schema)
            .map(
              response => response.status match {
                case OK => Ok(view(StubDataForm.stubDataForm, isNewContextRoot, showSuccess = true))
                case _ => InternalServerError(view(StubDataForm.stubDataForm.fill(schema), isNewContextRoot, errorResponse = Some(response.body)))
              }
            )
        }
      )
  }

  def stubProxy(isNewContextRoot: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[DataModel](
      json => {
        dynamicStubConnector.addData(json).map(
          response => response.status match {
            case OK => Ok(s"The following JSON was added to the stub: \n\n${Json.toJson(json)}")
            case _ =>
              logger.error(s"[stubProxy] ${response.body}")
              InternalServerError(response.body)
          }
        )
      }
    )
  }

  def deleteAllProxy(isNewContextRoot: Boolean): Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.deleteAllData().map(
      response => response.status match {
        case OK => Ok("Delete All Data from the Stub...")
        case _ => InternalServerError(response.body)
      }
    )
  }

  private def view(form: Form[DataModel],
                   isNewContextRoot: Boolean,
                   showSuccess: Boolean = false,
                   errorResponse: Option[String] = None
                  )(implicit request: Request[AnyContent]) =
    stubDataView(
      form,
      testOnly.controllers.routes.StubDataController.submit(isNewContextRoot),
      showSuccess,
      errorResponse
    )
}
