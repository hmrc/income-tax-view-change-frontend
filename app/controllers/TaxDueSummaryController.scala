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
import auth.MtdItUser
import config.featureswitch.{Class4UpliftEnabled, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels._
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxCalcBreakdown

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaxDueSummaryController @Inject()(checkSessionTimeout: SessionTimeoutPredicate,
                                        authenticate: AuthenticationPredicate,
                                        retrieveNino: NinoPredicate,
                                        retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        calculationService: CalculationService,
                                        itvcErrorHandler: ItvcErrorHandler,
                                        retrieveBtaNavPartial: BtaNavBarPredicate,
                                        taxCalcBreakdown: TaxCalcBreakdown,
                                        val auditingService: AuditingService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val executionContext: ExecutionContext
                                       ) extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {


  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen
    retrieveIncomeSources andThen retrieveBtaNavPartial

  def showTaxDueSummary(taxYear: Int): Action[AnyContent] = {
    action.async {
      implicit user => {
        calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
          case liabilityCalc: LiabilityCalculationResponse =>
            val viewModel = TaxDueSummaryViewModel(liabilityCalc)
            auditingService.extendedAudit(TaxDueResponseAuditModel(user, viewModel, taxYear))
            Ok(taxCalcBreakdown(viewModel, taxYear, backUrl(taxYear), false, user.btaNavPartial, isEnabled(Class4UpliftEnabled)))
          case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
            Logger("application").info("[TaxDueController][showTaxDueSummary] No calculation data returned from downstream. Not Found.")
            itvcErrorHandler.showInternalServerError()
          case _: LiabilityCalculationError =>
            Logger("application").error(
              "[TaxDueController][showTaxDueSummary[" + taxYear +
                "]] No new calc deductions data error found. Downstream error")
            itvcErrorHandler.showInternalServerError()
        }
      }
    }
  }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url

}
