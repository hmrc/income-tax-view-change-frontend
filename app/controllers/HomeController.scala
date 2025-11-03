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
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.sessionUtils.SessionKeys
import enums.MTDSupportingAgent
import models.admin._
import models.financialDetails.{ChargeItem, FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.homePage._
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.NextUpdatesTileViewModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import services.optIn.OptInService
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val primaryAgentHomeView: views.html.agent.PrimaryAgentHome,
                               val supportingAgentHomeView: views.html.agent.SupportingAgentHome,
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
                              (implicit val ec: ExecutionContext,
                               implicit val itvcErrorHandler: ItvcErrorHandler,
                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleShowRequest()
  }

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate]) if user.usersRole == MTDSupportingAgent =>
        buildHomePageForSupportingAgent(nextUpdatesDueDates)
      case Right(nextUpdatesDueDates: Seq[LocalDate]) => buildHomePage(nextUpdatesDueDates, origin)
      case Left(ex) =>
        Logger("application").error(s"Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
        Future.successful(handleErrorGettingDueDates(user.isAgent()))
    } recover {
      case ex =>
        Logger("application").error(s"Downstream error, ${ex.getMessage} - ${ex.getCause}")
        handleErrorGettingDueDates(user.isAgent())
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

      val yourBusinessesTileViewModel = YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)
      val yourReportingObligationsTileViewModel = YourReportingObligationsTileViewModel(currentTaxYear, isEnabled(ReportingFrequencyPage), currentITSAStatus)

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

    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()
      paymentsDue = getDueDates(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      dunningLockExists = hasDunningLock(unpaidCharges)
      outstandingChargesModel <- getOutstandingChargesModel(unpaidCharges)
      outstandingChargeDueDates = getRelevantDates(outstandingChargesModel)
      overDuePaymentsCount = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      accruingInterestPaymentsCount = NextPaymentsTileViewModel.paymentsAccruingInterestCount(unpaidCharges, dateService.getCurrentDate)
      currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
      penaltiesCount <- penaltyDetailsService.getPenaltiesCount(isEnabled(PenaltiesBackendEnabled))
      paymentsDueMerged = mergePaymentsDue(paymentsDue, outstandingChargeDueDates)
      mandation <- ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(_.isMandated)
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
      val penaltiesAndAppealsTileViewModel: PenaltiesAndAppealsTileViewModel = PenaltiesAndAppealsTileViewModel(isEnabled(PenaltiesAndAppeals), penaltyDetailsService.getPenaltySubmissionFrequency(currentITSAStatus), penaltiesCount)

      val paymentCreditAndRefundHistoryTileViewModel =
        PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds), isEnabled(ClaimARefundR18), user.incomeSources.yearOfMigration.isDefined)

      val yourBusinessesTileViewModel = YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome)

      val returnsTileViewModel = ReturnsTileViewModel(currentTaxYear, isEnabled(ITSASubmissionIntegration))

      val yourReportingObligationsTileViewModel = YourReportingObligationsTileViewModel(currentTaxYear, isEnabled(ReportingFrequencyPage), currentITSAStatus)

      NextPaymentsTileViewModel(paymentsDueMerged, overDuePaymentsCount, accruingInterestPaymentsCount, isEnabled(YourSelfAssessmentCharges)).verify match {

        case Right(viewModel: NextPaymentsTileViewModel) => val homeViewModel = HomePageViewModel(
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

        val mandationStatus = if (mandation) SessionKeys.mandationStatus -> "on"
        else SessionKeys.mandationStatus -> "off"

          auditingService.extendedAudit(HomeAudit(user, paymentsDueMerged, overDuePaymentsCount, nextUpdatesTileViewModel))
          if(user.isAgent()) {
            Ok(primaryAgentHomeView(
              homeViewModel
            )).addingToSession(mandationStatus)
          } else {
            Ok(homeView(
              homeViewModel
            )).addingToSession(mandationStatus)
          }
        case Left(ex: Throwable) =>
          Logger("application").error(s"Unable to create the view model ${ex.getMessage} - ${ex.getCause}")
          handleErrorGettingDueDates(user.isAgent())
    }
}
  }

  private def getDueDates(unpaidCharges: List[FinancialDetailsResponseModel], isFilterOutCodedPoasEnabled: Boolean,
                          penaltiesEnabled: Boolean): List[LocalDate] = {
    val chargesList = unpaidCharges collect {
      case fdm: FinancialDetailsModel => fdm
    }
      whatYouOweService.getFilteredChargesList(chargesList,
          isFilterOutCodedPoasEnabled,
          penaltiesEnabled,
          mainChargeIsNotPaidFilter).flatMap(_.dueDate)
      .sortWith(_ isBefore _)
      .sortBy(_.toEpochDay())
  }

private def getOutstandingChargesModel(unpaidCharges: List[FinancialDetailsResponseModel])
                                      (implicit user: MtdItUser[_]): Future[List[OutstandingChargeModel]] =
  whatYouOweService.getWhatYouOweChargesList(
    unpaidCharges,
    isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
    isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
    mainChargeIsNotPaidFilter,
    isEnabled(ClaimARefundR18)
  ) map {
    case WhatYouOweChargesList(_, _, Some(OutstandingChargesModel(outstandingCharges)), _, _) =>
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

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem]  = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid  => x
  }

  private def getNextDueDatesIfEnabled()
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    if (isEnabled(OptInOptOutContentUpdateR17)) {
      nextUpdatesService.getNextDueDates()
    } else {
      Future.successful((None, None))
    }
  }

}
