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

import java.time.LocalDate
import audit.AuditingService
import audit.models.HomeAudit
import auth.MtdItUser
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}

import javax.inject.{Inject, Singleton}
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.twirl.api.Html
import services.{DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService, NextUpdatesService, WhatYouOweService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val authorisedFunctions: AuthorisedFunctions,
                               val nextUpdatesService: NextUpdatesService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                               val financialDetailsService: FinancialDetailsService,
                               implicit val dateService: DateServiceInterface,
                               val whatYouOweService: WhatYouOweService,
                               auditingService: AuditingService,
                               auth: AuthenticatorPredicate)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private def view(availableCredit: BigDecimal,nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePaymentsCount: Option[Int],
                   overDueUpdatesCount: Option[Int], dunningLockExists: Boolean, currentTaxYear: Int,
                   displayCeaseAnIncome: Boolean, isAgent: Boolean, origin: Option[String] = None)
                  (implicit user: MtdItUser[_]): Html = {
    homeView(
      availableCredit = availableCredit,
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      overDuePaymentsCount = overDuePaymentsCount,
      overDueUpdatesCount = overDueUpdatesCount,
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

  def handleShowRequest(itvcErrorHandler: ShowInternalServerError, isAgent: Boolean, incomeSourceCurrentTaxYear: Int, origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    implicit val isTimeMachineEnabled: Boolean = isEnabled(TimeMachineAddYear)
    nextUpdatesService.getNextDeadlineDueDateAndOverDueObligations.flatMap { latestDeadlineDate =>

      val unpaidChargesFuture: Future[List[FinancialDetailsResponseModel]] = financialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))

      val dueDates: Future[List[LocalDate]] = unpaidChargesFuture.map {
        _.flatMap {
          case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
          case _ => List.empty[LocalDate]
        }.sortWith(_ isBefore _)
      }

      val availableCredit: Future[BigDecimal] = financialDetailsService.getAllFinancialDetails
        .map(
          _.flatMap {
            case (_, model: FinancialDetailsModel) if isEnabled(CreditsRefundsRepay) => model.balanceDetails.availableCredit
            case _ => None
          }.sum
        )

      for {
        paymentsDue <- dueDates.map(_.sortBy(_.toEpochDay()))
        unpaidCharges <- unpaidChargesFuture
        availableCredit <- availableCredit
        dunningLockExistsValue = unpaidCharges.collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }.getOrElse(false)
        outstandingChargesModel <- whatYouOweService.getWhatYouOweChargesList(unpaidCharges, isEnabled(CodingOut), isEnabled(MFACreditsAndDebits)).map(_.outstandingChargesModel match {
          case Some(OutstandingChargesModel(locm)) => locm.filter(ocm => ocm.relevantDueDate.isDefined && ocm.chargeName == "BCD")
          case _ => Nil
        })
        outstandingChargesDueDate = outstandingChargesModel.flatMap {
          case OutstandingChargeModel(_, Some(relevantDate), _, _) => List(relevantDate)
          case _ => Nil
        }
        overDuePaymentsCount = paymentsDue.count(_.isBefore(dateService.getCurrentDate(isTimeMachineEnabled))) + outstandingChargesModel.length
        overDueUpdatesCount = latestDeadlineDate._2.size
        paymentsDueMerged = (paymentsDue ::: outstandingChargesDueDate).sortWith(_ isBefore _).headOption
        displayCeaseAnIncome = user.incomeSources.hasOngoingBusinessOrPropertyIncome
      } yield {
        auditingService.extendedAudit(HomeAudit(
          mtdItUser = user,
          paymentsDueMerged,
          latestDeadlineDate._1,
          overDuePaymentsCount,
          overDueUpdatesCount))
        Ok(
          view(
            availableCredit,
            paymentsDueMerged,
            latestDeadlineDate._1,
            Some(overDuePaymentsCount),
            Some(overDueUpdatesCount),
            dunningLockExistsValue,
            incomeSourceCurrentTaxYear,
            displayCeaseAnIncome,
            isAgent = isAgent
          )
        )
      }

    }.recover {
      case ex =>
        Logger("application")
          .error(s"[HomeController][Home] Downstream error, ${ex.getMessage} - ${ex.getCause}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)),
        origin = origin
      )
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true,
        dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
      )
  }
}
