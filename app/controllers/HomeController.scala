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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.admin._
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.homePage._
import models.incomeSourceDetails.TaxYear
import models.obligations.NextUpdatesTileViewModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
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

  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShowRequest(isAgent = false, origin)
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleShowRequest(isAgent = true)
  }

  def handleShowRequest(isAgent: Boolean, origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] =
    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate]) => buildHomePage(nextUpdatesDueDates, isAgent, origin)
      case Left(ex) =>
        Logger("application").error(s"Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
        Future.successful(handleErrorGettingDueDates(isAgent))
    } recover {
      case ex =>
        Logger("application").error(s"Downstream error, ${ex.getMessage} - ${ex.getCause}")
        handleErrorGettingDueDates(isAgent)
    }

  private def buildHomePage(nextUpdatesDueDates: Seq[LocalDate], isAgent: Boolean, origin: Option[String])
                           (implicit user: MtdItUser[_]): Future[Result] =
    for {
      unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))
      paymentsDue = getDueDates(unpaidCharges)
      dunningLockExists = hasDunningLock(unpaidCharges)
      outstandingChargesModel <- getOutstandingChargesModel(unpaidCharges)
      outstandingChargeDueDates = getRelevantDates(outstandingChargesModel)
      overDuePaymentsCount = calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
      paymentsDueMerged = mergePaymentsDue(paymentsDue, outstandingChargeDueDates)
    } yield {

      val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates, dateService.getCurrentDate, isEnabled(OptOut))

      val paymentCreditAndRefundHistoryTileViewModel =
        PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds), user.incomeSources.yearOfMigration.isDefined)

      val yourBusinessesTileViewModel = YourBusinessesTileViewModel(user.incomeSources.hasOngoingBusinessOrPropertyIncome, isEnabled(IncomeSources),
        isEnabled(IncomeSourcesNewJourney))

      val returnsTileViewModel = ReturnsTileViewModel(TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd), isEnabled(ITSASubmissionIntegration))

      NextPaymentsTileViewModel(paymentsDueMerged, overDuePaymentsCount).verify match {

        case Right(viewModel: NextPaymentsTileViewModel) => val homeViewModel = HomePageViewModel(
          utr = user.saUtr,
          nextPaymentsTileViewModel = viewModel,
          returnsTileViewModel = returnsTileViewModel,
          nextUpdatesTileViewModel = nextUpdatesTileViewModel,
          paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel,
          yourBusinessesTileViewModel = yourBusinessesTileViewModel,
          dunningLockExists = dunningLockExists,
          origin = origin
        )
          auditingService.extendedAudit(HomeAudit(user, paymentsDueMerged, overDuePaymentsCount, nextUpdatesTileViewModel))
          Ok(homeView(
            homeViewModel,
            isAgent = isAgent
          ))
        case Left(ex: Throwable) =>
          Logger("application").error(s"Unable to create the view model ${ex.getMessage} - ${ex.getCause}")
          handleErrorGettingDueDates(isAgent)
    }
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
    case WhatYouOweChargesList(_, _, Some(OutstandingChargesModel(outstandingCharges)), _) =>
      outstandingCharges.filter(_.isBalancingChargeDebit)
        .filter(_.relevantDueDate.isDefined)
    case _ => Nil
  }

private def calculateOverduePaymentsCount(paymentsDue: List[LocalDate], outstandingChargesModel: List[OutstandingChargeModel]): Int = {
  val overduePaymentsCountFromDate = paymentsDue.count(_.isBefore(dateService.getCurrentDate))
  val overdueChargesCount = outstandingChargesModel.length
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
}
