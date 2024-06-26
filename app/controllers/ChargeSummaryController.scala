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
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.{ChargeHistoryConnector, FinancialDetailsConnector}
import controllers.ChargeSummaryController.ErrorCode
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.GatewayPage
import forms.utils.SessionKeys.gatewayPage
import models.admin.{ChargeHistory, CodingOut, MFACreditsAndDebits, PaymentAllocation}
import models.chargeHistory._
import models.chargeSummary.PaymentHistoryAllocations
import models.financialDetails._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ChargeHistoryService, DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{AuthenticatorPredicate, FallBackBackLinks}
import views.html.ChargeSummary
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object ChargeSummaryController {
  case class ErrorCode(message: String)
}

class ChargeSummaryController @Inject()(val authenticate: AuthenticationPredicate,
                                        val auth: AuthenticatorPredicate,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                        val financialDetailsService: FinancialDetailsService,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val chargeSummaryView: ChargeSummary,
                                        val retrievebtaNavPartial: NavBarPredicate,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val chargeHistoryService: ChargeHistoryService,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val customNotFoundErrorView: CustomNotFoundError,
                                        val featureSwitchPredicate: FeatureSwitchPredicate)
                                       (implicit val appConfig: FrontendAppConfig,
                                        dateService: DateServiceInterface,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with FallBackBackLinks {

  lazy val action: ActionBuilder[MtdItUser, AnyContent] =
    checkSessionTimeout andThen authenticate andThen retrieveNinoWithIncomeSources andThen featureSwitchPredicate andThen retrievebtaNavPartial

  def onError(message: String, isAgent: Boolean, showInternalServerError: Boolean)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ChargeSummaryController]${if (isAgent) "[Agent]" else ""}[showChargeSummary]"
    Logger("application").error(s"$errorPrefix $message")
    if (showInternalServerError) {
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
    } else {
      if (isAgent) Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show.url)
      else Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
    }
  }

  private def isMFADebit(fdm: FinancialDetailsModel, id: String): Boolean = {
    fdm.financialDetails.exists(fd => fd.transactionId.contains(id) && MfaDebitUtils.isMFADebitMainType(fd.mainType))
  }

  def handleRequest(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    financialDetailsService.getAllFinancialDetails.flatMap { financialResponses =>
      Logger("application").debug(s"- financialResponses = $financialResponses")

      val paymentsFromAllYears = financialResponses.collect {
        case (_, model: FinancialDetailsModel) => model.filterPayments()
      }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None), List(), List()))((merged, next) => merged.mergeLists(next))

      val matchingYear: List[FinancialDetailsResponseModel] = financialResponses.collect {
        case (year, response) if year == taxYear => response
      }
      matchingYear.headOption match {
        case Some(fdm: FinancialDetailsModel) if (!isEnabled(MFACreditsAndDebits) && isMFADebit(fdm, id)) =>
          Future.successful(Ok(customNotFoundErrorView()))
        case Some(fdmForTaxYear: FinancialDetailsModel) if fdmForTaxYear.documentDetails.exists(_.transactionId == id) =>
          doShowChargeSummary(taxYear, id, isLatePaymentCharge, fdmForTaxYear, paymentsFromAllYears, isAgent, origin, isMFADebit(fdmForTaxYear, id))
        case Some(_: FinancialDetailsModel) =>
          Future.successful(onError(s"Transaction id not found for tax year $taxYear", isAgent, showInternalServerError = false))
        case Some(error: FinancialDetailsErrorModel) =>
          Future.successful(onError(s"Financial details error :: $error", isAgent, showInternalServerError = true))
        case None =>
          Future.successful(onError("Failed to find related financial detail for tax year and charge ", isAgent, showInternalServerError = true))
      }
    }
  }

  def show(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false, origin: Option[String] = None): Action[AnyContent] =
    action.async {
      implicit user =>
        handleRequest(taxYear, id, isLatePaymentCharge, isAgent = false, origin)
    }

  def showAgent(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] =
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleRequest(taxYear, id, isLatePaymentCharge, isAgent = true)
    }


  private def doShowChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean,
                                  chargeDetailsforTaxYear: FinancialDetailsModel, paymentsForAllYears: FinancialDetailsModel,
                                  isAgent: Boolean, origin: Option[String],
                                  isMFADebit: Boolean)
                                 (implicit user: MtdItUser[_], dateService: DateServiceInterface): Future[Result] = {
    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
    val documentDetailWithDueDate: DocumentDetailWithDueDate = chargeDetailsforTaxYear.findDocumentDetailByIdWithDueDate(id).get
    val financialDetailsForCharge = chargeDetailsforTaxYear.financialDetails.filter(_.transactionId.contains(id))
    val chargeReference: Option[String] = financialDetailsForCharge.headOption match {
      case Some(value) => value.chargeReference
      case None => None
    }
    val paymentBreakdown: List[FinancialDetail] =
      if (!isLatePaymentCharge) {
        financialDetailsForCharge.filter(_.messageKeyByTypes.isDefined)
      } else Nil
    val paymentAllocationEnabled: Boolean = isEnabled(PaymentAllocation)
    val paymentAllocations: List[PaymentHistoryAllocations] =
      if (paymentAllocationEnabled) {
        financialDetailsForCharge
          .filter(_.messageKeyByTypes.isDefined)
          .flatMap(chargeFinancialDetail => paymentsForAllYears.getAllocationsToCharge(chargeFinancialDetail))
      } else Nil

    chargeHistoryService.chargeHistoryResponse(isLatePaymentCharge, documentDetailWithDueDate.documentDetail.isPayeSelfAssessment,
      chargeReference, isEnabled(ChargeHistory), isEnabled(CodingOut)).map {
      case Right(chargeHistory) =>
        if (!isEnabled(CodingOut) && (documentDetailWithDueDate.documentDetail.isPayeSelfAssessment ||
          documentDetailWithDueDate.documentDetail.isClass2Nic ||
          documentDetailWithDueDate.documentDetail.isCancelledPayeSelfAssessment)) {
          onError("Coding Out is disabled and redirected to not found page", isAgent, showInternalServerError = false)
        } else {
          auditChargeSummary(documentDetailWithDueDate, paymentBreakdown, chargeHistory, paymentAllocations,
            isLatePaymentCharge, isMFADebit, taxYear)

          mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate) match {
            case Right(_) =>
              Ok(chargeSummaryView(
                currentDate = dateService.getCurrentDate,
                documentDetailWithDueDate = documentDetailWithDueDate,
                backUrl = getChargeSummaryBackUrl(sessionGatewayPage, taxYear, origin, isAgent),
                gatewayPage = sessionGatewayPage,
                paymentBreakdown = paymentBreakdown,
                paymentAllocations = paymentAllocations,
                payments = paymentsForAllYears,
                chargeHistoryEnabled = isEnabled(ChargeHistory),
                paymentAllocationEnabled = paymentAllocationEnabled,
                latePaymentInterestCharge = isLatePaymentCharge,
                codingOutEnabled = isEnabled(CodingOut),
                btaNavPartial = user.btaNavPartial,
                isAgent = isAgent,
                isMFADebit = isMFADebit,
                documentType = documentDetailWithDueDate.documentDetail.getDocType,
                adjustmentHistory = chargeHistoryService.getAdjustmentHistory(chargeHistory, documentDetailWithDueDate.documentDetail)
              ))

            case Left(ec) => onError(s"Invalid response from charge history: ${ec.message}", isAgent, showInternalServerError = true)
          }

        }
      case _ =>
        onError("Invalid response from charge history", isAgent, showInternalServerError = true)
    }
  }

  def mandatoryViewDataPresent(isLatePaymentCharge: Boolean, documentDetailWithDueDate: DocumentDetailWithDueDate)(implicit user: MtdItUser[_]): Either[ErrorCode, Boolean] = {

    val viewSection1 = isEnabled(ChargeHistory) && (!isLatePaymentCharge && !(isEnabled(CodingOut) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment))
    val viewSection2 = isEnabled(ChargeHistory) && isLatePaymentCharge
    val viewSection3 = isEnabled(ChargeHistory) && (isEnabled(CodingOut) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment)

    val values = List(
      (viewSection1, true, "Original Amount"),
      (viewSection2, documentDetailWithDueDate.documentDetail.interestEndDate.isDefined, "Interest EndDate"),
      (viewSection2, documentDetailWithDueDate.documentDetail.latePaymentInterestAmount.isDefined, "Late Payment Interest Amount"),
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


  private def auditChargeSummary(documentDetailWithDueDate: DocumentDetailWithDueDate,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentHistoryAllocations], isLatePaymentCharge: Boolean,
                                 isMFADebit: Boolean, taxYear: Int)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
    auditingService.extendedAudit(ChargeSummaryAudit(
      mtdItUser = user,
      docDateDetail = documentDetailWithDueDate,
      paymentBreakdown = paymentBreakdown,
      chargeHistories = chargeHistories,
      paymentAllocations = paymentAllocations,
      isLatePaymentCharge = isLatePaymentCharge,
      isMFADebit = isMFADebit,
      taxYear = taxYear
    ))
  }
}
