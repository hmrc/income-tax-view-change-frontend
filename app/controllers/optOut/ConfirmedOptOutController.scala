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
import enums._
import models.admin.ReportingFrequencyPage
import models.itsaStatus.ITSAStatus
import models.optout._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optout.{MultiYearOptOutProposition, OptOutService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optOut.ConfirmedOptOutView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedOptOutController @Inject()(val authActions: AuthActions,
                                          val view: ConfirmedOptOutView,
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

  private[controllers] def viewScenarioHandler()(implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]):
  Future[Either[ConfirmedOptOutViewScenariosError, ConfirmedOptOutViewScenarios]] = {

    for {
      optOutProposition <- optOutService.fetchOptOutProposition()
      chosenTaxYear <- optOutService.determineOptOutIntentYear()
    } yield {
      val isCurrentQuarterly = optOutProposition.isCurrentYearQuarterly
      val isNextQuarterly = optOutProposition.isNextYearQuarterly
      val isNextAnnual = optOutProposition.isNextYearAnnual
      val isCurrentAnnual = optOutProposition.isCurrentYearAnnual
      val isMandatedNext = optOutProposition.nextTaxYear.status == ITSAStatus.Mandated
      val isMandatedCurrent = optOutProposition.currentTaxYear.status == ITSAStatus.Mandated

      (chosenTaxYear, optOutProposition.optOutPropositionType) match {
        case (CurrentTaxYear, _) if isCurrentQuarterly && isMandatedNext =>
          Right(CurrentYearNYMandatedScenario)
        case (CurrentTaxYear, Some(MultiYearOptOutProposition(p))) if p.isCurrentYearQuarterly && p.isNextYearQuarterly =>
          Right(CurrentYearNYQuarterlyOrAnnualScenario)
        case (CurrentTaxYear, _) if isCurrentQuarterly && (isNextQuarterly || isNextAnnual) =>
          Right(CurrentYearNYQuarterlyOrAnnualScenario)
        case (NextTaxYear, _) if isCurrentAnnual && isNextQuarterly =>
          Right(NextYearCYAnnualScenario)
        case (NextTaxYear, _) if isCurrentQuarterly || isNextQuarterly || (isMandatedCurrent && isNextQuarterly) =>
          Right(NextYearCYMandatedOrQuarterlyScenario)
        case (NoChosenTaxYear | PreviousTaxYear, _) if
          (isCurrentAnnual && isNextAnnual) ||
            (isCurrentQuarterly && isNextAnnual) ||
            (isCurrentAnnual && isNextQuarterly) ||
            (isCurrentQuarterly && isNextQuarterly) =>
          Right(PreviousAndNoStatusValidScenario)
        case (PreviousTaxYear | CurrentTaxYear, _) =>
          Right(DefaultValidScenario)
        case _ =>
          Left(UnableToDetermineContent)
      }
    }
  }


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
            viewModel: Option[ConfirmedOptOutViewModel] <- optOutService.optOutConfirmedPageViewModel()
            viewScenarioContent: Either[ConfirmedOptOutViewScenariosError, ConfirmedOptOutViewScenarios] <- viewScenarioHandler()
            _ <- optOutService.updateJourneyStatusInSessionData(true)
          } yield {
            (viewScenarioContent, viewModel) match {
              case (Left(error), _) =>
                Logger("application").error(s"[ConfirmedOptOutController][show] Error, invalid Opt-out journey, error: $error")
                errorHandler(isAgent).showInternalServerError()
              case (Right(viewScenario), Some(viewModel)) =>
                Logger("application").debug(s"[ConfirmedOptOutController][show] Success, showing ConfirmedOptOutView for scenario: $viewScenario")
                Ok(view(
                  viewModel = viewModel,
                  isAgent = isAgent,
                  showReportingFrequencyContent = showReportingFrequencyContent,
                  confirmedOptOutViewScenarios = viewScenario,
                  selfAssessmentTaxReturnLink = appConfig.selfAssessmentTaxReturnLink,
                  compatibleSoftwareLink = appConfig.compatibleSoftwareLink
                ))
            }
          }
        }
      }
    }
}