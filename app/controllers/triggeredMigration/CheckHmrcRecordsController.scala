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

package controllers.triggeredMigration

import auth.authV2.AuthActions
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import enums.TriggeredMigration.TriggeredMigrationCeased
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import services.triggeredMigration.TriggeredMigrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.TriggeredMigrationUtils
import views.html.triggeredMigration.CheckHmrcRecordsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckHmrcRecordsController @Inject()(view: CheckHmrcRecordsView,
                                           val auth: AuthActions,
                                           triggeredMigrationService: TriggeredMigrationService,
                                           sessionService: SessionService)
                                          (mcc: MessagesControllerComponents,
                                           implicit val appConfig: FrontendAppConfig,
                                           implicit val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {


  def show(isAgent: Boolean, state: Option[String] = None): Action[AnyContent] = auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    //TODO: Redirect the user back to the triggered migration page if they press the backlink (Requires the data from the API to know if the user has to stay in the journey)
    withTriggeredMigrationFS {
      if (state.isDefined) {
        val sessionId = hc.sessionId.getOrElse {
          throw new RuntimeException("No session ID found when attempting to clear session")
        }
        sessionService.clearSession(sessionId.value)
      }

      val viewModel = triggeredMigrationService.getCheckHmrcRecordsViewModel(user.incomeSources, state)

      Future.successful(Ok(view(viewModel, isAgent)))
    }
  }
}

