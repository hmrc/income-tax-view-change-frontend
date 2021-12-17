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
import audit.models.TaxYearOverviewResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import implicits.ImplicitDateFormatter
import models.calculation.{CalcDisplayModel, CalcDisplayNoDataFound, CalcOverview, Calculation}
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialDetailsService, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxYearOverview

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearOverviewController @Inject()(taxYearOverview: TaxYearOverview,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          calculationService: CalculationService,
                                          financialDetailsService: FinancialDetailsService,
                                          incomeSourceDetailsService: IncomeSourceDetailsService,
                                          nextUpdatesService: NextUpdatesService,
                                          auditingService: AuditingService
                                         )(implicit val appConfig: FrontendAppConfig,
                                           val languageUtils: LanguageUtils,
                                           mcc: MessagesControllerComponents,
                                           implicit val ec: ExecutionContext,
                                           val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = false) flatMap { implicit mtdItUser =>
        withCalculation(getClientNino(request), taxYear) { calculationOpt =>
          withTaxYearFinancials(taxYear) { documentDetailsWithDueDates =>
            withObligationsModel(taxYear) { obligations =>
              calculationOpt.foreach(calculation =>
                if (isEnabled(TxmEventsApproved)) {
                  auditingService.extendedAudit(TaxYearOverviewResponseAuditModel(
                    mtdItUser, calculation,
                    documentDetailsWithDueDates, obligations))
                }
              )
              val codingOutEnabled = isEnabled(CodingOut)
              Future.successful(Ok(view(
                taxYear,
                calculationOpt.map(calc => CalcOverview(calc)),
                documentDetailsWithDueDates = documentDetailsWithDueDates,
                obligations = obligations,
                codingOutEnabled = codingOutEnabled
              )(request, mtdItUser)).addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")(request))
            }
          }
        }
      }
  }

  def backUrl(): String = {
    controllers.agent.routes.TaxYearsController.show().url
  }

  private def view(taxYear: Int,
                   calculationOverview: Option[CalcOverview],
                   documentDetailsWithDueDates: List[DocumentDetailWithDueDate],
                   obligations: ObligationsModel,
                   codingOutEnabled: Boolean
                  )(implicit request: Request[_], user: MtdItUser[_]): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overviewOpt = calculationOverview,
      charges = documentDetailsWithDueDates,
      obligations = obligations,
      backUrl = backUrl(),
      isAgent = true,
      codingOutEnabled = codingOutEnabled
    )
  }

  private def withCalculation(nino: String, taxYear: Int)(f: Option[Calculation] => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    calculationService.getCalculationDetail(nino, taxYear) flatMap {
      case CalcDisplayModel(_, _, calculation, _) => f(Some(calculation))
      case CalcDisplayNoDataFound => f(None)
      case _ =>
        Logger("application").error(s"[TaxYearOverviewController][withCalculation] - Could not retrieve calculation for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, documentDetails, _) =>
        val docDetailsNoPayments = documentDetails.filter(_.paymentLot.isEmpty)
        val docDetailsCodingOut = docDetailsNoPayments.filter(_.isCodingOutDocumentDetail(isEnabled(CodingOut)))
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isNotCodingOutDocumentDetail).filter(_.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.latePaymentInterestAmount.isDefined).filter(_.latePaymentInterestAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, isLatePaymentInterest = true,
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOutPaye: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => dd.isPayeSelfAssessment && dd.amountCodedOutIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOut: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => !dd.isPayeSelfAssessment && dd.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        f(documentDetailsWithDueDates ++ documentDetailsWithDueDatesForLpi ++ documentDetailsWithDueDatesCodingOutPaye ++ documentDetailsWithDueDatesCodingOut)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ =>
        Logger("application").error(s"[TaxYearOverviewController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(f: ObligationsModel => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel => f(obligationsModel)
      case _ =>
        Logger("application").error(s"[TaxYearOverviewController][withObligationsModel] - Could not retrieve obligations for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

}
