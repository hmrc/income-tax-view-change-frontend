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
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
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
                  (implicit user: MtdItUser[_]): Html = {
    homeView(
      nextPaymentDueDate = nextPaymentDueDate,
      overDuePaymentsCount = overDuePaymentsCount,
      nextUpdatesTileViewModel = nextUpdatesTileViewModel,
      paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel,
      user.saUtr,
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear,
      displayCeaseAnIncome = displayCeaseAnIncome,
      isAgent = isAgent,
      origin = origin,
      creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
      paymentHistoryEnabled = isEnabled(PaymentHistoryRefunds),
      incomeSourcesEnabled = isEnabled(IncomeSources),
      isUserMigrated = user.incomeSources.yearOfMigration.isDefined
    )
  }

  def handleShowRequest(isAgent: Boolean, origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val isTimeMachineEnabled: Boolean = isEnabled(TimeMachineAddYear)
    val incomeSourceCurrentTaxYear: Int = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
    val currentDate = dateService.getCurrentDate(isTimeMachineEnabled)

    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate]) =>
        val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates, currentDate)
        val unpaidChargesFuture: Future[List[FinancialDetailsResponseModel]] = financialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))

        val dueDates: Future[List[LocalDate]] = unpaidChargesFuture.map {
          _.flatMap {
            case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
            case _ => List.empty[LocalDate]
          }.sortWith(_ isBefore _)
        }

        for {
          paymentsDue <- dueDates.map(_.sortBy(_.toEpochDay()))
          unpaidCharges <- unpaidChargesFuture
          dunningLockExistsValue = unpaidCharges.collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }.getOrElse(false)
          outstandingChargesModel <- whatYouOweService.getWhatYouOweChargesList(unpaidCharges, isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(TimeMachineAddYear)).map(_.outstandingChargesModel match {
            case Some(OutstandingChargesModel(locm)) => locm.filter(ocm => ocm.relevantDueDate.isDefined && ocm.chargeName == "BCD")
            case _ => Nil
          })
          outstandingChargesDueDate = outstandingChargesModel.flatMap {
            case OutstandingChargeModel(_, Some(relevantDate), _, _) => List(relevantDate)
            case _ => Nil
          }
          overDuePaymentsCount = paymentsDue.count(_.isBefore(dateService.getCurrentDate(isTimeMachineEnabled))) + outstandingChargesModel.length
          paymentsDueMerged = (paymentsDue ::: outstandingChargesDueDate).sortWith(_ isBefore _).headOption
          displayCeaseAnIncome = user.incomeSources.hasOngoingBusinessOrPropertyIncome
        } yield {
          auditingService.extendedAudit(HomeAudit(
            mtdItUser = user,
            paymentsDueMerged,
            overDuePaymentsCount,
            nextUpdatesTileViewModel))

          val paymentCreditAndRefundHistoryTileViewModel = PaymentCreditAndRefundHistoryTileViewModel(
            unpaidCharges,
            creditsRefundsRepayEnabled = isEnabled(CreditsRefundsRepay),
            paymentHistoryRefundsEnabled = isEnabled(PaymentHistoryRefunds)
          )

          Ok(
            view(
              paymentsDueMerged,
              Some(overDuePaymentsCount),
              nextUpdatesTileViewModel,
              paymentCreditAndRefundHistoryTileViewModel,
              dunningLockExistsValue,
              incomeSourceCurrentTaxYear,
              displayCeaseAnIncome,
              isAgent = isAgent
            )
          )
        }
      case Left(ex) =>
        Logger("application")
          .error(s"[HomeController][handleShowRequest]: Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }.recover {
      case ex =>
        Logger("application")
          .error(s"[HomeController][handleShowRequest] Downstream error, ${ex.getMessage} - ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShowRequest(isAgent = false, origin)
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleShowRequest(isAgent = true)
  }
}
