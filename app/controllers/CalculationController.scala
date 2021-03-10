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
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation._
import models.financialDetails.{Charge, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, TransactionModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialDetailsService, FinancialTransactionsService}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.{taxYearOverview, taxYearOverviewOld}
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
                                      val auditingService: AuditingService
                                     )
                                     (implicit val appConfig: FrontendAppConfig,
                                      val languageUtils: LanguageUtils,
                                      mcc: MessagesControllerComponents,
                                      val executionContext: ExecutionContext,
                                      dateFormatter: ImplicitDateFormatterImpl)
                                      extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  private def view(taxYear: Int,
                   calculation: Calculation,
                   transaction: Option[TransactionModel] = None,
                   charge: Option[Charge] = None
                  )(implicit request: Request[_]): Html = {
    if(isEnabled(TaxYearOverviewUpdate)) {
      taxYearOverview(
        taxYear = taxYear,
        overview = CalcOverview(calculation, transaction),
        dateFormatter
      )
    } else {
      taxYearOverviewOld(
        taxYear = taxYear,
        overview = CalcOverview(calculation, transaction),
        transaction = transaction,
        charge = charge,
        incomeBreakdown = isEnabled(IncomeBreakdown),
        deductionBreakdown = isEnabled(DeductionBreakdown),
        taxDue = isEnabled(TaxDue),
        dateFormatter
      )
    }
  }

  private def showCalculationForYear(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      calculationService.getCalculationDetail(user.nino, taxYear) flatMap {
        case CalcDisplayModel(_, calcAmount, calculation, _) =>
          auditingService.extendedAudit(BillsAuditModel(user, calcAmount))
          if (calculation.crystallised) {
            if(isEnabled(NewFinancialDetailsApi)) {
              financialDetailsService.getFinancialDetails(taxYear, user.nino) map {
                case _: FinancialDetailsErrorModel =>
                  Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve financial details model for year: $taxYear")
                  itvcErrorHandler.showInternalServerError()
                case financialDetailsModel: FinancialDetailsModel =>
                  val charge = financialDetailsModel.findChargeForTaxYear(taxYear)
                  Ok(view(taxYear, calculation, charge = charge))
              }
            } else {
              financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear) map {
                case _: FinancialTransactionsErrorModel =>
                  Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve financial transactions model for year: $taxYear")
                  itvcErrorHandler.showInternalServerError()
                case financialTransactionsModel: FinancialTransactionsModel =>
                  val transaction = financialTransactionsModel.findChargeForTaxYear(taxYear)
                  Ok(view(taxYear, calculation, transaction))
              }
            }
          } else {
            Future.successful(Ok(view(taxYear, calculation)))
          }
        case CalcDisplayNoDataFound | CalcDisplayError =>
          Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve calculation for year $taxYear")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def renderCalculationPage(taxYear: Int): Action[AnyContent] = {
    if (taxYear > 0) {
      showCalculationForYear(taxYear)
    } else {
      action.async { implicit request =>
        Future.successful(BadRequest(views.html.errorPages.standardError(
          messagesApi.preferred(request)("standardError.heading"),
          messagesApi.preferred(request)("standardError.message")
        )))
      }
    }
  }

}

