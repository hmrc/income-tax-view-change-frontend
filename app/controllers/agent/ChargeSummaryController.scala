/*
 * Copyright 2021 HM Revenue & Customs
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
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import controllers.predicates.IncomeTaxAgentUser
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.ChargeSummary

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ChargeSummaryController @Inject()(chargeSummaryView: ChargeSummary,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        financialDetailsService: FinancialDetailsService,
                                        incomeSourceDetailsService: IncomeSourceDetailsService,
                                        auditingService: AuditingService
                                       )(implicit val appConfig: FrontendAppConfig,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         dateFormatter: ImplicitDateFormatterImpl,
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(documentDetailWithDueDate: DocumentDetailWithDueDate, chargeHistoryOpt: Option[List[ChargeHistoryModel]], latePaymentInterestCharge: Boolean,
                   backLocation: Option[String], taxYear: Int, paymentAllocations: List[PaymentsWithChargeType], payments: FinancialDetailsModel,
                   paymentBreakdown: List[FinancialDetail], paymentAllocationEnabled: Boolean)(implicit request: Request[_]): Html = {

    chargeSummaryView(
      documentDetailWithDueDate = documentDetailWithDueDate,
      chargeHistoryOpt = chargeHistoryOpt,
      latePaymentInterestCharge = latePaymentInterestCharge,
      backUrl = backUrl(backLocation, taxYear),
			paymentAllocations = paymentAllocations,
			payments = payments,
			paymentBreakdown = paymentBreakdown,
			paymentAllocationEnabled = paymentAllocationEnabled
    )
  }

  def showChargeSummary(taxYear: Int, chargeId: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { mtdItUser =>
          financialDetailsService.getAllFinancialDetails(mtdItUser, implicitly, implicitly).flatMap { financialResponses =>
						val payments = financialResponses.collect {
							case (_, model: FinancialDetailsModel) => model.filterPayments()
						}.foldLeft(FinancialDetailsModel(List(), List()))((merged, next) => merged.merge(next))

						val matchingYear = financialResponses.collect {
							case (year, response) if year == taxYear => response
						}

            matchingYear.headOption match {
              case Some(financialDetailsModel: FinancialDetailsModel) if financialDetailsModel.documentDetails.exists(_.transactionId == chargeId) =>
                doShowChargeSummary(taxYear, chargeId, isLatePaymentCharge, financialDetailsModel, payments)(hc, mtdItUser, user)
              case Some(_: FinancialDetailsModel) =>
                Logger.warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
                Future.successful(Redirect(controllers.agent.routes.HomeController.show().url))
              case _ =>
                Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
                Future.successful(itvcErrorHandler.showInternalServerError())
            }
          }
        }
    }
  }

  private def doShowChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean, chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel)
                                 (implicit hc: HeaderCarrier, user: MtdItUser[_], incomeTaxAgentUser: IncomeTaxAgentUser): Future[Result] = {
    val backLocation = user.session.get(SessionKeys.chargeSummaryBackPage)
    val documentDetailWithDueDate: DocumentDetailWithDueDate = chargeDetails.findDocumentDetailByIdWithDueDate(id).get
    val financialDetailsModel = chargeDetails.financialDetails.filter(_.transactionId.contains(id))


    val paymentBreakdown: List[FinancialDetail] =
      if (!isLatePaymentCharge) {
        financialDetailsModel.filter(_.messageKeyByTypes.isDefined)
      } else Nil


    val paymentAllocationEnabled: Boolean = isEnabled(PaymentAllocation)
    val paymentAllocations: List[PaymentsWithChargeType] =
      if (paymentAllocationEnabled) {
        financialDetailsModel.flatMap(_.allocation)
      } else Nil


    getChargeHistory(id, isLatePaymentCharge).map { chargeHistoryOpt =>
      auditChargeSummary(documentDetailWithDueDate)
      Ok(view(documentDetailWithDueDate, chargeHistoryOpt, isLatePaymentCharge, backLocation, taxYear,
        paymentAllocations = paymentAllocations,
        paymentBreakdown = paymentBreakdown,
        paymentAllocationEnabled = paymentAllocationEnabled,
				payments = payments))
    }
  }

  private def auditChargeSummary(documentDetailWithDueDate: DocumentDetailWithDueDate)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_], incomeTaxAgentUser: IncomeTaxAgentUser): Unit = {
    if (isEnabled(TxmEventsApproved)) {
      auditingService.extendedAudit(ChargeSummaryAudit(
        mtdItUser = user,
        docDateDetail = documentDetailWithDueDate,
        agentReferenceNumber = incomeTaxAgentUser.agentReferenceNumber
      ))
    }
  }

  def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
    case Some("taxYearOverview") => controllers.agent.routes.TaxYearOverviewController.show(taxYear).url + "#payments"
    case Some("paymentDue") => controllers.agent.routes.WhatYouOweController.show().url
    case _ => controllers.agent.routes.HomeController.show().url
  }

  private def getChargeHistory(chargeId: String, isLatePaymentCharge: Boolean)(implicit req: Request[_]): Future[Option[List[ChargeHistoryModel]]] = {
    ChargeHistory.fold(
      ifDisabled = Future.successful(None),
      ifEnabled = if (isLatePaymentCharge) Future.successful(Some(Nil)) else {
        financialDetailsService.getChargeHistoryDetails(getClientMtditid, chargeId)
          .map(historyListOpt => historyListOpt.map(sortHistory) orElse Some(Nil))
      }
    )
  }

  private def sortHistory(list: List[ChargeHistoryModel]): List[ChargeHistoryModel] = {
    list.sortBy(chargeHistory => chargeHistory.reversalDate)
  }

}
