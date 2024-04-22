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
import models.chargeHistory.{ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails.{FinancialDetail, _}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.FallBackBackLinks
import views.html.ChargeSummary
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import controllers.ChargeSummaryController._

object ChargeSummaryController {
  case class ErrorCode(message: String, code: Int = 0, showInternalServerError: Boolean = true) extends RuntimeException(message)
  type ItvcResponse[T]  = Either[ErrorCode, T]
  type ItvcResult  = Future[Result]
  implicit def toEither[T](t: T): ItvcResponse[T] = Right(t)
  implicit def toEither[T](ec: ErrorCode): ItvcResponse[T] = Left(ec)
  implicit def toF[A](e: Either[ErrorCode, A]): Future[A] = e match {
    case Right(a) => Future.successful(a)
    case Left(errorCode) => Future.failed(errorCode)
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

  val action: ActionBuilder[MtdItUser, AnyContent] =
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

  //to be removed
  private def chargeHistoryResponse(isLatePaymentCharge: Boolean, isPayeSelfAssessment: Boolean, documentNumber: String)
                                     (implicit user: MtdItUser[_]): Future[Either[ChargeHistoryResponseModel, List[ChargeHistoryModel]]] = {
      if (!isLatePaymentCharge && isEnabled(ChargeHistory) && !(isEnabled(CodingOut) && isPayeSelfAssessment)) {
        financialDetailsConnector.getChargeHistory(user.mtditid, documentNumber).map {
          case chargesHistory: ChargesHistoryModel => Right(chargesHistory.chargeHistoryDetails.getOrElse(Nil))
          case errorResponse => Left(errorResponse)
        }
      } else {
        Future.successful(Right(Nil))
      }
    }

    //to be removed
    def doShowChargeSummary2(taxYear: Int, id: String, isLatePaymentCharge: Boolean,
                                    chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel,
                                    isAgent: Boolean, origin: Option[String],
                                    isMFADebit: Boolean)
                                   (implicit user: MtdItUser[_], dateService: DateServiceInterface): Future[Result] = {

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
            auditChargeSummary(documentDetailWithDueDate, paymentBreakdown, chargeHistory, paymentAllocations,
              isLatePaymentCharge, isMFADebit, taxYear)
            Ok(chargeSummaryView(
              currentDate = dateService.getCurrentDate,
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
              isAgent = isAgent,
              isMFADebit = isMFADebit
            ))
          }
        case _ =>
          onError("Invalid response from charge history", isAgent, showInternalServerError = true)
      }
    }

  private def doShowChargeSummary(request: ChargeSummaryViewRequest)
                                 (implicit user: MtdItUser[_], dateService: DateServiceInterface): ItvcResult = {

    def processSteps(): ItvcResult = for {
      sessionGatewayPage <- toF(getGatewayPage())
      documentDetailWithDueDate <- toF(request.chargeDetails.findDocumentDetailByIdWithDueDate(request.documentNumber).toRight(ErrorCode("missing DocumentDetailByIdWithDueDate")))
      financialDetails <- toF(Right(request.chargeDetails.financialDetails.filter(_.transactionId.contains(request.documentNumber))))
      paymentBreakdown <- toF(paymentBreakdown(request.isLatePaymentCharge, financialDetails))
      paymentAllocationEnabled <- toF(isEnabled(PaymentAllocation))
      paymentAllocations <- toF(paymentAllocations(paymentAllocationEnabled, paymentBreakdown))
      _ <- toF(mandatoryViewDataPresent(request.isLatePaymentCharge, documentDetailWithDueDate))
      _ <- toF(checkIfCodingOutIsDisabled(documentDetailWithDueDate))
      viewDataE <- fetchChargeHistory(ChargeSummaryViewData(request, sessionGatewayPage, documentDetailWithDueDate, paymentBreakdown, paymentAllocationEnabled, paymentAllocations))
      viewData <- toF(viewDataE)
      _ <- toF(auditChargeSummary(viewData))
      result <- toF(processChargeSummaryView(viewData))
    } yield result

    processSteps() recover {
      case ec: ErrorCode => onError(ec.message, request.isAgent, showInternalServerError = ec.showInternalServerError)
    }
  }

  def getGatewayPage()(implicit user: MtdItUser[_]): ItvcResponse[Option[GatewayPage]] = {
    user.session.get(gatewayPage).map(GatewayPage(_))
  }
  def paymentBreakdown(isLatePaymentCharge: Boolean, fd: List[FinancialDetail]): ItvcResponse[List[FinancialDetail]] =
    if (!isLatePaymentCharge) fd.filter(_.messageKeyByTypes.isDefined) else Nil
  def paymentAllocations(paymentAllocationEnabled: Boolean, financialDetails: List[FinancialDetail]): ItvcResponse[List[PaymentsWithChargeType]] =
    if (paymentAllocationEnabled) financialDetails.flatMap(_.allocation) else Nil
  def checkIfCodingOutIsDisabled(data: DocumentDetailWithDueDate): ItvcResponse[Boolean] = {
    if (isDisabled(CodingOut) && (data.documentDetail.isPayeSelfAssessment ||
      data.documentDetail.isClass2Nic ||
      data.documentDetail.isCancelledPayeSelfAssessment))
      ErrorCode("Coding Out is disabled and redirected to not found page", showInternalServerError = false)
    else true
  }
  def auditChargeSummary(data: ChargeSummaryViewData)(implicit hc: HeaderCarrier, user: MtdItUser[_]): ItvcResponse[Boolean] = {
    auditChargeSummary(data.documentDetailWithDueDate, data.paymentBreakdown, data.chargeHistory,
      data.paymentAllocations, data.request.isLatePaymentCharge, data.request.isMFADebit, data.request.taxYear)
    true
  }

  def processChargeSummaryView(data: ChargeSummaryViewData)
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]): ItvcResponse[Result] = {
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

    //to-remove//val v = documentDetailWithDueDate.copy(documentDetail = documentDetailWithDueDate.documentDetail.copy(originalAmount = Some(10)))

    val viewSection1 = isEnabled(ChargeHistory) && (!isLatePaymentCharge && !(isEnabled(CodingOut) && documentDetailWithDueDate.documentDetail.isPayeSelfAssessment))
    val viewSection2 = isEnabled(ChargeHistory) && isLatePaymentCharge

    val values = List(
      (viewSection1, documentDetailWithDueDate.documentDetail.originalAmount.isDefined, "Original Amount"),
      (viewSection2, documentDetailWithDueDate.documentDetail.interestEndDate.isDefined, "Interest EndDate"),
      (viewSection2, documentDetailWithDueDate.documentDetail.latePaymentInterestAmount.isDefined, "Late Payment Interest Amount"),
    )

    val undefinedOptions = values.filter(_._1).flatMap {
      case (_, true, _) => None
      case (_, false, name) => Some(name)
    }

    if (undefinedOptions.isEmpty) true
    else {
      val valuePhrase = if (undefinedOptions.size > 1) "values" else "value"
      val msg = s"Missing view $valuePhrase: ${undefinedOptions.mkString(", ")}"
      Logger("application").error(msg)
      ErrorCode(msg)
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
