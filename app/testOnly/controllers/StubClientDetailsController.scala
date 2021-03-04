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
import javax.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.connectors.MatchingStubConnector
import testOnly.forms.StubClientDetailsForm
import testOnly.models.StubClientDetailsModel
import testOnly.views.html.injected.StubClientDetails
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class StubClientDetailsController @Inject()(stubClientDetails: StubClientDetails,
                                            matchingStubConnector: MatchingStubConnector)
                                           (implicit mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig,
                                            ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def form: Form[StubClientDetailsModel] = StubClientDetailsForm.clientDetailsForm.fill(
    StubClientDetailsModel(
      nino = "AA000000A",
      utr = "1234567890",
      status = OK
    )
  )

  def show: Action[AnyContent] = Action { implicit req =>
    Ok(stubClientDetails(
      clientDetailsForm = form,
      postAction = testOnly.controllers.routes.StubClientDetailsController.submit()
    ))
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    StubClientDetailsForm.clientDetailsForm.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(stubClientDetails(
        clientDetailsForm = hasErrors,
        postAction = testOnly.controllers.routes.StubClientDetailsController.submit()
      ))), { data =>
        matchingStubConnector.stubClient(data) map { response =>
          Logger.info(s"[StubClientDetailsController][submit] - matching stub, status: ${response.status}, body: ${response.body}")
          Redirect(controllers.agent.routes.EnterClientsUTRController.show())
        }
      }
    )
  }

}
