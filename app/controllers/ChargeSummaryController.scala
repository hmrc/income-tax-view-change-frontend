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
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, CodingOut, FeatureSwitching, PaymentAllocation, TxmEventsApproved, TxmEventsR6}
import config.{FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import javax.inject.Inject
import models.chargeHistory.{ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryModel}
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel, PaymentsWithChargeType}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.FinancialDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ChargeSummary

import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryController @Inject()(authenticate: AuthenticationPredicate,
                                        checkSessionTimeout: SessionTimeoutPredicate,
                                        retrieveNino: NinoPredicate,
                                        retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        financialDetailsService: FinancialDetailsService,
                                        auditingService: AuditingService,
                                        itvcErrorHandler: ItvcErrorHandler,
                                        incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                        chargeSummaryView: ChargeSummary,
                                        retrievebtaNavPartial: BtaNavBarPredicate)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  def showChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrievebtaNavPartial).async {
      implicit user =>
        financialDetailsService.getAllFinancialDetails(user, implicitly, implicitly).flatMap { financialResponses =>
          val payments = financialResponses.collect {
            case (_, model: FinancialDetailsModel) => model.filterPayments()
          }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00), List(), List()))((merged, next) => merged.mergeLists(next))

          val matchingYear = financialResponses.collect {
            case (year, response) if year == taxYear => response
          }

          matchingYear.headOption match {
            case Some(success: FinancialDetailsModel) if success.documentDetails.exists(_.transactionId == id) =>
              doShowChargeSummary(taxYear, id, isLatePaymentCharge, success, payments)
            case Some(_: FinancialDetailsModel) =>
              Logger("application").warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
              Future.successful(Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url))
            case _ =>
              Logger("application").warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
    }

  private def doShowChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean, chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel)
                                 (implicit user: MtdItUser[_]): Future[Result] = {
    val backLocation = user.session.get(SessionKeys.chargeSummaryBackPage)
    val documentDetailWithDueDate: DocumentDetailWithDueDate = chargeDetails.findDocumentDetailByIdWithDueDate(id).get
    val financialDetails = chargeDetails.financialDetails.filter(_.transactionId.contains(id))

    val paymentBreakdown: List[FinancialDetail] =
      if (!isLatePaymentCharge) {
        financialDetails.filter(_.messageKeyByTypes.isDefined)
      } else Nil

    val paymentAllocationEnabled: Boolean = isEnabled(PaymentAllocation)
    val paymentAllocations: List[PaymentsWithChargeType] =
      if (paymentAllocationEnabled) {
        financialDetails.flatMap(_.allocation)
      } else Nil

    chargeHistoryResponse(isLatePaymentCharge, documentDetailWithDueDate.documentDetail.isPayeSelfAssessment, id).map {
      case Right(chargeHistory) =>
        auditChargeSummary(id, chargeDetails, paymentBreakdown, chargeHistory, paymentAllocations, isLatePaymentCharge)
        Ok(chargeSummaryView(
          documentDetailWithDueDate = documentDetailWithDueDate,
          backUrl = backUrl(backLocation, taxYear),
          paymentBreakdown = paymentBreakdown,
          chargeHistory = chargeHistory,
          paymentAllocations = paymentAllocations,
          payments = payments,
          chargeHistoryEnabled = isEnabled(ChargeHistory),
          paymentAllocationEnabled = paymentAllocationEnabled,
          latePaymentInterestCharge = isLatePaymentCharge,
          codingOutEnabled = isEnabled(CodingOut),
          btaNavPartial = user.btaNavPartial
        ))
      case _ =>
        Logger("application").warn("[ChargeSummaryController][showChargeSummary] Invalid response from charge history")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def chargeHistoryResponse(isLatePaymentCharge: Boolean, isPayeSelfAssessment: Boolean, documentNumber: String)
                                   (implicit user: MtdItUser[_]): Future[Either[ChargeHistoryResponseModel, List[ChargeHistoryModel]]] = {
    if (!isLatePaymentCharge && isEnabled(ChargeHistory) && !(isEnabled(CodingOut) && isPayeSelfAssessment)) {
      incomeTaxViewChangeConnector.getChargeHistory(user.mtditid, documentNumber).map {
        case chargesHistory: ChargesHistoryModel => Right(chargesHistory.chargeHistoryDetails.getOrElse(Nil))
        case errorResponse => Left(errorResponse)
      }
    } else {
      Future.successful(Right(Nil))
    }
  }

  private def auditChargeSummary(id: String, financialDetailsModel: FinancialDetailsModel,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentsWithChargeType], isLatePaymentCharge: Boolean)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
    if (isEnabled(TxmEventsApproved)) {
      val documentDetailWithDueDate: DocumentDetailWithDueDate = financialDetailsModel.findDocumentDetailByIdWithDueDate(id).get
      auditingService.extendedAudit(ChargeSummaryAudit(
        mtdItUser = user,
        docDateDetail = documentDetailWithDueDate,
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocations,
        None,
        isEnabled(TxmEventsR6),
        isLatePaymentCharge = isLatePaymentCharge
      ))
    }
  }

  private def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
    case Some("taxYearOverview") => controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url + "#payments"
    case Some("whatYouOwe") => controllers.routes.WhatYouOweController.show().url
    case _ => controllers.routes.HomeController.home().url
  }
}
