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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.ChosenTaxYear
import models.admin.ReportingFrequencyPage
import models.optout.ConfirmedOptOutViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.optout.{OptOutProposition, OptOutService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ReportingObligationsUtils
import viewUtils.ConfirmedOptOutViewUtils
import views.html.optOut.ConfirmedOptOut

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ConfirmedOptOutController @Inject()(val authActions: AuthActions,
                                          confirmedOptOutViewUtils: ConfirmedOptOutViewUtils,
                                          val view: ConfirmedOptOut,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val optOutService: OptOutService
                                         )
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext
                                         )
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with ReportingObligationsUtils {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutFS {
        withRecover(isAgent) {

          val showReportingFrequencyContent = isEnabled(ReportingFrequencyPage)

          for {
            proposition: OptOutProposition <- optOutService.fetchOptOutProposition()
            chosenTaxYear: ChosenTaxYear <- optOutService.determineOptOutIntentYear()
            viewModel: Option[ConfirmedOptOutViewModel] <- optOutService.optOutConfirmedPageViewModel()
            submitYourTaxReturnContent: Option[Html] =
              confirmedOptOutViewUtils
                .submitYourTaxReturnContent(
                  `itsaStatusCY-1` = proposition.previousTaxYear.status,
                  `itsaStatusCY` = proposition.currentTaxYear.status,
                  `itsaStatusCY+1` = proposition.nextTaxYear.status,
                  chosenTaxYear = chosenTaxYear,
                  isMultiYear = proposition.isMultiYearOptOut,
                  isPreviousYearCrystallised = proposition.previousTaxYear.crystallised
                )
          } yield {
            viewModel match {
              case Some(viewModel) =>
                Ok(view(viewModel, isAgent, showReportingFrequencyContent, submitYourTaxReturnContent))
              case None =>
                Logger("application").error(s"error, invalid Opt-out journey")
                errorHandler(isAgent).showInternalServerError()
            }
          }
        }
      }
  }
}