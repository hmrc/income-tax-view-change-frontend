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

package controllers

import audit.AuditingService
import audit.models.HomeAudit
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.*
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.sessionUtils.SessionKeys
import enums.MTDSupportingAgent
import models.admin.*
import models.financialDetails.{ChargeItem, FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.homePage.*
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.newHomePage.*
import models.obligations.NextUpdatesTileViewModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, *}
import services.*
import services.optIn.OptInService
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.HomeView,
                               val newHomeYourTasksView: views.html.NewHomeYourTasksView,
                               val newHomeRecentActivityView: views.html.NewHomeRecentActivityView,
                               val newHomeOverviewView: views.html.NewHomeOverviewView,
                               val newHomeHelpView: views.html.NewHomeHelpView,
                               val primaryAgentHomeView: views.html.agent.PrimaryAgentHomeView,
                               val supportingAgentHomeView: views.html.agent.SupportingAgentHomeView,
                               val authActions: AuthActions,
                               val nextUpdatesService: NextUpdatesService,
                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                               val financialDetailsService: FinancialDetailsService,
                               val dateService: DateServiceInterface,
                               val whatYouOweService: WhatYouOweService,
                               val ITSAStatusService: ITSAStatusService,
                               val penaltyDetailsService: PenaltyDetailsService,
                               val optInService: OptInService,
                               val optOutService: OptOutService,
                               auditingService: AuditingService)
                              (implicit
                               val ec: ExecutionContext,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async {
    implicit mtdItUser =>
      handleShowRequest(origin)
  }

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isEnabled(NewHomePage)) {
      handleYourTasks(origin, user.isAgent())
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
        Future.successful(handleErrorGettingDueDates(user.isAgent()))
    }
  }

  private def buildHomePageForSupportingAgent(nextUpdatesDueDates: Seq[LocalDate])
                                             (implicit user: MtdItUser[_]): Future[Result] = {

    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
      (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- getNextDueDatesIfEnabled()
      _ <- optInService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
    } yield {
      val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates,
        currentDate = dateService.getCurrentDate,
        isReportingFrequencyEnabled = isEnabled(ReportingFrequencyPage),
        showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
        currentYearITSAStatus = currentITSAStatus,
        nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
        nextTaxReturnDueDate = nextTaxReturnDueDate)

      val yourBusinessesTileViewModel = models.homePage.YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)
      val yourReportingObligationsTileViewModel = models.homePage.YourReportingObligationsTileViewModel(currentTaxYear, isEnabled(ReportingFrequencyPage), currentITSAStatus)

      auditingService.extendedAudit(HomeAudit.applySupportingAgent(user, nextUpdatesTileViewModel))
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
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
      paymentsDue = getDueDates(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      dunningLockExists = hasDunningLock(unpaidCharges)
      outstandingChargesModel <- getOutstandingChargesModel(unpaidCharges)
      outstandingChargeDueDates = getRelevantDates(outstandingChargesModel)
      overDuePaymentsCount = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      accruingInterestPaymentsCount = models.homePage.NextPaymentsTileViewModel.paymentsAccruingInterestCount(unpaidCharges, getCurrentDate)
      currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
      penaltiesCount <- penaltyDetailsService.getPenaltiesCount(isEnabled(PenaltiesBackendEnabled))
      paymentsDueMerged = mergePaymentsDue(paymentsDue, outstandingChargeDueDates)
      mandation <- ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandated)
      (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- getNextDueDatesIfEnabled()
      _ <- optInService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
    } yield {

      val nextUpdatesTileViewModel =
        NextUpdatesTileViewModel(
          dueDates = nextUpdatesDueDates,
          currentDate = getCurrentDate,
          isReportingFrequencyEnabled = isEnabled(ReportingFrequencyPage),
          showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
          currentYearITSAStatus = currentITSAStatus,
          nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
          nextTaxReturnDueDate = nextTaxReturnDueDate
        )

      val penaltiesAndAppealsTileViewModel: models.homePage.PenaltiesAndAppealsTileViewModel =
        models.homePage.PenaltiesAndAppealsTileViewModel(isEnabled(PenaltiesAndAppeals), penaltyDetailsService.getPenaltySubmissionFrequency(currentITSAStatus), penaltiesCount)

      val paymentCreditAndRefundHistoryTileViewModel =
        PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds), user.incomeSources.yearOfMigration.isDefined)

      val yourBusinessesTileViewModel =
        models.homePage.YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)

      val returnsTileViewModel =
        models.homePage.ReturnsTileViewModel(currentTaxYear, isEnabled(ITSASubmissionIntegration))

      val yourReportingObligationsTileViewModel =
        models.homePage.YourReportingObligationsTileViewModel(currentTaxYear, isEnabled(ReportingFrequencyPage), currentITSAStatus)

      models.homePage.NextPaymentsTileViewModel(paymentsDueMerged, overDuePaymentsCount, accruingInterestPaymentsCount).verify match {

        case Right(viewModel: models.homePage.NextPaymentsTileViewModel) =>
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

          auditingService.extendedAudit(HomeAudit(user, paymentsDueMerged, overDuePaymentsCount, nextUpdatesTileViewModel))

          if (user.isAgent()) {
            Ok(primaryAgentHomeView(homeViewModel)).addingToSession(mandationStatus)
          } else {
            Ok(homeView(homeViewModel)).addingToSession(mandationStatus)
          }
        case Left(ex: Throwable) =>
          Logger("application").error(s"Unable to create the view model ${ex.getMessage} - ${ex.getCause}")
          handleErrorGettingDueDates(user.isAgent())
      }
    }
  }

  private def getDueDates(unpaidCharges: List[FinancialDetailsResponseModel], isFilterOutCodedPoasEnabled: Boolean, penaltiesEnabled: Boolean): List[LocalDate] = {

    val chargesList =
      unpaidCharges.collect {
        case fdm: FinancialDetailsModel => fdm
      }
    whatYouOweService.getFilteredChargesList(
        financialDetailsList = chargesList,
        isFilterCodedOutPoasEnabled = isFilterOutCodedPoasEnabled,
        isPenaltiesEnabled = penaltiesEnabled,
        remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot = mainChargeIsNotPaidFilter).flatMap(_.dueDate)
      .sortWith(_ isBefore _)
      .sortBy(_.toEpochDay())
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

  private def calculateOverduePaymentsCount(paymentsDue: List[LocalDate], outstandingChargesModel: List[OutstandingChargeModel]): Int = {
    val overduePaymentsCountFromDate = paymentsDue.count(_.isBefore(dateService.getCurrentDate))
    val overdueChargesCount = outstandingChargesModel.flatMap(_.relevantDueDate).count(_.isBefore(dateService.getCurrentDate))

    overduePaymentsCountFromDate + overdueChargesCount
  }

  private def mergePaymentsDue(paymentsDue: List[LocalDate], outstandingChargesDueDate: List[LocalDate]): Option[LocalDate] =
    (paymentsDue ::: outstandingChargesDueDate)
      .sortWith(_ isBefore _)
      .headOption

  private def hasDunningLock(financialDetails: List[FinancialDetailsResponseModel]): Boolean =
    financialDetails
      .collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }
      .getOrElse(false)

  private def getRelevantDates(outstandingCharges: List[OutstandingChargeModel]): List[LocalDate] =
    outstandingCharges
      .collect { case OutstandingChargeModel(_, relevantDate, _, _) => relevantDate }
      .flatten

  private def handleErrorGettingDueDates(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
  }

  private def getCurrentITSAStatus(taxYear: TaxYear)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[ITSAStatus.ITSAStatus] = {
    ITSAStatusService.getStatusTillAvailableFutureYears(taxYear.previousYear).map(_.view.mapValues(_.status)
      .toMap
      .withDefaultValue(ITSAStatus.NoStatus)
    ).map(detail => detail(taxYear))
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }

  private def getNextDueDatesIfEnabled()
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    if (isEnabled(OptInOptOutContentUpdateR17)) {
      nextUpdatesService.getNextDueDates()
    } else {
      Future.successful((None, None))
    }
  }

  //These should probably each have their own controllers, as they're going to each be calling the APIs independently and will have different ViewModels
  private def handleYourTasks(origin: Option[String] = None, isAgent: Boolean)
                             (implicit user: MtdItUser[_]): Future[Result] = {
    val getCurrentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val getCurrentDate = dateService.getCurrentDate
    val currentTaxYear = TaxYear(getCurrentTaxYearEnd - 1, getCurrentTaxYearEnd)

    for {
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
      _ <- optInService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
      mandation <- ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandated)
      chargeItemList = getChargeList(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
    } yield {

      val creditsRefundsRepayEnabled= isEnabled(CreditsRefundsRepay)
      val mandationStatus =
        if (mandation) SessionKeys.mandationStatus -> "on"
        else SessionKeys.mandationStatus -> "off"
        
      val homeViewModel = NewHomePageViewModel(chargeItemList, unpaidCharges, creditsRefundsRepayEnabled)
      
      if (user.isAgent()) {
        Ok(newHomeYourTasksView(origin, isAgent,
          yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent),
          overviewUrl(origin, isAgent), helpUrl(origin, isAgent), homeViewModel)).addingToSession(mandationStatus)
      } else {
        Ok(newHomeYourTasksView(origin, isAgent,
          yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent),
          overviewUrl(origin, isAgent), helpUrl(origin, isAgent), homeViewModel))
      }
    }
  }

    def handleRecentActivity(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        Future.successful(Ok(newHomeRecentActivityView(origin, isAgent, yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent), helpUrl(origin, isAgent))))
    }

    def handleOverview(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        Future.successful(Ok(newHomeOverviewView(origin, isAgent, yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent), helpUrl(origin, isAgent))))
    }

    def handleHelp(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        Future.successful(Ok(newHomeHelpView(origin, isAgent, yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent), overviewUrl(origin, isAgent), helpUrl(origin, isAgent))))
    }

    def yourTasksUrl(origin: Option[String] = None, isAgent: Boolean): String = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show(origin).url

    def recentActivityUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleRecentActivity(origin, isAgent).url

    def overviewUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleOverview(origin, isAgent).url

    def helpUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleHelp(origin, isAgent).url
    
}
