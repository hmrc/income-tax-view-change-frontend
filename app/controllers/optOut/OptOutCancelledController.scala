/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optOut

import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.admin.ReportingFrequencyPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DateService
import views.html.optOut.OptOutCancelledView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutCancelledController @Inject()(
                                           val authorisedFunctions: FrontendAuthorisedFunctions,
                                           val auth: AuthActions,
                                           dateService: DateService,
                                           view: OptOutCancelledView
                                         )(
                                           implicit val appConfig: FrontendAppConfig,
                                           mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                         )

  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] =
    auth.individualOrAgentWithClient.async { implicit user =>
      val currentTaxYearStart = dateService.getCurrentTaxYear.startYear.toString
      val currentTaxYearEnd = dateService.getCurrentTaxYear.endYear.toString
      Future(Ok(
        view(user.isAgent(), currentTaxYearStart, currentTaxYearEnd)
      ))
    }
}