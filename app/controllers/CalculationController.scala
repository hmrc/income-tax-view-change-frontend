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
import audit.models.BillsAuditing.BillsAuditModel
import audit.models.{TaxYearOverviewRequestAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation._
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, TransactionModel}
import models.reportDeadlines.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialDetailsService, FinancialTransactionsService, ReportDeadlinesService}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.{taxYearOverview, taxYearOverviewOld}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationController @Inject()(authenticate: AuthenticationPredicate,
                                      calculationService: CalculationService,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      financialTransactionsService: FinancialTransactionsService,
                                      financialDetailsService: FinancialDetailsService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      retrieveNino: NinoPredicate,
                                      reportDeadlinesService: ReportDeadlinesService,
                                      val auditingService: AuditingService)
                                     (implicit val appConfig: FrontendAppConfig,
                                      val languageUtils: LanguageUtils,
                                      mcc: MessagesControllerComponents,
                                      val executionContext: ExecutionContext,
                                      dateFormatter: ImplicitDateFormatterImpl)
  extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  private def viewOld(
                       taxYear: Int,
                       calculation: Calculation,
                       transaction: Option[TransactionModel] = None,
                       charge: Option[DocumentDetailWithDueDate] = None
                     )(implicit request: Request[_]): Html = {
    taxYearOverviewOld(
      taxYear = taxYear,
      overview = CalcOverview(calculation, transaction),
      transaction = transaction,
      charge = charge,
      incomeBreakdown = isEnabled(IncomeBreakdown),
      deductionBreakdown = isEnabled(DeductionBreakdown),
      taxDue = isEnabled(TaxDue),
      dateFormatter,
      backUrl = backUrl
    )
  }

  private def showCalculationForYear(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      calculationService.getCalculationDetail(user.nino, taxYear) flatMap {
        case CalcDisplayModel(_, calcAmount, calculation, _) =>
          auditingService.extendedAudit(BillsAuditModel(user, calcAmount))
          if (calculation.crystallised) {
            if (isEnabled(NewFinancialDetailsApi)) {
              financialDetailsService.getFinancialDetails(taxYear, user.nino) map {
                case _: FinancialDetailsErrorModel =>
                  Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve financial details model for year: $taxYear")
                  itvcErrorHandler.showInternalServerError()
                case financialDetailsModel: FinancialDetailsModel =>
                  val charge = financialDetailsModel.findDocumentDetailForYearWithDueDate(taxYear)
                  Ok(viewOld(taxYear, calculation, charge = charge))
              }
            } else {
              financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear) map {
                case _: FinancialTransactionsErrorModel =>
                  Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve financial transactions model for year: $taxYear")
                  itvcErrorHandler.showInternalServerError()
                case financialTransactionsModel: FinancialTransactionsModel =>
                  val transaction = financialTransactionsModel.findChargeForTaxYear(taxYear)
                  Ok(viewOld(taxYear, calculation, transaction))
              }
            }
          } else {
            Future.successful(Ok(viewOld(taxYear, calculation)))
          }
        case CalcDisplayNoDataFound | CalcDisplayError =>
          Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve calculation for year $taxYear")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }


  private def view(taxYear: Int,
                   calculationOverview: Option[CalcOverview] = None,
                   charge: List[DocumentDetailWithDueDate],
                   obligations: ObligationsModel
                  )(implicit request: Request[_],
                    user: MtdItUser[_]): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overviewOpt = calculationOverview,
      charges = charge,
      obligations,
      dateFormatter,
      backUrl = backUrl
    )
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(documentDetails, _) =>
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          documentDetails.map(documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail)))
        }
        f(documentDetailsWithDueDates)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ => Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(implicit user: MtdItUser[AnyContent]) = {
    reportDeadlinesService.getReportDeadlines(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    )
  }

  private def showTaxYearOverview(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      if (isEnabled(TxmEventsApproved)) {
        auditingService.extendedAudit(TaxYearOverviewRequestAuditModel(user, None))
      }
      calculationService.getCalculationDetail(user.nino, taxYear) flatMap {
        case CalcDisplayModel(_, calcAmount, calculation, _) =>
          withTaxYearFinancials(taxYear) { charges =>
            withObligationsModel(taxYear) map {
              case obligationsModel: ObligationsModel =>
                if (isEnabled(TxmEventsApproved)) {
                  auditingService.extendedAudit(TaxYearOverviewResponseAuditModel(user, None, calculation, charges, obligationsModel))
                }
                Ok(view(taxYear, calculationOverview = Some(CalcOverview(calculation, None)),
                charge = charges, obligations = obligationsModel)).addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")
              case _ => itvcErrorHandler.showInternalServerError()
            }
          }
        case CalcDisplayNoDataFound =>
          withTaxYearFinancials(taxYear) { charges =>
            withObligationsModel(taxYear) map {
              case obligationsModel: ObligationsModel => Ok(view(taxYear, charge = charges,
                obligations = obligationsModel)).addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")
              case _ => itvcErrorHandler.showInternalServerError()
            }
          }
        case CalcDisplayError =>
          Logger.error(s"[CalculationController][showTaxYearOverview] - Could not retrieve calculation for year $taxYear")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def renderTaxYearOverviewPage(taxYear: Int): Action[AnyContent] = {
    if (taxYear > 0) {
      if (isEnabled(TaxYearOverviewUpdate)) {
        showTaxYearOverview(taxYear)
      } else {
        showCalculationForYear(taxYear)
      }
    } else {
      action.async { implicit request =>
        Future.successful(BadRequest(views.html.errorPages.standardError(
          messagesApi.preferred(request)("standardError.heading"),
          messagesApi.preferred(request)("standardError.message")
        )))
      }
    }
  }

  lazy val backUrl: String = controllers.routes.TaxYearsController.viewTaxYears().url

}

