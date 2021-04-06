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

import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalcDisplayModel, CalcOverview, Calculation}
import models.financialDetails.{Charge, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.financialTransactions.TransactionModel
import models.reportDeadlines.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialDetailsService, IncomeSourceDetailsService, ReportDeadlinesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.TaxYearOverview

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearOverviewController @Inject()(taxYearOverview: TaxYearOverview,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          calculationService: CalculationService,
                                          financialDetailsService: FinancialDetailsService,
                                          incomeSourceDetailsService: IncomeSourceDetailsService,
                                          reportDeadlinesService: ReportDeadlinesService
                                         )(implicit val appConfig: FrontendAppConfig,
                                           val languageUtils: LanguageUtils,
                                           mcc: MessagesControllerComponents,
                                           dateFormatter: ImplicitDateFormatterImpl,
                                           implicit val ec: ExecutionContext,
                                           val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(AgentViewer)) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { implicit mtdItUser =>
          withCalculation(getClientNino(request), taxYear) { calculation =>
            withTaxYearFinancials(taxYear) { charges =>
              withObligationsModel(taxYear) { obligations =>
                Future.successful(Ok(view(taxYear, calculation, charges = charges, obligations = obligations)(request, mtdItUser))
                  .addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")(request))
              }
            }
          }
        }
      } else {
        Future.failed(new NotFoundException("[TaxYearOverviewController][show] - Agent viewer is disabled"))
      }
  }

  def backUrl(): String = {
    //todo: update to return to the tax years page when implemented
    controllers.agent.routes.HomeController.show().url
  }

  private def view(taxYear: Int,
                   calculation: Calculation,
                   charges: List[Charge],
                   obligations: ObligationsModel
                  )(implicit request: Request[_], user: MtdItUser[_]): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overview = CalcOverview(calculation, None),
      charges = charges,
      obligations = obligations,
      implicitDateFormatter = dateFormatter,
      backUrl = backUrl()
    )
  }

  private def withCalculation(nino: String, taxYear: Int)(f: Calculation => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    calculationService.getCalculationDetail(nino, taxYear) flatMap {
      case CalcDisplayModel(_, _, calculation, _) => f(calculation)
      case _ =>
        Logger.error(s"[TaxYearOverviewController][withCalculation] - Could not retrieve calculation for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[Charge] => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case FinancialDetailsModel(charges) => f(charges)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ =>
        Logger.error(s"[TaxYearOverviewController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(f: ObligationsModel => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    reportDeadlinesService.getReportDeadlines(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel => f(obligationsModel)
      case _ =>
        Logger.error(s"[TaxYearOverviewController][withObligationsModel] - Could not retrieve obligations for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

}
