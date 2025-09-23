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
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.ChargeSummaryController.ErrorCode
import enums.GatewayPage.GatewayPage
import forms.utils.SessionKeys.gatewayPage
import models.admin._
import models.chargeHistory._
import models.chargeSummary.{ChargeSummaryViewModel, PaymentHistoryAllocations}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ChargeHistoryService, DateServiceInterface, FinancialDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.FallBackBackLinks
import views.html.{ChargeSummary, YourSelfAssessmentChargeSummary}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object ChargeSummaryController {
  case class ErrorCode(message: String)
}

class ChargeSummaryController @Inject()(val authActions: AuthActions,
                                        val financialDetailsService: FinancialDetailsService,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        val chargeSummaryView: ChargeSummary,
                                        val yourSelfAssessmentChargeSummary: YourSelfAssessmentChargeSummary,
                                        val chargeHistoryService: ChargeHistoryService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        dateService: DateServiceInterface,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FallBackBackLinks with TransactionUtils with FeatureSwitching {

  def onError(message: String, isAgent: Boolean, showInternalServerError: Boolean)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ChargeSummaryController]${if (isAgent) "[Agent]" else ""}[showChargeSummary]"
    Logger("application").error(s"$errorPrefix $message")
    if (showInternalServerError) {
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
    } else {
      if (isAgent) Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
      else Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
    }
  }

  private def isMFADebit(fdm: FinancialDetailsModel, id: String): Boolean = {
    fdm.financialDetails.exists(fd => fd.transactionId.contains(id) && MfaDebitUtils.isMFADebitMainTransaction(fd.mainTransaction))
  }

  def handleRequest(taxYear: Int, id: String, isInterestCharge: Boolean = false, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    //TODO: Remove multi-year call
    financialDetailsService.getAllFinancialDetails.flatMap { financialResponses =>
      Logger("application").debug(s"- financialResponses = $financialResponses")

      val paymentsFromAllYears = financialResponses.collect {
        case (_, model: FinancialDetailsModel) => model.filterPayments()
      }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None), List(), List(), List()))((merged, next) => merged.mergeLists(next))

      val matchingYear: List[FinancialDetailsResponseModel] = financialResponses.collect {
        case (year, response) if year == taxYear => response
      }
      matchingYear.headOption match {
        case Some(fdmForTaxYear: FinancialDetailsModel) if fdmForTaxYear.asChargeItems.exists(_.transactionId == id) =>
          doShowChargeSummary(taxYear, id, isInterestCharge, fdmForTaxYear, paymentsFromAllYears, isAgent, origin, isMFADebit(fdmForTaxYear, id))
        case Some(_: FinancialDetailsModel) =>
          Future.successful(onError(s"Transaction id not found for tax year $taxYear", isAgent, showInternalServerError = false))
        case Some(error: FinancialDetailsErrorModel) =>
          Future.successful(onError(s"Financial details error :: $error", isAgent, showInternalServerError = true))
        case None =>
          Future.successful(onError("Failed to find related financial detail for tax year and charge ", isAgent, showInternalServerError = true))
      }
    }
  }

  def show(taxYear: Int, id: String, isInterestCharge: Boolean = false, origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual.async {
      implicit user =>
        handleRequest(taxYear, id, isInterestCharge, isAgent = false, origin)
    }

  def showAgent(taxYear: Int, id: String, isInterestCharge: Boolean = false): Action[AnyContent] =
    authActions.asMTDPrimaryAgent.async {
      implicit mtdItUser =>
        handleRequest(taxYear, id, isInterestCharge, isAgent = true)
    }


  private def doShowChargeSummary(taxYear: Int, id: String, isInterestCharge: Boolean,
                                  chargeDetailsforTaxYear: FinancialDetailsModel, paymentsForAllYears: FinancialDetailsModel,
                                  isAgent: Boolean, origin: Option[String],
                                  isMFADebit: Boolean)
                                 (implicit user: MtdItUser[_], dateService: DateServiceInterface): Future[Result] = {

    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
    val documentDetailWithDueDate: DocumentDetailWithDueDate = chargeDetailsforTaxYear.findDocumentDetailByIdWithDueDate(id).get
    val financialDetailsForCharge = chargeDetailsforTaxYear.financialDetails.filter(_.transactionId.contains(id))

    val chargeItem = ChargeItem.fromDocumentPair(
      documentDetailWithDueDate.documentDetail,
      financialDetailsForCharge)

    if (chargeItem.isPenalty && !isEnabled(PenaltiesAndAppeals)){
      Future.successful(onError("Charge type prohibited by feature switches", isAgent, showInternalServerError = true))
    } else {

      val chargeReference: Option[String] = financialDetailsForCharge.headOption match {
        case Some(value) => value.chargeReference
        case None => None
      }
      val paymentBreakdown: List[FinancialDetail] =
        if (!isInterestCharge) {
          financialDetailsForCharge.filter(_.messageKeyByTypes.isDefined)
        } else Nil

      val paymentAllocations: List[PaymentHistoryAllocations] =
        financialDetailsForCharge
          .filter(_.messageKeyByTypes.isDefined)
          .flatMap(chargeFinancialDetail => paymentsForAllYears.getAllocationsToCharge(chargeFinancialDetail))


      chargeHistoryService.chargeHistoryResponse(isInterestCharge, chargeReference, isEnabled(ChargeHistory)).map {
        case Right(chargeHistory) =>
          auditChargeSummary(chargeItem, paymentBreakdown,
            chargeHistory, paymentAllocations, isInterestCharge, isMFADebit, taxYear)

          val chargeItems: List[ChargeItem] = chargeDetailsforTaxYear.asChargeItems
          val (poaOneChargeUrl, poaTwoChargeUrl) = {
            for {
              poaOneChargeItem <- chargeItems
                .filter(c => List(PoaOneDebit, PoaOneReconciliationDebit).contains(c.transactionType))
              poaTwoChargeItem <- chargeItems
                .filter(c => List(PoaTwoDebit, PoaTwoReconciliationDebit).contains(c.transactionType))
            } yield {
              if (isAgent)
                (
                  routes.ChargeSummaryController.showAgent(poaOneChargeItem.taxYear.endYear, poaOneChargeItem.transactionId).url,
                  routes.ChargeSummaryController.showAgent(poaTwoChargeItem.taxYear.endYear, poaTwoChargeItem.transactionId).url
                )
              else
                (routes.ChargeSummaryController.show(poaOneChargeItem.taxYear.endYear, poaOneChargeItem.transactionId).url,
                  routes.ChargeSummaryController.show(poaTwoChargeItem.taxYear.endYear, poaTwoChargeItem.transactionId).url
                )
            }
          }.headOption.getOrElse(("", ""))
          val whatYouOweUrl = {
            if (isAgent) controllers.routes.WhatYouOweController.showAgent().url
            else controllers.routes.WhatYouOweController.show(origin).url
          }
          val saChargesUrl: String = {
            if (isAgent) controllers.routes.YourSelfAssessmentChargesController.showAgent().url
            else controllers.routes.YourSelfAssessmentChargesController.show(origin).url
          }

          chargeReference match {
            case Some(chargeRef) =>
              val LSPUrl = appConfig.incomeTaxPenaltiesFrontend
              val LPPUrl = if (isAgent) appConfig.incomeTaxPenaltiesFrontendLPP1CalculationAgent(chargeRef) else appConfig.incomeTaxPenaltiesFrontendLPP1Calculation(chargeRef)

              val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
                currentDate = dateService.getCurrentDate,
                chargeItem = chargeItem,
                backUrl = getChargeSummaryBackUrl(sessionGatewayPage, taxYear, origin, isAgent),
                gatewayPage = sessionGatewayPage,
                paymentBreakdown = paymentBreakdown,
                paymentAllocations = paymentAllocations,
                payments = paymentsForAllYears,
                chargeHistoryEnabled = isEnabled(ChargeHistory),
                latePaymentInterestCharge = isInterestCharge,
                penaltiesEnabled = isEnabled(PenaltiesAndAppeals),
                reviewAndReconcileCredit = chargeHistoryService.getReviewAndReconcileCredit(chargeItem, chargeDetailsforTaxYear),
                btaNavPartial = user.btaNavPartial,
                isAgent = isAgent,
                adjustmentHistory = chargeHistoryService.getAdjustmentHistory(chargeHistory, documentDetailWithDueDate.documentDetail),
                poaExtraChargeLink = checkForPoaExtraChargeLink(chargeDetailsforTaxYear, documentDetailWithDueDate, isAgent),
                poaOneChargeUrl = poaOneChargeUrl,
                poaTwoChargeUrl = poaTwoChargeUrl,
                LSPUrl = LSPUrl,
                LPPUrl = LPPUrl
              )

              mandatoryViewDataPresent(isInterestCharge, documentDetailWithDueDate) match {
                case Right(_) => Ok {
                  if ((isEnabled(YourSelfAssessmentCharges) && chargeItem.isIncludedInSACSummary) || (chargeItem.transactionType == ITSAReturnAmendment && !chargeItem.isOnlyInterest)) {
                    yourSelfAssessmentChargeSummary(viewModel, whatYouOweUrl, saChargesUrl, isEnabled(YourSelfAssessmentCharges))
                  } else
                    chargeSummaryView(viewModel, whatYouOweUrl, saChargesUrl, isEnabled(YourSelfAssessmentCharges))
                }
                case Left(ec) => onError(s"Invalid response from charge history: ${ec.message}", isAgent, showInternalServerError = true)
              }
            case None => onError("No chargeReference found", isAgent, showInternalServerError = true)
          }
        case _ =>
          onError("Invalid response from charge history", isAgent, showInternalServerError = true)
      }
    }
  }

  private def checkForPoaExtraChargeLink(chargeDetailsForTaxYear: FinancialDetailsModel,
                                         documentDetailWithDueDate: DocumentDetailWithDueDate,
                                         isAgent: Boolean): Option[String] = {
    val chargeItem: Option[ChargeItem] = getChargeItemOpt(chargeDetailsForTaxYear.financialDetails)(documentDetailWithDueDate.documentDetail)

    chargeItem match {
      case Some(value) =>
        val desiredMainTransaction = value.poaLinkForDrilldownPage
        val extraChargeId = chargeDetailsForTaxYear.financialDetails.find(x => x.taxYear == documentDetailWithDueDate.documentDetail.taxYear.toString
          && x.mainTransaction.contains(desiredMainTransaction)).getOrElse(FinancialDetail("9999", items = None)).transactionId

        extraChargeId match {
          case Some(validId) =>
            if (isAgent) Some(controllers.routes.ChargeSummaryController.showAgent(documentDetailWithDueDate.documentDetail.taxYear, validId).url)
            else Some(controllers.routes.ChargeSummaryController.show(documentDetailWithDueDate.documentDetail.taxYear, validId).url)
          case None => None
        }
      case None => None
    }
  }

  def mandatoryViewDataPresent(isLatePaymentCharge: Boolean, documentDetailWithDueDate: DocumentDetailWithDueDate)(implicit user: MtdItUser[_]): Either[ErrorCode, Boolean] = {

    val viewSection1 = isEnabled(ChargeHistory) && (!isLatePaymentCharge && !documentDetailWithDueDate.documentDetail.isPayeSelfAssessment)
    val viewSection2 = isEnabled(ChargeHistory) && isLatePaymentCharge
    val viewSection3 = isEnabled(ChargeHistory) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment

    val values = List(
      (viewSection1, true, "Original Amount"),
      (viewSection2, documentDetailWithDueDate.documentDetail.interestEndDate.isDefined, "Interest EndDate"),
      (viewSection2, documentDetailWithDueDate.documentDetail.accruingInterestAmount.isDefined ||
        documentDetailWithDueDate.documentDetail.interestOutstandingAmount.isDefined, "Interest Amount"),
      (viewSection3, true, "Original Amount")
    )

    val undefinedOptions = values.filter(_._1).flatMap {
      case (_, true, _) => None
      case (_, false, name) => Some(name)
    }

    if (undefinedOptions.isEmpty) Right(true)
    else {
      val valuePhrase = if (undefinedOptions.size > 1) "values" else "value"
      val msg = s"Missing view $valuePhrase: ${undefinedOptions.mkString(", ")}"
      Logger("application").error(msg)
      Left(ErrorCode(msg))
    }
  }


  private def auditChargeSummary(chargeItem: ChargeItem,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentHistoryAllocations], isLatePaymentCharge: Boolean,
                                 isMFADebit: Boolean, taxYear: Int)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
    auditingService.extendedAudit(ChargeSummaryAudit(
      mtdItUser = user,
      chargeItem = chargeItem,
      paymentBreakdown = paymentBreakdown,
      chargeHistories = chargeHistories,
      paymentAllocations = paymentAllocations,
      isLatePaymentCharge = isLatePaymentCharge,
      isMFADebit = isMFADebit,
      taxYear = TaxYear.makeTaxYearWithEndYear(taxYear)
    ))
  }
}
