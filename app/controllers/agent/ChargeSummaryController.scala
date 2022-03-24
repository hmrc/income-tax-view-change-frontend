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
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import controllers.predicates.IncomeTaxAgentUser
import implicits.ImplicitDateFormatter
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
import views.html.ChargeSummary

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
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(documentDetailWithDueDate: DocumentDetailWithDueDate, chargeHistoryOpt: Option[List[ChargeHistoryModel]], latePaymentInterestCharge: Boolean,
                   backLocation: Option[String], taxYear: Int, paymentAllocations: List[PaymentsWithChargeType], payments: FinancialDetailsModel,
                   paymentBreakdown: List[FinancialDetail], paymentAllocationEnabled: Boolean, codingOutEnabled: Boolean)(implicit request: Request[_]): Html = {

    chargeSummaryView(
      documentDetailWithDueDate = documentDetailWithDueDate,
      chargeHistory = chargeHistoryOpt.getOrElse(Nil),
      latePaymentInterestCharge = latePaymentInterestCharge,
      backUrl = backUrl(backLocation, taxYear),
      paymentAllocations = paymentAllocations,
      payments = payments,
      paymentBreakdown = paymentBreakdown,
      paymentAllocationEnabled = paymentAllocationEnabled,
      codingOutEnabled = codingOutEnabled,
      chargeHistoryEnabled = isEnabled(ChargeHistory),
      isAgent = true
    )
  }

  def showChargeSummary(taxYear: Int, chargeId: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { mtdItUser =>
          financialDetailsService.getAllFinancialDetails(mtdItUser, implicitly, implicitly).flatMap { financialResponses =>
            val payments = financialResponses.collect {
              case (_, model: FinancialDetailsModel) => model.filterPayments()
            }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00), None, List(), List()))((merged, next) => merged.mergeLists(next))

            val matchingYear = financialResponses.collect {
              case (year, response) if year == taxYear => response
            }

            matchingYear.headOption match {
              case Some(financialDetailsModel: FinancialDetailsModel) if financialDetailsModel.documentDetails.exists(_.transactionId == chargeId) =>
                doShowChargeSummary(taxYear, chargeId, isLatePaymentCharge, financialDetailsModel, payments)(hc, mtdItUser, user)
              case Some(_: FinancialDetailsModel) =>
                Logger("application").warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
                Future.successful(Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url))
              case _ =>
                Logger("application").warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
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

    val codingOutEnabled = isEnabled(CodingOut)

    getChargeHistory(id, isLatePaymentCharge).map { chargeHistoryOpt =>
      if (isDisabled(CodingOut) && (documentDetailWithDueDate.documentDetail.isPayeSelfAssessment ||
        documentDetailWithDueDate.documentDetail.isClass2Nic ||
        documentDetailWithDueDate.documentDetail.isCancelledPayeSelfAssessment)) {
        Logger("application").warn(s"[ChargeSummaryController][showChargeSummary] Coding Out is disabled and redirected to not found page")
        Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
      } else {
         auditChargeSummary(documentDetailWithDueDate, paymentBreakdown, chargeHistoryOpt.getOrElse(List.empty), paymentAllocations, isLatePaymentCharge)
       Ok(view(documentDetailWithDueDate, chargeHistoryOpt, isLatePaymentCharge, backLocation, taxYear,
         paymentAllocations = paymentAllocations,
         paymentBreakdown = paymentBreakdown,
         paymentAllocationEnabled = paymentAllocationEnabled,
         payments = payments,
         codingOutEnabled = codingOutEnabled))
        }
    }
  }

  private def auditChargeSummary(documentDetailWithDueDate: DocumentDetailWithDueDate,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentsWithChargeType], isLatePaymentCharge: Boolean)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_], incomeTaxAgentUser: IncomeTaxAgentUser): Unit = {
      auditingService.extendedAudit(ChargeSummaryAudit(
        mtdItUser = user,
        docDateDetail = documentDetailWithDueDate,
        paymentBreakdown = paymentBreakdown,
        chargeHistories = chargeHistories,
        paymentAllocations = paymentAllocations,
        agentReferenceNumber = incomeTaxAgentUser.agentReferenceNumber,
        isLatePaymentCharge = isLatePaymentCharge
      ))
  }

  def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
    case Some("taxYearOverview") => controllers.agent.routes.TaxYearOverviewController.show(taxYear).url + "#payments"
    case Some("whatYouOwe") => controllers.routes.WhatYouOweController.showAgent().url
    case _ => controllers.routes.HomeController.showAgent().url
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
