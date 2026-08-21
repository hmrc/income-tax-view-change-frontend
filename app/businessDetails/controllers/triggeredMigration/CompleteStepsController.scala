/*
 * Copyright 2026 HM Revenue & Customs
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

import businessDetails.services.SessionService
import businessDetails.utils.TriggeredMigrationUtils
import com.google.inject.{Inject, Singleton}
import common.auth.AuthActions
import common.config.FrontendAppConfig
import businessDetails.views.html.triggeredMigration.CompleteStepsView
import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes
import common.models.incomeSourceDetails.TaxYear
import common.services.DateService
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompleteStepsController @Inject()(view: CompleteStepsView,
                                        val auth: AuthActions,
                                        dateService: DateService,
                                        sessionService: SessionService)
                                       (mcc: MessagesControllerComponents,
                                        implicit val appConfig: FrontendAppConfig,
                                        implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {




  def show(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent, triggeredMigrationPage = true).async { implicit user =>
      withTriggeredMigrationFS {
        val sessionId = hc.sessionId.map(_.value) getOrElse {
          throw new Exception("Missing sessionId in HeaderCarrier")
        }      

        sessionService.clearSession(sessionId)

        val currentTaxYear: TaxYear = dateService.getCurrentTaxYear
        val checkHmrcRecordsUrl: String = triggeredMigrationRoutes.CheckHmrcRecordsController.show(isAgent).url

        Future.successful(Ok(
          view(
            isAgent,
            appConfig.findOutHowLink,
            checkHmrcRecordsUrl,
            currentTaxYear
          ))
        )
      }
    }
}
