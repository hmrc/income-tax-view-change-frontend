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
import audit.models.{TaxYearOverviewRequestAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys
import models.calculation._
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.reportDeadlines.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialDetailsService, ReportDeadlinesService}
import views.html.TaxYearOverview
import views.html.errorPages.StandardError

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearOverviewController @Inject()(taxYearOverviewView: TaxYearOverview,
                                          standardErrorView: StandardError,
                                          authenticate: AuthenticationPredicate,
                                          calculationService: CalculationService,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          financialDetailsService: FinancialDetailsService,
                                          itvcErrorHandler: ItvcErrorHandler,
                                          retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          retrieveNino: NinoPredicate,
                                          reportDeadlinesService: ReportDeadlinesService,
                                          val auditingService: AuditingService)
                                         (implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  private def view(taxYear: Int,
                   calculationOverview: Option[CalcOverview] = None,
                   charge: List[DocumentDetailWithDueDate],
                   obligations: ObligationsModel
                  )(implicit user: MtdItUser[_]): Html = {
    taxYearOverviewView(
      taxYear = taxYear,
      overviewOpt = calculationOverview,
      charges = charge,
      obligations,
      backUrl = backUrl
    )
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(documentDetails, _) =>
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          documentDetails.filter(_.paymentLot.isEmpty).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail)))
        }
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          documentDetails.filter(_.paymentLot.isEmpty).filter(_.latePaymentInterestAmount.isDefined).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, true))
        }
        f((documentDetailsWithDueDates ++ documentDetailsWithDueDatesForLpi))
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
        showTaxYearOverview(taxYear)
    } else {
      action.async { implicit request =>
        Future.successful(BadRequest(standardErrorView(
          messagesApi.preferred(request)("standardError.heading"),
          messagesApi.preferred(request)("standardError.message")
        )))
      }
    }
  }

  lazy val backUrl: String = controllers.routes.TaxYearsController.viewTaxYears().url

}

