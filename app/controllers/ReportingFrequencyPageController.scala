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

package controllers

import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.ReportingFrequencyViewModel
import models.admin.ReportingFrequencyPage
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DateService
import services.optout.OptOutService
import views.html.ReportingFrequencyView
import views.html.errorPages.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReportingFrequencyPageController @Inject()(
                                                  optOutService: OptOutService,
                                                  val authorisedFunctions: FrontendAuthorisedFunctions,
                                                  val auth: AuthActions,
                                                  dateService: DateService,
                                                  errorTemplate: ErrorTemplate,
                                                  view: ReportingFrequencyView
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

      for {
        (checks, optOutJourneyType) <- optOutService.nextUpdatesPageOptOutViewModels()
      } yield {
        if (isEnabled(ReportingFrequencyPage)) {

          val optOutUrl: Option[String] =
            optOutJourneyType.map {
              case singleYearModel: OptOutOneYearViewModel =>
                controllers.optOut.routes.ConfirmOptOutController.show(user.isAgent()).url
              case multiYearModel: OptOutMultiYearViewModel =>
                controllers.optOut.routes.OptOutChooseTaxYearController.show(user.isAgent()).url
            }

          Ok(view(
            ReportingFrequencyViewModel(
              isAgent = user.isAgent(),
              currentTaxYear = dateService.getCurrentTaxYear,
              nextTaxYear = dateService.getCurrentTaxYear.nextYear,
              optOutJourneyUrl = optOutUrl
            )
          ))
        } else {
          InternalServerError(
            errorTemplate(
              pageTitle = "standardError.heading",
              heading = "standardError.heading",
              message = "standardError.message",
              isAgent = user.isAgent()
            )
          )
        }
      }
    }
}