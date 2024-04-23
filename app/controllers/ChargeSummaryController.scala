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
import connectors.FinancialDetailsConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.GatewayPage
import forms.utils.SessionKeys.gatewayPage
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.FallBackBackLinks
import views.html.ChargeSummary
import views.html.errorPages.CustomNotFoundError
import ChargeSummaryController._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object ChargeSummaryController {

  case class ErrorCode(message: String, code: Int = 0, showInternalServerError: Boolean = true) extends RuntimeException(message)

  type ItvcResponse[T]  = Either[ErrorCode, T]
  type ItvcResult  = Future[Result]

  implicit class ValueToEither[V](v: V) {
    def toRightE: Right[ErrorCode, V] = Right(v)
    def toLeftE[R]: Left[ErrorCode, R] = Left(v.asInstanceOf[ErrorCode])
  }

  implicit class EitherToFuture[T](either: Either[ErrorCode, T]) {
    def toFuture: Future[T] = either match {
      case Right(a) => Future.successful(a)
      case Left(errorCode) => Future.failed(errorCode)
    }
  }

  implicit class TypeToFuture[T](t: T) {
    def toFuture: Future[T] = Future.successful(t)
  }

}

class ChargeSummaryController @Inject()(val authenticate: AuthenticationPredicate,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                        val financialDetailsService: FinancialDetailsService,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val financialDetailsConnector: FinancialDetailsConnector,
                                        val chargeSummaryView: ChargeSummary,
                                        val retrievebtaNavPartial: NavBarPredicate,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val customNotFoundErrorView: CustomNotFoundError)
                                       (implicit val appConfig: FrontendAppConfig,
                                        dateService: DateServiceInterface,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with FallBackBackLinks {

  lazy val action: ActionBuilder[MtdItUser, AnyContent] =
    checkSessionTimeout andThen authenticate andThen retrieveNinoWithIncomeSources andThen retrievebtaNavPartial

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
      Logger("application").debug(s"[ChargeSummaryController][handleRequest] - financialResponses = $financialResponses")
      val payments = financialResponses.collect {
        case (_, model: FinancialDetailsModel) => model.filterPayments()
      }.foldLeft(FinancialDetailsModel(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None), List(), List()))((merged, next) => merged.mergeLists(next))
      val matchingYear: List[FinancialDetailsResponseModel] = financialResponses.collect {
        case (year, response) if year == taxYear => response
      }
      matchingYear.headOption match {
        case Some(fdm: FinancialDetailsModel) if (isDisabled(MFACreditsAndDebits) && isMFADebit(fdm, id)) =>
          Future.successful(Ok(customNotFoundErrorView()))
        case Some(fdm: FinancialDetailsModel) if fdm.documentDetails.exists(_.transactionId == id) =>
          doShowChargeSummary(ChargeSummaryViewRequest(taxYear, id, isLatePaymentCharge, fdm, payments, isAgent, origin, isMFADebit(fdm, id)))
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
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(taxYear, id, isLatePaymentCharge, isAgent = true)
          }
    }

  private def doShowChargeSummary(request: ChargeSummaryViewRequest)
                                 (implicit user: MtdItUser[_], dateService: DateServiceInterface): ItvcResult = {

    def processSteps(): ItvcResult = for {
      sessionGatewayPage <- getGatewayPage(user).toFuture
      documentDetailWithDueDate <- getDocumentDetailWithDueDate(request).toFuture
      financialDetails <- getFinancialDetails(request).toFuture
      paymentBreakdown <- paymentBreakdown(request.isLatePaymentCharge, financialDetails).toFuture
      paymentAllocationEnabled <- isEnabled(PaymentAllocation).toFuture
      paymentAllocations <- paymentAllocations(paymentAllocationEnabled, financialDetails).toFuture
      _ <- mandatoryViewDataPresent(request.isLatePaymentCharge, documentDetailWithDueDate).toFuture
      _ <- codingOutNotDisabled(documentDetailWithDueDate).toFuture
      viewDataE <- fetchChargeHistory(ChargeSummaryViewData(request, sessionGatewayPage, documentDetailWithDueDate, paymentBreakdown, paymentAllocationEnabled, paymentAllocations))
      viewData <- viewDataE.toFuture
      _ <- audit(viewData).toFuture
      result <- view(viewData).toFuture
    } yield result

    processSteps() recover {
      case ec: ErrorCode => onError(ec.message, request.isAgent, showInternalServerError = ec.showInternalServerError)
    }
  }

  def getGatewayPage(user: MtdItUser[_]): Option[GatewayPage] = user.session.get(gatewayPage).map(GatewayPage(_))

  def getDocumentDetailWithDueDate(request: ChargeSummaryViewRequest): ItvcResponse[DocumentDetailWithDueDate] = {
    request.chargeDetails.findDocumentDetailByIdWithDueDate(request.documentNumber)
      .toRight(ErrorCode("DocumentDetailByIdWithDueDate is missing"))
  }

  def getFinancialDetails(request: ChargeSummaryViewRequest): List[FinancialDetail] = {
    request.chargeDetails.financialDetails.filter(_.transactionId.contains(request.documentNumber))
  }

  def paymentBreakdown(isLatePaymentCharge: Boolean, fd: List[FinancialDetail]): List[FinancialDetail] =
    if (!isLatePaymentCharge) {
      fd.filter(_.messageKeyByTypes.isDefined)
    } else Nil

  def paymentAllocations(paymentAllocationEnabled: Boolean, financialDetails: List[FinancialDetail]): List[PaymentsWithChargeType] =
    if (paymentAllocationEnabled) financialDetails.flatMap(_.allocation) else Nil

  def codingOutNotDisabled(data: DocumentDetailWithDueDate): ItvcResponse[Boolean] = {
    if (isDisabled(CodingOut) && (data.documentDetail.isPayeSelfAssessment ||
      data.documentDetail.isClass2Nic ||
      data.documentDetail.isCancelledPayeSelfAssessment))
      ErrorCode("Coding Out is disabled and redirected to not found page", showInternalServerError = false).toLeftE
    else true.toRightE
  }

  def audit(data: ChargeSummaryViewData)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Boolean = {
    auditChargeSummary(data.documentDetailWithDueDate, data.paymentBreakdown, data.chargeHistory,
      data.paymentAllocations, data.request.isLatePaymentCharge, data.request.isMFADebit, data.request.taxYear)
    true
  }

  def view(data: ChargeSummaryViewData)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Result = {

    Ok(chargeSummaryView(
      currentDate = dateService.getCurrentDate,
      documentDetailWithDueDate = data.documentDetailWithDueDate,
      backUrl = getChargeSummaryBackUrl(data.sessionGatewayPage, data.request.taxYear, data.request.origin, data.request.isAgent),
      gatewayPage = data.sessionGatewayPage,
      paymentBreakdown = data.paymentBreakdown,
      chargeHistory = data.chargeHistory,
      paymentAllocations = data.paymentAllocations,
      payments = data.request.payments,
      chargeHistoryEnabled = isEnabled(ChargeHistory),
      paymentAllocationEnabled = data.paymentAllocationEnabled,
      latePaymentInterestCharge = data.request.isLatePaymentCharge,
      codingOutEnabled = isEnabled(CodingOut),
      btaNavPartial = user.btaNavPartial,
      isAgent = data.request.isAgent,
      isMFADebit = data.request.isMFADebit
    ))

  }

  def mandatoryViewDataPresent(isLatePaymentCharge: Boolean, documentDetailWithDueDate: DocumentDetailWithDueDate): ItvcResponse[Boolean] = {


    val viewSection1 = isEnabled(ChargeHistory) && (!isLatePaymentCharge && !(isEnabled(CodingOut) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment))
    val viewSection2 = isEnabled(ChargeHistory) && isLatePaymentCharge
    val viewSection3 = isEnabled(ChargeHistory) && (isEnabled(CodingOut) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment)

    val values = List(
      (viewSection1, documentDetailWithDueDate.documentDetail.originalAmount.isDefined, "Original Amount"),
      (viewSection2, documentDetailWithDueDate.documentDetail.interestEndDate.isDefined, "Interest EndDate"),
      (viewSection2, documentDetailWithDueDate.documentDetail.latePaymentInterestAmount.isDefined, "Late Payment Interest Amount"),
      (viewSection3, documentDetailWithDueDate.documentDetail.originalAmount.isDefined, "Original Amount")
    )

    val undefinedOptions = values.filter(_._1).flatMap {
      case (_, true, _) => None
      case (_, false, name) => Some(name)
    }

    if (undefinedOptions.isEmpty) true.toRightE
    else {
      val valuePhrase = if (undefinedOptions.size > 1) "values" else "value"
      val msg = s"Missing view $valuePhrase: ${undefinedOptions.mkString(", ")}"
      Logger("application").error(msg)
      ErrorCode(msg).toLeftE
    }
  }

  case class ChargeSummaryViewRequest(taxYear: Int, documentNumber: String, isLatePaymentCharge: Boolean,
                                              chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel,
                                              isAgent: Boolean, origin: Option[String], isMFADebit: Boolean)
  case class ChargeSummaryViewData(request: ChargeSummaryViewRequest, sessionGatewayPage: Option[GatewayPage],
                                           documentDetailWithDueDate: DocumentDetailWithDueDate,
                                           paymentBreakdown: List[FinancialDetail], paymentAllocationEnabled: Boolean,
                                           paymentAllocations:  List[PaymentsWithChargeType],
                                           chargeHistory: List[ChargeHistoryModel] = List())

  def fetchChargeHistory(data: ChargeSummaryViewData)
                                   (implicit user: MtdItUser[_]): Future[Either[ErrorCode, ChargeSummaryViewData]] = {

    val makeGetChargeHistoryCall = !data.request.isLatePaymentCharge && isEnabled(ChargeHistory) && !(isEnabled(CodingOut) &&
      data.documentDetailWithDueDate.documentDetail.isPayeSelfAssessment)

    if (makeGetChargeHistoryCall) {

      financialDetailsConnector.getChargeHistory(user.mtditid, data.request.documentNumber).map {
        case chargesHistory: ChargesHistoryModel => Right(data.copy(chargeHistory = chargesHistory.chargeHistoryDetails.getOrElse(Nil)))
        case error: ChargesHistoryErrorModel => Left(ErrorCode(error.message, error.code))
      }

    }
    else Future.successful(Right(data.copy(chargeHistory = Nil)))

  }

  private def auditChargeSummary(documentDetailWithDueDate: DocumentDetailWithDueDate,
                                 paymentBreakdown: List[FinancialDetail], chargeHistories: List[ChargeHistoryModel],
                                 paymentAllocations: List[PaymentsWithChargeType], isLatePaymentCharge: Boolean,
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
