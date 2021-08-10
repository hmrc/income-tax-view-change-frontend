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

package controllers

import audit.AuditingService
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import javax.inject.Inject
import models.chargeHistory.{ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryModel}
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel, PaymentsWithChargeType}
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
																				chargeSummaryView: ChargeSummary)
																			 (implicit val appConfig: FrontendAppConfig,
																				val languageUtils: LanguageUtils,
																				mcc: MessagesControllerComponents,
																				val executionContext: ExecutionContext)
	extends BaseController with FeatureSwitching with I18nSupport {

	def showChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] =
		(checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
			implicit user =>
				financialDetailsService.getAllFinancialDetails(user, implicitly, implicitly).flatMap { financialResponses =>
					val payments = financialResponses.collect {
						case (_, model: FinancialDetailsModel) => model.filterPayments()
					}.foldLeft(FinancialDetailsModel(List(), List()))((merged, next) => merged.merge(next))

					val matchingYear = financialResponses.collect {
						case (year, response) if year == taxYear => response
					}

					matchingYear.headOption match {
						case Some(success: FinancialDetailsModel) if success.documentDetails.exists(_.transactionId == id) =>
							doShowChargeSummary(taxYear, id, isLatePaymentCharge, success, payments)
						case Some(_: FinancialDetailsModel) =>
							Logger.warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
							Future.successful(Redirect(controllers.routes.HomeController.home().url))
						case _ =>
							Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
							Future.successful(itvcErrorHandler.showInternalServerError())
					}
				}
		}

	private def doShowChargeSummary(taxYear: Int, id: String, isLatePaymentCharge: Boolean, chargeDetails: FinancialDetailsModel, payments: FinancialDetailsModel)
																 (implicit user: MtdItUser[_]): Future[Result] = {
		val backLocation = user.session.get(SessionKeys.chargeSummaryBackPage)
		val documentDetail = chargeDetails.documentDetails.find(_.transactionId == id).get
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

		val chargeHistoryEnabled = isEnabled(ChargeHistory)
		val chargeHistoryFuture: Future[Either[ChargeHistoryResponseModel, List[ChargeHistoryModel]]] =
			if (!isLatePaymentCharge && chargeHistoryEnabled) {
				incomeTaxViewChangeConnector.getChargeHistory(user.mtditid, id).map {
					case chargesHistory: ChargesHistoryModel => Right(chargesHistory.chargeHistoryDetails.getOrElse(Nil))
					case errorResponse => Left(errorResponse)
				}
			} else {
				Future.successful(Right(Nil))
			}

		chargeHistoryFuture.map {
			case Right(chargeHistory) =>
				auditChargeSummary(id, chargeDetails)
				Ok(chargeSummaryView(
					documentDetail = documentDetail,
					dueDate = chargeDetails.getDueDateFor(documentDetail),
					backUrl = backUrl(backLocation, taxYear),
					paymentBreakdown = paymentBreakdown,
					chargeHistory = chargeHistory,
					paymentAllocations = paymentAllocations,
					payments = payments,
					chargeHistoryEnabled = chargeHistoryEnabled,
					paymentAllocationEnabled = paymentAllocationEnabled,
					latePaymentInterestCharge = isLatePaymentCharge
				))
			case _ =>
				Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from charge history")
				itvcErrorHandler.showInternalServerError()
		}
	}

	private def auditChargeSummary(id: String, financialDetailsModel: FinancialDetailsModel)
																(implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
		if (isEnabled(TxmEventsApproved)) {
			val documentDetailWithDueDate: DocumentDetailWithDueDate = financialDetailsModel.findDocumentDetailByIdWithDueDate(id).get
			auditingService.extendedAudit(ChargeSummaryAudit(
				mtdItUser = user,
				docDateDetail = documentDetailWithDueDate,
				None
			))
		}
	}

	private def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
		case Some("taxYearOverview") => controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url + "#payments"
		case Some("whatYouOwe") => controllers.routes.WhatYouOweController.viewPaymentsDue().url
		case _ => controllers.routes.HomeController.home().url
	}
}
