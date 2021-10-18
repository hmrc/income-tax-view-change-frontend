/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.{I18nSupport}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.{Configuration, Environment}
import testOnly.connectors.DynamicStubConnector
import testOnly.forms.StubDataForm
import testOnly.models.DataModel
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import testOnly.views.html.StubDataView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StubDataController @Inject()(stubDataView: StubDataView)
                                  (implicit val appConfig: FrontendAppConfig,
                                   override val config: Configuration,
                                   override val env: Environment,
                                   implicit val mcc: MessagesControllerComponents,
                                   implicit val executionContext: ExecutionContext,
                                   val dynamicStubConnector: DynamicStubConnector
                                  ) extends BaseController with AuthRedirects with I18nSupport {

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(view(StubDataForm.stubDataForm)))
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      StubDataForm.stubDataForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        schema => {
          dynamicStubConnector.addData(schema).map(
            response => response.status match {
              case OK => Ok(view(StubDataForm.stubDataForm, showSuccess = true))
              case _ => InternalServerError(view(StubDataForm.stubDataForm.fill(schema), errorResponse = Some(response.body)))
            }
          )
        }
      )
  }

  val stubProxy: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[DataModel](
      json => dynamicStubConnector.addData(json).map(
        response => response.status match {
          case OK => Ok(s"The following JSON was added to the stub: \n\n${Json.toJson(json)}")
          case _ => InternalServerError(response.body)
        }
      )
    )
  }

  val deleteAllProxy: Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.deleteAllData().map(
      response => response.status match {
        case OK => Ok("Delete All Data from the Stub...")
        case _ => InternalServerError(response.body)
      }
    )
  }

  private def view(form: Form[DataModel],
                   showSuccess: Boolean = false,
                   errorResponse: Option[String] = None
                  )(implicit request: Request[AnyContent]) =
    stubDataView(
      form,
      testOnly.controllers.routes.StubDataController.submit(),
      showSuccess,
      errorResponse
    )
}
