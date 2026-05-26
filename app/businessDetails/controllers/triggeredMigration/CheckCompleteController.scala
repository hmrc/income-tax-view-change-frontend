/*
 * Copyright 2025 HM Revenue & Customs
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

package businessDetails.controllers.triggeredMigration

import businessDetails.utils.TriggeredMigrationUtils
import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import businessDetails.views.html.triggeredMigration.CheckCompleteView
import common.auth.AuthActions
import common.config.FrontendAppConfig
import common.services.SessionService

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import obligations.controllers.{routes => obligationsRoutes}

@Singleton
class CheckCompleteController @Inject()(view: CheckCompleteView,
                                        val auth: AuthActions,
                                        sessionService: SessionService)
                                       (mcc: MessagesControllerComponents,
                                        implicit val appConfig: FrontendAppConfig,
                                        implicit val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {

  private def nextUpdatesLink(isAgent: Boolean): String =
    if (isAgent) obligationsRoutes.NextUpdatesController.showAgent().url
    else obligationsRoutes.NextUpdatesController.show().url

  def show(isAgent: Boolean): Action[AnyContent] = auth.asMTDIndividualOrAgentWithClient(isAgent, triggeredMigrationPage = true).async { implicit user =>
    withTriggeredMigrationFS {
      val compatibleSoftwareLink: String = appConfig.compatibleSoftwareLink

      val sessionId = hc.sessionId.map(_.value) getOrElse {
        throw new Exception("Missing sessionId in HeaderCarrier")
      }

      sessionService.clearSession(sessionId)

      Future.successful(Ok(
        view(
          isAgent,
          compatibleSoftwareLink,
          nextUpdatesLink(isAgent),
          postAction = routes.CheckCompleteController.submit(isAgent)
        ))
      )
    }
  }

  def submit(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      @unused
      val compatibleSoftwareLink: String = appConfig.compatibleSoftwareLink
      withTriggeredMigrationFS {

        if (isAgent) {
          Future.successful(Redirect(hub.controllers.routes.HomeController.showAgent()))
        } else {
          Future.successful(Redirect(hub.controllers.routes.HomeController.show()))
        }
      }
    }
}
