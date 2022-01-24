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
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.calculation._
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import models.liabilitycalculation.viewModels.IncomeBreakdownViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.{IncomeBreakdown, IncomeBreakdownOld}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSummaryController @Inject()(val incomeBreakdownOld: IncomeBreakdownOld,
                                        val incomeBreakdown: IncomeBreakdown,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val authenticate: AuthenticationPredicate,
                                        val retrieveNino: NinoPredicate,
                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        val calculationService: CalculationService,
                                        val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler)
                                       (implicit val executionContext: ExecutionContext,
                                        val languageUtils: LanguageUtils,
                                        val appConfig: FrontendAppConfig,
                                        mcc: MessagesControllerComponents)
                                        extends BaseController with ImplicitDateFormatter with FeatureSwitching  with I18nSupport {

  val action: ActionBuilder[MtdItUserWithNino, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino


  def showIncomeSummary(taxYear: Int): Action[AnyContent] =
    action.async {
      implicit user =>
        if (isEnabled(NewTaxCalcProxy)) {
          calculationService.getLiabilityCalculationDetail(user.nino, taxYear).map {
            case liabilityCalc: LiabilityCalculationResponse =>
              val viewModel = IncomeBreakdownViewModel(liabilityCalc.calculation)
              viewModel match {
                case Some(model) => Ok(incomeBreakdown(model, taxYear, backUrl(taxYear), isAgent = false))
                case _ =>
                  Logger("application").warn(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
                  itvcErrorHandler.showInternalServerError()
              }

            case _: LiabilityCalculationError =>
              Logger("application").error(
                s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No new calc income data error found. Downstream error")
              itvcErrorHandler.showInternalServerError()
          }
        } else {
          calculationService.getCalculationDetail(user.nino, taxYear).flatMap {
            case calcDisplayModel: CalcDisplayModel =>
              Future.successful(Ok(incomeBreakdownOld(calcDisplayModel, taxYear, backUrl(taxYear))))

            case CalcDisplayNoDataFound =>
              Logger("application").warn(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
              Future.successful(itvcErrorHandler.showInternalServerError())

            case CalcDisplayError =>
              Logger("application").error(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Downstream error")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
    }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url

}
