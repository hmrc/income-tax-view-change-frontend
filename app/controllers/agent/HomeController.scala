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

package controllers.agent

import _root_.utils.CurrentDateProvider
import audit.AuditingService
import audit.models.HomeAudit
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, IncomeSourceDetailsService, NextUpdatesService, WhatYouOweService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.Home

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(homeView: Home,
                               nextUpdatesService: NextUpdatesService,
                               financialDetailsService: FinancialDetailsService,
                               incomeSourceDetailsService: IncomeSourceDetailsService,
                               currentDateProvider: CurrentDateProvider,
                               whatYouOweService: WhatYouOweService,
                               auditingService: AuditingService,
                               val authorisedFunctions: FrontendAuthorisedFunctions)
                              (implicit mcc: MessagesControllerComponents,
                               implicit val appConfig: FrontendAppConfig,
                               itvcErrorHandler: AgentItvcErrorHandler,
                               implicit val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePaymentsCount: Option[Int],
                   overDueUpdatesCount: Option[Int], dunningLockExists: Boolean, currentTaxYear: Int, isAgent: Boolean = true)
                  (implicit user: MtdItUser[_]): Html = {
    homeView(
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      overDuePaymentsCount = overDuePaymentsCount,
      overDueUpdatesCount = overDueUpdatesCount,
      user.saUtr,
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      paymentHistoryEnabled = isEnabled(PaymentHistory),
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear,
      isAgent = isAgent
    )
  }

  private def getOutstandingChargesDueDate(outstandingChargesModel: List[OutstandingChargeModel]): List[LocalDate] = outstandingChargesModel.flatMap {
    case OutstandingChargeModel(_, Some(relevantDate), _, _) => List(LocalDate.parse(relevantDate))
    case _ => Nil
  }

  private def getOutstandingChargesModel(mtdUser: MtdItUser[_])(implicit headerCarrier: HeaderCarrier): Future[List[OutstandingChargeModel]] =
    whatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdUser).map(_.outstandingChargesModel match {
      case Some(OutstandingChargesModel(locm)) => locm.filter(ocm => ocm.relevantDueDate.isDefined && ocm.chargeName == "BCD")
      case _ => Nil
    })

  private def getDueDates(unpaidCharges: List[FinancialDetailsResponseModel]): List[LocalDate] = unpaidCharges.flatMap {
    case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
    case _ => List.empty[LocalDate]
  }.sortWith(_ isBefore _).sortBy(_.toEpochDay())

  def show(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>

        for {
          mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true)
          latestDeadlineDate <- nextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(implicitly, implicitly, mtdItUser)
          unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails(mtdItUser, implicitly, implicitly)
          _ = if (unpaidCharges.exists(fds => fds.isInstanceOf[FinancialDetailsErrorModel]
            && fds.asInstanceOf[FinancialDetailsErrorModel].code != NOT_FOUND))
            throw new InternalServerException("[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details")
          outstandingChargesModel <- getOutstandingChargesModel(mtdItUser)
          paymentsDue = getDueDates(unpaidCharges)
          dunningLockExistsValue = unpaidCharges.collectFirst { case fdm: FinancialDetailsModel if fdm.dunningLockExists => true }
          overDuePaymentsCount = paymentsDue.count(_.isBefore(currentDateProvider.getCurrentDate())) + outstandingChargesModel.length
          overDueUpdatesCount = latestDeadlineDate._2.size
          paymentsDueMerged = (paymentsDue ::: getOutstandingChargesDueDate(outstandingChargesModel)).sortWith(_ isBefore _).headOption
        } yield {
          if (isEnabled(TxmEventsApproved)) {
            auditingService.extendedAudit(HomeAudit(
              mtdItUser = mtdItUser,
              paymentsDueMerged,
              latestDeadlineDate._1,
              overDuePaymentsCount,
              overDueUpdatesCount
            ))
          }

          Ok(
            view(
              paymentsDueMerged,
              latestDeadlineDate._1,
              Some(overDuePaymentsCount),
              Some(overDueUpdatesCount),
              dunningLockExistsValue.isDefined,
              mtdItUser.incomeSources.getCurrentTaxEndYear
            )(mtdItUser)
          )
        }
  }
}