/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import com.fasterxml.jackson.core.JsonParseException
import config.FrontendAppConfig
import controllers.BaseController
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.{Configuration, Environment}
import testOnly.connectors.DynamicStubConnector
import testOnly.forms.StubSchemaForm
import testOnly.models.SchemaModel
import uk.gov.hmrc.auth.frontend.Redirects
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Try

@Singleton
class StubSchemaController @Inject()(implicit val appConfig: FrontendAppConfig,
                                     override val config: Configuration,
                                     override val env: Environment,
                                     implicit val messagesApi: MessagesApi,
                                     val dynamicStubConnector: DynamicStubConnector
                                  ) extends BaseController with Redirects {

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(view(StubSchemaForm.stubSchemaForm)))
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      StubSchemaForm.stubSchemaForm.bindFromRequest.fold(
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

  private def view(form: Form[SchemaModel], showSuccess: Boolean = false, errorMessage: Option[String] = None)(implicit request: Request[AnyContent]) =
    testOnly.views.html.stubSchemaView(
      form,
      testOnly.controllers.routes.StubSchemaController.submit(),
      showSuccess,
      errorMessage
    )
}
