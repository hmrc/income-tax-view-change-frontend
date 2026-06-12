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

package hub.controllers.newHomePage

import common.auth.{AuthActions, MtdItUser}
import common.config.featureswitch.FeatureSwitching
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.models.admin.{CreditsRefundsRepay, FilterCodedOutPoas, PenaltiesAndAppeals, RecentActivity}
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus
import common.services.{AuditingService, DateServiceInterface, ITSAStatusService}
import common.utils.sessionUtils.SessionKeys
import financials.services.*
import hub.audit.models.HomeAudit
import hub.models.newHomePage.SubmissionDeadlinesViewModel
import hub.services.newHomePage.HandleYourTasksService
import hub.utils.HomePageUtils
import hub.views.html.newHomePage.NewHomeYourTasksView
import models.creditsandrefunds.CreditsModel
import models.financialDetails.*
import obligations.models.{ObligationsModel, ObligationsResponseModel, SingleObligationModel}
import obligations.services.NextUpdatesService
import obligations.services.reportingObligations.optOut.OptOutService
import obligations.services.reportingObligations.signUp.SignUpService
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HandleYourTasksController @Inject()(val authActions: AuthActions,
                                          val handleYourTasksView: NewHomeYourTasksView,
                                          val signUpService: SignUpService,
                                          val optOutService: OptOutService,
                                          val ITSAStatusService: ITSAStatusService,
                                          val whatYouOweService: WhatYouOweService,
                                          val creditService: CreditService,
                                          val dateService: DateServiceInterface,
                                          val financialDetailsService: FinancialDetailsService,
                                          val nextUpdatesService: NextUpdatesService,
                                          val handleYourTasksService: HandleYourTasksService,
                                          val auditingService: AuditingService)
                                         (implicit val ec: ExecutionContext,
                                          mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends FrontendController(mcc) with I18nSupport with FeatureSwitching with HomePageUtils {


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
    handleYourTasks(origin, user.isAgent)
  }

  private def handleYourTasks(origin: Option[String] = None, isAgent: Boolean)
                             (implicit user: MtdItUser[_]): Future[Result] = {
    val currentTaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

    for {
      credits: CreditsModel <- creditService.getAllCredits
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails()

      _ <- signUpService.updateJourneyStatusInSessionData(journeyComplete = false)
      _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
      currentItsaStatus <- getCurrentITSAStatus(currentTaxYear)
      chargeItemList = getChargeList(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      
      outstandingChargesModel <- getOutstandingChargesModel(unpaidCharges)
      paymentsDue = getDueDates(unpaidCharges, isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals))
      outstandingChargeDueDates = getRelevantDates(outstandingChargesModel)
      overDuePaymentsCount = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      paymentsDueMerged = mergePaymentsDue(paymentsDue, outstandingChargeDueDates)

      obligationsResponseModel = nextUpdatesService.getOpenObligations()
      dueDates <- nextUpdatesService.getDueDates(Some(obligationsResponseModel)).flatMap {
        case Right(dueDates) => Future.successful(dueDates)
        case Left(ex) => 
          Logger("application").error(s"Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
          Future.failed(ex)
      }

      updatesAndDeadlinesViewModel <- getNextUpdates(obligationsResponseModel)
    } yield {

      val mandation = currentItsaStatus == ITSAStatus.Mandated

      val creditsRefundsRepayEnabled = isEnabled(CreditsRefundsRepay)
      val penaltiesAndAppealsEnabled = isEnabled(PenaltiesAndAppeals)
      val mandationStatus =
        if (mandation) SessionKeys.mandationStatus -> "on"
        else SessionKeys.mandationStatus -> "off"

      val yourTaskCardViewModel = handleYourTasksService.getYourTasksCards(updatesAndDeadlinesViewModel, isAgent, chargeItemList, credits, creditsRefundsRepayEnabled, currentItsaStatus, penaltiesAndAppealsEnabled)
      
      val overdueUpdatesCount = dueDates.count(_.isBefore(dateService.getCurrentDate))
      val nextUpdateDueDate = dueDates.sortWith(_ isBefore _).headOption
      val userIsCYPlusOne = currentItsaStatus == ITSAStatus.NoStatus

      if(user.isSupportingAgent) {
        auditingService.extendedAudit(
          HomeAudit.applySupportingAgent(
            mtdItUser = user,
            overdueUpdatesCount = overdueUpdatesCount,
            nextUpdateDueDate = nextUpdateDueDate,
            userIsCYPlusOne = userIsCYPlusOne
          )
        )
      } else {
        auditingService.extendedAudit(HomeAudit(
          mtdItUser = user,
          nextPaymentDueDate = paymentsDueMerged,
          overduePaymentsCount = overDuePaymentsCount,
          overdueUpdatesCount = overdueUpdatesCount,
          nextUpdateDueDate = nextUpdateDueDate,
          userIsCYPlusOne = userIsCYPlusOne
        ))
      }

      Ok(handleYourTasksView(origin,
        yourTasksUrl(origin, isAgent), recentActivityUrl(origin, isAgent),
        overviewUrl(origin, isAgent), helpUrl(origin, isAgent), yourTaskCardViewModel, isEnabled(RecentActivity))).addingToSession(mandationStatus)
    }
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

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }

  private def getNextUpdates(obligationsResponseModel: Future[ObligationsResponseModel])(implicit user: MtdItUser[_]): Future[SubmissionDeadlinesViewModel] = {

    val submissionDeadlinesViewModel = {
      for {
        (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- nextUpdatesService.getNextDueDates(Some(obligationsResponseModel))
        nextUpdatesOpenObligations <- getOpenObligations(obligationsResponseModel)
      } yield {
        SubmissionDeadlinesViewModel(
          openObligations = nextUpdatesOpenObligations,
          currentDate = dateService.getCurrentDate,
          nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
          nextTaxReturnDueDate = nextTaxReturnDueDate
        )
      }
    }.recoverWith {
      case ex =>
        Logger("application").error(s"Failed to retrieve reporting content checks: ${ex.getMessage}")
        Future.successful(SubmissionDeadlinesViewModel(Seq.empty, dateService.getCurrentDate, None, None))
    }
    submissionDeadlinesViewModel
  }

  private def getOpenObligations(obligationsResponseModel: Future[ObligationsResponseModel]): Future[Seq[SingleObligationModel]] = {
    obligationsResponseModel.flatMap {
      case openObligations: ObligationsModel if openObligations.obligations.forall(_.obligations.nonEmpty) => Future.successful(openObligations.obligations.flatMap(_.obligations))
      case _ =>
        Logger("application").error("Unexpected Exception getting open obligations")
        Future.successful(Seq.empty[SingleObligationModel])
    }
  }

  private def getCurrentITSAStatus(currentTaxYear: TaxYear)(
    implicit hc: HeaderCarrier,
    user: MtdItUser[_]
  ): Future[ITSAStatus.ITSAStatus] = {
    ITSAStatusService
      .getITSAStatusDetail(currentTaxYear, false, false)
      .map { statusDetailList =>
        statusDetailList
          .flatMap(_.itsaStatusDetails)
          .flatMap(_.map(_.status))
          .headOption
          .getOrElse(ITSAStatus.NoStatus)
      }
  }
}
