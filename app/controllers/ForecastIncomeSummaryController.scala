/*
 * Copyright 2022 HM Revenue & Customs
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
import auth.MtdItUserWithNino
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ForecastIncomeSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ForecastIncomeSummaryController @Inject()(val forecastIncomeSummaryView: ForecastIncomeSummary,
                                                val checkSessionTimeout: SessionTimeoutPredicate,
                                                val authenticate: AuthenticationPredicate,
                                                val retrieveNino: NinoPredicate,
                                                val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                val calculationService: CalculationService,
                                                val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                                val auditingService: AuditingService,
                                                val retrieveBtaNavBar: BtaNavFromNinoPredicate,
                                                val itvcErrorHandler: ItvcErrorHandler)
                                               (implicit val executionContext: ExecutionContext,
                                        val languageUtils: LanguageUtils,
                                        val appConfig: FrontendAppConfig,
                                        mcc: MessagesControllerComponents)
  extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUserWithNino, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveBtaNavBar


  def show(taxYear: Int): Action[AnyContent] =
    action.async {
      implicit user =>
        calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
          case liabilityCalc: LiabilityCalculationResponse =>
//            println(liabilityCalc.calculation)
            val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
            viewModel match {
              case Some(model) => Ok(forecastIncomeSummaryView(model, taxYear, backUrl(taxYear), isAgent = false, btaNavPartial = user.btaNavPartial))
              case _ =>
                Logger("application").warn(s"[ForecastIncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
                itvcErrorHandler.showInternalServerError()
            }
          case error: LiabilityCalculationError if error.status == NOT_FOUND =>
            Logger("application").error(s"[ForecastIncomeSummaryController][showIncomeSummary[$taxYear]] No income data found.")
            itvcErrorHandler.showInternalServerError()
          case _: LiabilityCalculationError =>
            Logger("application").error(
              s"[ForecastIncomeSummaryController][showIncomeSummary[$taxYear]] No new calc income data error found. Downstream error")
            itvcErrorHandler.showInternalServerError()
        }
    }

  def showAgent(taxYear: Int): Action[AnyContent] =
    action.async {
      implicit user =>
        calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
          case liabilityCalc: LiabilityCalculationResponse =>
            val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
            viewModel match {
              case Some(model) => Ok(forecastIncomeSummaryView(model, taxYear, backUrl(taxYear), isAgent = false, btaNavPartial = user.btaNavPartial))
              case _ =>
                Logger("application").warn(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
                itvcErrorHandler.showInternalServerError()
            }
          case error: LiabilityCalculationError if error.status == NOT_FOUND =>
            Logger("application").info(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data found.")
            itvcErrorHandler.showInternalServerError()
          case _: LiabilityCalculationError =>
            Logger("application").error(
              s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No new calc income data error found. Downstream error")
            itvcErrorHandler.showInternalServerError()
        }
    }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url

}
