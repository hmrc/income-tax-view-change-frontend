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
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.homePage.PaymentCreditAndRefundHistoryTileViewModel
import models.nextUpdates.NextUpdatesTileViewModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val authorisedFunctions: AuthorisedFunctions,
                               val nextUpdatesService: NextUpdatesService,
                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                               val financialDetailsService: FinancialDetailsService,
                               val dateService: DateServiceInterface,
                               val whatYouOweService: WhatYouOweService,
                               auditingService: AuditingService,
                               auth: AuthenticatorPredicate)
                              (implicit val ec: ExecutionContext,
                               implicit val itvcErrorHandler: ItvcErrorHandler,
                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def view(nextPaymentDueDate: Option[LocalDate], overDuePaymentsCount: Option[Int], nextUpdatesTileViewModel: NextUpdatesTileViewModel,
                   paymentCreditAndRefundHistoryTileViewModel: PaymentCreditAndRefundHistoryTileViewModel, dunningLockExists: Boolean, currentTaxYear: Int,
                   displayCeaseAnIncome: Boolean, isAgent: Boolean, origin: Option[String] = None)
                  (implicit user: MtdItUser[_]): Html =
    homeView(
      origin = origin,
      utr = user.saUtr,
      isAgent = isAgent,
      currentTaxYear = currentTaxYear,
      dunningLockExists = dunningLockExists,
      nextPaymentDueDate = nextPaymentDueDate,
      overDuePaymentsCount = overDuePaymentsCount,
      displayCeaseAnIncome = displayCeaseAnIncome,
      incomeSourcesEnabled = isEnabled(IncomeSources),
      nextUpdatesTileViewModel = nextUpdatesTileViewModel,
      creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
      paymentHistoryEnabled = isEnabled(PaymentHistoryRefunds),
      isUserMigrated = user.incomeSources.yearOfMigration.isDefined,
      incomeSourcesNewJourneyEnabled = isEnabled(IncomeSourcesNewJourney),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel
    )

  def handleShowRequest(origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate]) => buildHomePage(nextUpdatesDueDates)
      case Left(ex)                                   => handleErrorGettingDueDates(ex)
    } recover {
      case ex =>
        Logger("application").error(s"[HomeController][handleShowRequest] Downstream error, ${ex.getMessage} - ${ex.getCause}")
        errorHandler(user.isAgent).showInternalServerError()
    }
  }

  private def buildHomePage(nextUpdatesDueDates: Seq[LocalDate])
                           (implicit user: MtdItUser[_]): Future[Result] =
    for {
      unpaidCharges             <- financialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))
      paymentsDue                = getDueDates(unpaidCharges)
      dunningLockExists          = unpaidCharges.collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }.getOrElse(false)
      outstandingChargesModel   <- getOutstandingChargesModel(unpaidCharges)
      outstandingChargesDueDate  = outstandingChargesModel.collect { case OutstandingChargeModel(_, relevantDate, _, _) => relevantDate}.flatten
      overDuePaymentsCount       = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      paymentsDueMerged          = mergePaymentsDue(paymentsDue, outstandingChargesDueDate)
    } yield {

      lazy val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates, dateService.getCurrentDate)

      auditingService.extendedAudit(HomeAudit(user, paymentsDueMerged, overDuePaymentsCount, nextUpdatesTileViewModel))

      lazy val paymentCreditAndRefundHistoryTileViewModel =
        PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds))

      Ok(view(
        isAgent = user.isAgent,
        dunningLockExists = dunningLockExists,
        nextPaymentDueDate = paymentsDueMerged,
        currentTaxYear = dateService.getCurrentTaxYearEnd,
        overDuePaymentsCount = Some(overDuePaymentsCount),
        nextUpdatesTileViewModel = nextUpdatesTileViewModel,
        displayCeaseAnIncome = user.incomeSources.hasOngoingBusinessOrPropertyIncome,
        paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel
      ))
    }

  private def getDueDates(unpaidCharges: List[FinancialDetailsResponseModel]): List[LocalDate] =
    (unpaidCharges collect {
      case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
    })
      .flatten
      .sortWith(_ isBefore _)
      .sortBy(_.toEpochDay())

  private def getOutstandingChargesModel(unpaidCharges: List[FinancialDetailsResponseModel])
                                        (implicit user: MtdItUser[_]): Future[List[OutstandingChargeModel]] =
    whatYouOweService.getWhatYouOweChargesList(
      unpaidCharges,
      isEnabled(CodingOut),
      isEnabled(MFACreditsAndDebits)
    ) map {
      case WhatYouOweChargesList(_, _, Some(OutstandingChargesModel(locm)), _) => locm.filter(_.hasRelevantDueDateWithBCDChargeName)
      case _ => Nil
    }

  private def calculateOverduePaymentsCount(paymentsDue: List[LocalDate], outstandingChargesModel: List[OutstandingChargeModel]): Int = {
    lazy val overduePaymentsCountFromDate = paymentsDue.count(_.isBefore(dateService.getCurrentDate))
    lazy val overdueChargesCount = outstandingChargesModel.length
    overduePaymentsCountFromDate + overdueChargesCount
  }

  private def mergePaymentsDue(paymentsDue: List[LocalDate], outstandingChargesDueDate: List[LocalDate]): Option[LocalDate] = {
    (paymentsDue ::: outstandingChargesDueDate)
      .sortWith(_ isBefore _)
      .headOption
  }

  private def handleErrorGettingDueDates(ex: Throwable)(implicit user: MtdItUser[_]): Future[Result] = {
    Logger("application").error(s"[HomeController][handleShowRequest]: Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
    Future.successful { errorHandler(user.isAgent).showInternalServerError() }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShowRequest(origin)
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleShowRequest()
  }
}
