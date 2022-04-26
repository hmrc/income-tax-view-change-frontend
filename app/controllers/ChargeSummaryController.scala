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
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import forms.utils.SessionKeys.gatewayPage
import models.chargeHistory.{ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryModel}
import models.financialDetails._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.FallBackBackLinks
import views.html.ChargeSummary

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryController @Inject()(val authenticate: AuthenticationPredicate,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val retrieveNino: NinoPredicate,
                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        val financialDetailsService: FinancialDetailsService,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                        val chargeSummaryView: ChargeSummary,
                                        val retrievebtaNavPartial: NavBarPredicate,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val authorisedFunctions: FrontendAuthorisedFunctions)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with FallBackBackLinks {

  val action: ActionBuilder[MtdItUser, AnyContent] =
    checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrievebtaNavPartial

  def onError(message: String, isAgent: Boolean, showInternalServerError: Boolean)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ForecastTaxCalcSummaryController]${if (isAgent) "[Agent]" else ""}[showChargeSummary]"
    Logger("application").error(s"$errorPrefix $message")
    if(showInternalServerError) {
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
    } else {
      if(isAgent) Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
      else Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
    }
  }

  def handleRequest(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    financialDetailsService.getAllFinancialDetails(user, implicitly, implicitly).flatMap { financialResponses =>
      val payments = financialResponses.collect {
        case (_, model: FinancialDetailsModel) => model.filterPayments()
      }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00, None, None, None), None, List(), List()))((merged, next) => merged.mergeLists(next))

      val matchingYear = financialResponses.collect {
        case (year, response) if year == taxYear => response
      }

      matchingYear.headOption match {
        case Some(success: FinancialDetailsModel) if success.documentDetails.exists(_.transactionId == id) =>
          doShowChargeSummary(taxYear, id, isLatePaymentCharge, success, payments, isAgent, origin)
        case Some(_: FinancialDetailsModel) =>
          Future.successful(onError(s"Transaction id not found for tax year $taxYear", isAgent, showInternalServerError = false))
        case _ =>
          Future.successful(onError("Invalid response from financial transactions", isAgent, showInternalServerError = true))
      }
    }
  }

  def show(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false, origin: Option[String] = None): Action[AnyContent] =
    action.async {
      implicit user =>
        handleRequest(taxYear, id, isLatePaymentCharge, isAgent = false, origin)
    }

  def showAgent(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap {
            implicit mtdItUser =>
            handleRequest(taxYear, id, isLatePaymentCharge, isAgent = true)
          }
    }


  private def doShowChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean,
                                  chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel,
                                  isAgent: Boolean, origin: Option[String])
                                 (implicit user: MtdItUser[_]): Future[Result] = {
    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
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
        if (isDisabled(CodingOut) && (documentDetailWithDueDate.documentDetail.isPayeSelfAssessment ||
          documentDetailWithDueDate.documentDetail.isClass2Nic ||
          documentDetailWithDueDate.documentDetail.isCancelledPayeSelfAssessment)) {
          onError("Coding Out is disabled and redirected to not found page", isAgent, showInternalServerError = false)
        } else {
          auditChargeSummary(documentDetailWithDueDate, paymentBreakdown, chargeHistory, paymentAllocations, isLatePaymentCharge)
          Ok(chargeSummaryView(
            documentDetailWithDueDate = documentDetailWithDueDate,
            backUrl = getChargeSummaryBackUrl(sessionGatewayPage, taxYear, origin, isAgent),
            gatewayPage = sessionGatewayPage,
            paymentBreakdown = paymentBreakdown,
            chargeHistory = chargeHistory,
            paymentAllocations = paymentAllocations,
            payments = payments,
            chargeHistoryEnabled = isEnabled(ChargeHistory),
            paymentAllocationEnabled = paymentAllocationEnabled,
            latePaymentInterestCharge = isLatePaymentCharge,
            codingOutEnabled = isEnabled(CodingOut),
            btaNavPartial = user.btaNavPartial,
            isAgent = isAgent
          ))
        }
      case _ =>
        onError("Invalid response from charge history", isAgent, showInternalServerError = true)
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

  private def auditChargeSummary(documentDetailWithDueDate: DocumentDetailWithDueDate,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentsWithChargeType], isLatePaymentCharge: Boolean)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {

    auditingService.extendedAudit(ChargeSummaryAudit(
      mtdItUser = user,
      docDateDetail = documentDetailWithDueDate,
      paymentBreakdown = paymentBreakdown,
      chargeHistories = chargeHistories,
      paymentAllocations = paymentAllocations,
      isLatePaymentCharge = isLatePaymentCharge
    ))
  }
}
