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
import audit.models._
import auth.MtdItUserWithNino
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.calculation._
import models.liabilitycalculation
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.DeductionBreakdown
import views.html.DeductionBreakdownNew

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeductionsSummaryController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val retrieveNino: NinoPredicate,
                                            val calculationService: CalculationService,
                                            val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                            val auditingService: AuditingService,
                                            val deductionBreakdownView: DeductionBreakdown,
                                            val deductionBreakdownViewNew: DeductionBreakdownNew,
                                            val itvcErrorHandler: ItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val executionContext: ExecutionContext,
                                            val languageUtils: LanguageUtils)
  extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUserWithNino, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino


  def showDeductionsSummary(taxYear: Int): Action[AnyContent] = {

    action.async {
      implicit user =>
        if (isEnabled(NewTaxCalcProxy)) {
          calculationService.getLiabilityCalculationDetail(user.nino, taxYear).map {
            case liabilityCalc: LiabilityCalculationResponse =>
              liabilityCalc.calculation.flatMap(c => c.allowancesAndDeductions) match {
                case None =>
                  Logger("application").error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No new deductions data could be retrieved. Downstream error")
                  itvcErrorHandler.showInternalServerError()
                case Some(_) =>
                  val viewModel = liabilityCalc.calculation.map(c =>
                    c.getAllowancesAndDeductionsViewModel()).getOrElse(throw new Exception("deductions view model not found"))
                  auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModelNew(user, viewModel))
                  Ok(deductionBreakdownViewNew(viewModel, taxYear, backUrl(taxYear)))
              }

            case _: LiabilityCalculationError =>
              Logger("application").error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Downstream error")
              itvcErrorHandler.showInternalServerError()
          }
        } else {
          calculationService.getCalculationDetail(user.nino, taxYear).map {
            case calcDisplayModel: CalcDisplayModel =>
              auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(user,
                calcDisplayModel.calcDataModel.allowancesAndDeductions, isEnabled(TxmEventsApproved)))
              Ok(deductionBreakdownView(calcDisplayModel, taxYear, backUrl(taxYear)))

            case CalcDisplayNoDataFound =>
              Logger("application").warn(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Not found")
              itvcErrorHandler.showInternalServerError()

            case CalcDisplayError =>
              Logger("application").error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Downstream error")
              itvcErrorHandler.showInternalServerError()
          }
        }
    }
  }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url
}
