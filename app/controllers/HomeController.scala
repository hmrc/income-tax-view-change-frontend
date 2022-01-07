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

import audit.AuditingService
import audit.models.HomeAudit
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, NextUpdatesService, WhatYouOweService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.CurrentDateProvider

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val retrieveNino: NinoPredicate,
                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                               val nextUpdatesService: NextUpdatesService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val financialDetailsService: FinancialDetailsService,
                               val currentDateProvider: CurrentDateProvider,
                               val whatYouOweService: WhatYouOweService,
                               auditingService: AuditingService)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePaymentsCount: Option[Int],
                   overDueUpdatesCount: Option[Int], dunningLockExists: Boolean, currentTaxYear: Int)
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
      currentTaxYear = currentTaxYear
    )
  }

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>

      nextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(user.incomeSources).flatMap { latestDeadlineDate =>

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
          overDuePaymentsCount = paymentsDue.count(_.isBefore(currentDateProvider.getCurrentDate())) + outstandingChargesModel.length
          overDueUpdatesCount = latestDeadlineDate._2.size
          paymentsDueMerged = (paymentsDue ::: outstandingChargesDueDate).sortWith(_ isBefore _).headOption
        } yield {
          if (isEnabled(TxmEventsApproved)) {
            auditingService.extendedAudit(HomeAudit(
              mtdItUser = user,
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
              user.incomeSources.getCurrentTaxEndYear
            )
          )
        }

      }.recover {
        case ex =>
          Logger("application").error(s"[HomeController][Home] Downstream error, ${ex.getMessage}")
          itvcErrorHandler.showInternalServerError()
      }
  }
}
