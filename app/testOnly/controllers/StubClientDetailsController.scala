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

import common.auth.actions.FeatureSwitchRetrievalAction
import common.config.FrontendAppConfig
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.connectors.MatchingStubConnector
import testOnly.forms.StubClientDetailsForm
import testOnly.models.StubClientDetailsModel
import testOnly.views.html.StubClientDetails
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubClientDetailsController @Inject()(stubClientDetails: StubClientDetails,
                                            featureSwitchRetrievalAction: FeatureSwitchRetrievalAction,
                                            matchingStubConnector: MatchingStubConnector)
                                           (implicit mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig,
                                            ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with Logging {

  def form: Form[StubClientDetailsModel] = StubClientDetailsForm.clientDetailsForm.fill(
    StubClientDetailsModel(
      nino = "AA888888A",
      utr = "1234567890",
      status = OK
    )
  )

  def show(isNewContextRoot: Boolean): Action[AnyContent] = Action { implicit req =>
    Ok(stubClientDetails(
      clientDetailsForm = form,
      postAction = testOnly.controllers.routes.StubClientDetailsController.submit(isNewContextRoot)
    ))
  }

  def submitWithParams(nino: String, utr: String, isNewContextRoot: Boolean): Action[AnyContent] = featureSwitchRetrievalAction.async { implicit request =>
    for {
      response <- matchingStubConnector.stubClient(StubClientDetailsModel(nino, utr, OK))
      newhubContextRouteEnabled <- featureSwitchService.isEnabled(NewHubContextRootEnabled)
    } yield {
      val redirectUrl = if (newhubContextRouteEnabled)
        hub.v2.controllers.agent.routes.EnterClientsUTRController.showWithUtr(utr)
      else
        hub.v1.controllers.agent.routes.EnterClientsUTRController.showWithUtr(utr)
      Redirect(redirectUrl)
    }
  }

  def submit(isNewContextRoot: Boolean): Action[AnyContent] = Action.async { implicit request =>
    StubClientDetailsForm.clientDetailsForm.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(stubClientDetails(
        clientDetailsForm = hasErrors,
        postAction = testOnly.controllers.routes.StubClientDetailsController.submit(isNewContextRoot)
      ))), { data =>
        for {
          response <- matchingStubConnector.stubClient(data)
          newhubContextRouteEnabled <- featureSwitchService.isEnabled(NewHubContextRootEnabled)
        } yield {
          logger.info(s"[submit] matching stub, status: ${response.status}, body: ${response.body}")
          val redirectUrl = if (newhubContextRouteEnabled)
            hub.v2.controllers.agent.routes.EnterClientsUTRController.show()
          else
            hub.v1.controllers.agent.routes.EnterClientsUTRController.show()
          Redirect(redirectUrl)
        }
      }
    )
  }

}
