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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.triggeredMigration.CheckCompleteView
import utils.TriggeredMigrationUtils

import scala.concurrent.Future

@Singleton
class CheckCompleteController @Inject()(view: CheckCompleteView,
                                        val auth: AuthActions)
                                       (mcc: MessagesControllerComponents,
                                        implicit val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {

  def show(isAgent: Boolean): Action[AnyContent] = auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    withTriggeredMigrationFS {
      val compatibleSoftwareLink: String = appConfig.compatibleSoftwareLink
      val nextUpdatesLink: String =
        if (isAgent) controllers.routes.NextUpdatesController.showAgent().url
        else controllers.routes.NextUpdatesController.show().url
      Future.successful(Ok(view(isAgent, compatibleSoftwareLink, nextUpdatesLink)))
    }
  }
}
