/*
 * Copyright 2022 HM Revenue & Customs
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
import auth.{MtdItUser}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, NavBarPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}

import javax.inject.{Inject, Singleton}
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, IncomeSourceDetailsService, NextUpdatesService, WhatYouOweService, DateService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val authorisedFunctions: AuthorisedFunctions,
                               val retrieveNino: NinoPredicate,
                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                               val nextUpdatesService: NextUpdatesService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                               val financialDetailsService: FinancialDetailsService,
                               val dateService: DateService,
                               val whatYouOweService: WhatYouOweService,
                               val retrieveBtaNavBar: NavBarPredicate,
                               auditingService: AuditingService)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePaymentsCount: Option[Int],
                   overDueUpdatesCount: Option[Int], dunningLockExists: Boolean, currentTaxYear: Int, isAgent: Boolean,
                   origin: Option[String] = None)
                  (implicit user: MtdItUser[_]): Html = {
    homeView(
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      overDuePaymentsCount = overDuePaymentsCount,
      overDueUpdatesCount = overDueUpdatesCount,
      user.saUtr,
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear,
      isAgent = isAgent,
      origin = origin,
      creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
      isUserMigrated = user.incomeSources.yearOfMigration.isDefined
    )
  }

  def handleShowRequest(itvcErrorHandler: ShowInternalServerError, isAgent: Boolean, incomeSourceCurrentTaxYear: Int, origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    nextUpdatesService.getNextDeadlineDueDateAndOverDueObligations().flatMap { latestDeadlineDate =>

      val unpaidCharges: Future[List[FinancialDetailsResponseModel]] = financialDetailsService.getAllUnpaidFinancialDetails

      val dueDates: Future[List[LocalDate]] = unpaidCharges.map {
        _.flatMap {
          case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
          case _ => List.empty[LocalDate]
        }.sortWith(_ isBefore _)
      }

      for {
        paymentsDue <- dueDates.map(_.sortBy(_.toEpochDay()))
        dunningLockExistsValue <- unpaidCharges.map(_.collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true })
        outstandingChargesModel <- whatYouOweService.getWhatYouOweChargesList.map(_.outstandingChargesModel match {
          case Some(OutstandingChargesModel(locm)) => locm.filter(ocm => ocm.relevantDueDate.isDefined && ocm.chargeName == "BCD")
          case _ => Nil
        })
        outstandingChargesDueDate = outstandingChargesModel.flatMap {
          case OutstandingChargeModel(_, Some(relevantDate), _, _) => List(LocalDate.parse(relevantDate))
          case _ => Nil
        }
        overDuePaymentsCount = paymentsDue.count(_.isBefore(dateService.getCurrentDate)) + outstandingChargesModel.length
        overDueUpdatesCount = latestDeadlineDate._2.size
        paymentsDueMerged = (paymentsDue ::: outstandingChargesDueDate).sortWith(_ isBefore _).headOption
      } yield {
        auditingService.extendedAudit(HomeAudit(
          mtdItUser = user,
          paymentsDueMerged,
          latestDeadlineDate._1,
          overDuePaymentsCount,
          overDueUpdatesCount))
        Ok(
          view(
            paymentsDueMerged,
            latestDeadlineDate._1,
            Some(overDuePaymentsCount),
            Some(overDueUpdatesCount),
            dunningLockExistsValue.isDefined,
            incomeSourceCurrentTaxYear,
            isAgent = isAgent
          )
        )
      }

    }.recover {
      case ex =>
        Logger("application").error(s"[HomeController][Home] Downstream error, ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        user.incomeSources.getCurrentTaxEndYear,
        origin = origin
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit agent =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleShowRequest(
              itvcErrorHandler = itvcErrorHandlerAgent,
              isAgent = true,
              mtdItUser.incomeSources.getCurrentTaxEndYear
            )
        }
  }
}
