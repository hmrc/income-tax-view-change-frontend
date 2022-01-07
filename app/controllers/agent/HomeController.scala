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

import audit.AuditingService
import audit.models.HomeAudit
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.outstandingCharges.OutstandingChargeModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, _}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.Home

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(home: Home,
                               nextUpdatesService: NextUpdatesService,
                               financialDetailsService: FinancialDetailsService,
                               incomeSourceDetailsService: IncomeSourceDetailsService,
                               whatYouOweService: WhatYouOweService,
                               auditingService: AuditingService,
                               val authorisedFunctions: FrontendAuthorisedFunctions)
                              (implicit mcc: MessagesControllerComponents,
                               implicit val appConfig: FrontendAppConfig,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  private def view(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]],
                   nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int],
                   overduePaymentExists: Boolean,
                   dunningLockExists: Boolean,
                   currentTaxYear: Int)
                  (implicit request: Request[_], user: MtdItUser[_]): Html = {
    home(
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      overduePaymentExists = overduePaymentExists,
      paymentHistoryEnabled = isEnabled(PaymentHistory),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear
    )
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true)
        dueObligationDetails <- nextUpdatesService.getObligationDueDates()(implicitly, implicitly, mtdItUser)
        unpaidFinancialDetails <- financialDetailsService.getAllUnpaidFinancialDetails(mtdItUser, implicitly, implicitly)
        _ = if(unpaidFinancialDetails.exists(fds => fds.isInstanceOf[FinancialDetailsErrorModel]
          && fds.asInstanceOf[FinancialDetailsErrorModel].code != NOT_FOUND))
          throw new InternalServerException("[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details")
        dueChargesDetails = financialDetailsService.getChargeDueDates(unpaidFinancialDetails)
        dunningLockExistsValue = dunningLockExists(unpaidFinancialDetails)
        outstandingChargesModels <- whatYouOweService.getWhatYouOweChargesList()(implicitly, mtdItUser)
        outstandingChargesModel = getOutstandingChargesModel(outstandingChargesModels)
        mergedDueChargesDetailsValue = mergeDueChargesDetails(dueChargesDetails, outstandingChargesModel)
      } yield {
        if (isEnabled(TxmEventsApproved)) {
          auditingService.extendedAudit(HomeAudit(
            mtdItUser, mergedDueChargesDetailsValue, dueObligationDetails
          ))
        }

        Ok(
          view(mergedDueChargesDetailsValue,
            dueObligationDetails,
            overduePaymentExists(mergedDueChargesDetailsValue),
            dunningLockExistsValue,
            currentTaxYear = mtdItUser.incomeSources.getCurrentTaxEndYear)(implicitly, mtdItUser)
        )
      }
  }

  private def dunningLockExists(financialDetailsResponseModel: List[FinancialDetailsResponseModel]): Boolean = {
    financialDetailsResponseModel.collectFirst {
      case fdm: FinancialDetailsModel if fdm.dunningLockExists => true
    }.isDefined
  }

  private def overduePaymentExists(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]]): Boolean = {
    nextPaymentOrOverdue match {
      case Some(_@Left((_, true))) => true
      case Some(_@Right(overdueCount)) if overdueCount > 0 => true
      case _ => false
    }
  }

  private def getOutstandingChargesModel(whatYouOweChargesList: WhatYouOweChargesList): Option[Either[(LocalDate, Boolean), Int]] = {
    val outstandingChargesModels = whatYouOweChargesList.outstandingChargesModel.map {
      _.outstandingCharges.filter(t => t.relevantDueDate.isDefined && t.chargeName == "BCD")
    }.getOrElse(Nil)

    outstandingChargesModels match {
      case OutstandingChargeModel(_, Some(relevantDueDate), _, _) :: Nil => Some(Left(LocalDate.parse(relevantDueDate), true))
      case _ :: xs => Some(Right(xs.length + 1))
      case _ => None
    }
  }

  private def mergeDueChargesDetails(
                                      first: Option[Either[(LocalDate, Boolean), Int]],
                                      second: Option[Either[(LocalDate, Boolean), Int]]
                                    ): Option[Either[(LocalDate, Boolean), Int]] =
    (first, second) match {
    case (first@Some(Left(tup1)), second@Some(Left(tup2))) => if (tup1._1 isBefore tup2._1) first else second
    case (first@Some(Left(_)), None) => first
    case (None, second@Some(Left(_))) => second
    case (first@Some(Right(count)), Some(Left(_))) => first.copy(Right(count + 1))
    case (Some(Left(_)), Some(Right(count))) => Some(Right(count + 1))
    case (first@Some(Right(count)), Some(Right(_))) => first.copy(Right(count + 1))
    case (first@Some(Right(_)), None) => first
    case _ => None
  }

}
