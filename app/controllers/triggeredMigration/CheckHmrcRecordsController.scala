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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.triggeredMigration.TriggeredMigrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.TriggeredMigrationUtils
import views.html.triggeredMigration.CheckHmrcRecordsView

import scala.concurrent.Future

@Singleton
class CheckHmrcRecordsController @Inject()(view: CheckHmrcRecordsView,
                                           val auth: AuthActions,
                                           triggeredMigrationService: TriggeredMigrationService)
                                          (mcc: MessagesControllerComponents,
                                           implicit val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {

  def show(isAgent: Boolean): Action[AnyContent] = auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    withTriggeredMigrationFS {
      val viewModel = triggeredMigrationService.getCheckHmrcRecordsViewModel(user.incomeSources)

      Future.successful(Ok(view(viewModel)))
    }
  }
}
