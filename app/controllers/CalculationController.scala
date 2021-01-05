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

import auth.MtdItUser
import config.featureswitch.{DeductionBreakdown, FeatureSwitching, IncomeBreakdown, TaxDue}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.{Inject, Singleton}
import models.calculation._
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, TransactionModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialTransactionsService}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.taxYearOverview

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationController @Inject()(authenticate: AuthenticationPredicate,
                                      calculationService: CalculationService,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      financialTransactionsService: FinancialTransactionsService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      retrieveNino: NinoPredicate)
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
                   incomeBreakdown: Boolean = false,
                   taxDue: Boolean = false)(implicit request: Request[_]): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overview = CalcOverview(calculation, transaction),
      transaction = transaction,
      incomeBreakdown = isEnabled(IncomeBreakdown),
      deductionBreakdown = isEnabled(DeductionBreakdown),
      taxDue = isEnabled(TaxDue),
      dateFormatter
    )
  }

  private def showCalculationForYear(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      calculationService.getCalculationDetail(user.nino, taxYear) flatMap {
        case CalcDisplayModel(_, _, calculation, _) =>
          if (calculation.crystallised) {
            financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear) map {
              case _: FinancialTransactionsErrorModel =>
                Logger.error(s"[CalculationController][showCalculationForYear] - Could not retrieve financial transactions model for year: $taxYear")
                itvcErrorHandler.showInternalServerError()
              case financialTransactionsModel: FinancialTransactionsModel =>
                val transaction = financialTransactionsModel.findChargeForTaxYear(taxYear)
                Ok(view(taxYear, calculation, transaction))
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

