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

package hub.controllers

import common.auth.{AuthActions, MtdItUser}
import common.config.*
import common.config.featureswitch.*
import common.enums.MTDSupportingAgent
import common.models.admin.*
import common.models.core.Nino
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus
import common.services.{AuditingService, DateServiceInterface, ITSAStatusService}
import common.utils.sessionUtils.SessionKeys
import financials.services.*
import hub.audit.models.HomeAudit
import hub.models.homePage.*
import hub.services.PenaltyDetailsService
import hub.utils.HomePageUtils
import models.financialDetails.*
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import hub.services.NextUpdatesService
import obligations.services.reportingObligations.optOut.OptOutService
import obligations.services.reportingObligations.signUp.SignUpService
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: hub.views.html.HomeView,
                               val newHomeRecentActivityView: hub.views.html.newHomePage.NewHomeRecentActivityView,
                               val newHomeOverviewView: hub.views.html.newHomePage.NewHomeOverviewView,
                               val newHomeHelpView: hub.views.html.newHomePage.NewHomeHelpView,
                               val primaryAgentHomeView: hub.views.html.agent.PrimaryAgentHomeView,
                               val supportingAgentHomeView: hub.views.html.agent.SupportingAgentHomeView,
                               val authActions: AuthActions,
                               val nextUpdatesService: NextUpdatesService,
                               val financialDetailsService: FinancialDetailsService,
                               val dateService: DateServiceInterface,
                               val whatYouOweService: WhatYouOweService,
                               val ITSAStatusService: ITSAStatusService,
                               val penaltyDetailsService: PenaltyDetailsService,
                               val creditService: CreditService,
                               val signUpService: SignUpService,
                               val optOutService: OptOutService,
                               auditingService: AuditingService)
                              (implicit
                               val ec: ExecutionContext,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching with HomePageUtils {

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividualWithIncomeSources().async {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClientWithIncomeSources().async {
    implicit mtdItUser =>
      handleShowRequest(origin)
  }

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isEnabled(NewHomePage)) {
      handleYourTasks(origin, user.isAgent)
    } else {
      handleOldHomePage(origin)
    }
  }

  private def handleOldHomePage(origin: Option[String] = None)
                               (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate]) if user.usersRole == MTDSupportingAgent =>
        buildHomePageForSupportingAgent(nextUpdatesDueDates)
      case Right(nextUpdatesDueDates: Seq[LocalDate]) =>
        buildHomePage(nextUpdatesDueDates, origin)
      case Left(ex) =>
        Logger("application").error(s"Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
        Future.successful(handleErrorGettingDueDates(user.isAgent))
    }
  }

  private def buildHomePageForSupportingAgent(nextUpdatesDueDates: Seq[LocalDate])
                                             (implicit user: MtdItUser[_]): Future[Result] = {

    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
      (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- getNextDueDatesIfEnabled()
      _ <- signUpService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
    } yield {
      val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates,
        currentDate = dateService.getCurrentDate,
        currentYearITSAStatus = currentITSAStatus,
        nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
        nextTaxReturnDueDate = nextTaxReturnDueDate)

      val yourBusinessesTileViewModel = YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)
      val yourReportingObligationsTileViewModel = YourReportingObligationsTileViewModel(currentTaxYear, currentITSAStatus)
      val userIsCYPlusOne = currentITSAStatus == ITSAStatus.NoStatus

      auditingService.extendedAudit(
        HomeAudit.applySupportingAgent(user,
          nextUpdatesTileViewModel.getNumberOfOverdueObligations,
          nextUpdatesTileViewModel.getNextDeadline,
          userIsCYPlusOne
        )
      )

      Ok(
        supportingAgentHomeView(
          yourBusinessesTileViewModel,
          nextUpdatesTileViewModel,
          yourReportingObligationsTileViewModel
        )
      )
    }
  }

  private def buildHomePage(nextUpdatesDueDates: Seq[LocalDate], origin: Option[String])
                           (implicit user: MtdItUser[_]): Future[Result] = {

    val getCurrentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val getCurrentDate = dateService.getCurrentDate
    val currentTaxYear = TaxYear(getCurrentTaxYearEnd - 1, getCurrentTaxYearEnd)

    for {
      credits <- creditService.getAllCredits
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
      paymentsDue = getDueDates(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      dunningLockExists = hasDunningLock(unpaidCharges)
      outstandingChargesModel <- getOutstandingChargesModel(unpaidCharges)
      outstandingChargeDueDates = getRelevantDates(outstandingChargesModel)
      overDuePaymentsCount = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      accruingInterestPaymentsCount = NextPaymentsTileViewModel.paymentsAccruingInterestCount(unpaidCharges, getCurrentDate)
      currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
      penaltiesCount <- penaltyDetailsService.getPenaltiesCount(isEnabled(PenaltiesBackendEnabled))
      paymentsDueMerged = mergePaymentsDue(paymentsDue, outstandingChargeDueDates)
      mandation <- ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandated)
      (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- getNextDueDatesIfEnabled()
      _ <- signUpService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
    } yield {

      val nextUpdatesTileViewModel =
        NextUpdatesTileViewModel(
          dueDates = nextUpdatesDueDates,
          currentDate = getCurrentDate,
          currentYearITSAStatus = currentITSAStatus,
          nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
          nextTaxReturnDueDate = nextTaxReturnDueDate
        )

      val penaltiesAndAppealsTileViewModel: PenaltiesAndAppealsTileViewModel =
        PenaltiesAndAppealsTileViewModel(isEnabled(PenaltiesAndAppeals), penaltyDetailsService.getPenaltySubmissionFrequency(currentITSAStatus), penaltiesCount)

      val paymentCreditAndRefundHistoryTileViewModel =
        PaymentCreditAndRefundHistoryTileViewModel(credits, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds), user.incomeSources.yearOfMigration.isDefined)

      val yourBusinessesTileViewModel =
        YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)

      val returnsTileViewModel =
        ReturnsTileViewModel(currentTaxYear, isEnabled(ITSASubmissionIntegration))

      val yourReportingObligationsTileViewModel =
        YourReportingObligationsTileViewModel(currentTaxYear, currentITSAStatus)

      NextPaymentsTileViewModel(paymentsDueMerged, overDuePaymentsCount, accruingInterestPaymentsCount).verify match {

        case Right(viewModel: NextPaymentsTileViewModel) =>
          val homeViewModel = HomePageViewModel(
            utr = user.saUtr,
            nextPaymentsTileViewModel = viewModel,
            returnsTileViewModel = returnsTileViewModel,
            nextUpdatesTileViewModel = nextUpdatesTileViewModel,
            paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel,
            yourBusinessesTileViewModel = yourBusinessesTileViewModel,
            yourReportingObligationsTileViewModel = yourReportingObligationsTileViewModel,
            penaltiesAndAppealsTileViewModel = penaltiesAndAppealsTileViewModel,
            dunningLockExists = dunningLockExists,
            origin = origin
          )

          val mandationStatus =
            if (mandation) SessionKeys.mandationStatus -> "on"
            else SessionKeys.mandationStatus -> "off"

          val userIsCYPlusOne = currentITSAStatus == ITSAStatus.NoStatus

          auditingService.extendedAudit(
            HomeAudit(
              user,
              paymentsDueMerged,
              overDuePaymentsCount,
              nextUpdatesTileViewModel.getNumberOfOverdueObligations,
              nextUpdatesTileViewModel.getNextDeadline,
              userIsCYPlusOne
            )
          )

          if (user.isAgent) {
            Ok(primaryAgentHomeView(homeViewModel)).addingToSession(mandationStatus)
          } else {
            Ok(homeView(homeViewModel)).addingToSession(mandationStatus)
          }
        case Left(ex: Throwable) =>
          Logger("application").error(s"Unable to create the view model ${ex.getMessage} - ${ex.getCause}")
          handleErrorGettingDueDates(user.isAgent)
      }
    }
  }

  private def getOutstandingChargesModel(unpaidCharges: List[FinancialDetailsResponseModel])
                                        (implicit user: MtdItUser[_]): Future[List[OutstandingChargeModel]] =
    whatYouOweService.getWhatYouOweChargesList(
      unpaidCharges,
      isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
      isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
      mainChargeIsNotPaidFilter
    ) map {
      case WhatYouOweChargesList(_, _, Some(OutstandingChargesModel(outstandingCharges)), _) =>
        outstandingCharges.filter(_.isBalancingChargeDebit)
          .filter(_.relevantDueDate.isDefined)
      case _ => Nil
    }

  private def hasDunningLock(financialDetails: List[FinancialDetailsResponseModel]): Boolean =
    financialDetails
      .collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }
      .getOrElse(false)


  private def handleErrorGettingDueDates(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }

  private def getNextDueDatesIfEnabled()
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    nextUpdatesService.getNextDueDates()
  }

  private def handleYourTasks(@unused origin: Option[String] = None, isAgent: Boolean)
                             (implicit  @unused user: MtdItUser[_]): Future[Result] = {
    if(isAgent){
      Future.successful(Redirect(hub.controllers.newHomePage.routes.HandleYourTasksController.showAgent()))
    }else {
     Future.successful(Redirect(hub.controllers.newHomePage.routes.HandleYourTasksController.show()))
    }
  }

  def handleOverview(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user => {
      for {
        ctaViewModel <- whatYouOweService.claimToAdjustViewModel(Nino(user.nino))
        credits <- creditService.getAllCredits
        unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
        chargeItem = getChargeList(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      }
      yield {
        Ok(newHomeOverviewView(origin, user.isSupportingAgent, dateService.getCurrentTaxYear,
          yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent),
          helpUrl(origin, isAgent), unpaidCharges.isEmpty, credits.availableCreditInAccount, ctaViewModel, chargeItem,
          isEnabled(PenaltiesAndAppeals), isEnabled(RecentActivity), isEnabled(CreditsRefundsRepay), isEnabled(MortgageEvidence)))
      }
    }
  }

  def handleHelp(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      Future.successful(Ok(newHomeHelpView(origin, yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent), helpUrl(origin, isAgent), isEnabled(RecentActivity))))
  }

  private def getChargeList(unpaidCharges: List[FinancialDetailsResponseModel], isFilterOutCodedPoasEnabled: Boolean, penaltiesEnabled: Boolean): List[ChargeItem] = {

    val chargesList =
      unpaidCharges.collect {
        case fdm: FinancialDetailsModel => fdm
      }
    whatYouOweService.getFilteredChargesList(
      financialDetailsList = chargesList,
      isFilterCodedOutPoasEnabled = isFilterOutCodedPoasEnabled,
      isPenaltiesEnabled = penaltiesEnabled,
      remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot = mainChargeIsNotPaidFilter)
  }

  private def getCurrentITSAStatus(taxYear: TaxYear)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[ITSAStatus.ITSAStatus] = {
    ITSAStatusService.getStatusTillAvailableFutureYears(taxYear.previousYear).map(_.view.mapValues(_.status)
      .toMap
      .withDefaultValue(ITSAStatus.NoStatus)
    ).map(detail => detail(taxYear))
  }
}
