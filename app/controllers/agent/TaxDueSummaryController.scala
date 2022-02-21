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

package controllers.agent

import audit.AuditingService
import audit.models._
import config.featureswitch.{Class4UpliftEnabled, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.TaxCalcBreakdown

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaxDueSummaryController @Inject()(taxCalcBreakdown: TaxCalcBreakdown,
                                        val appConfig: FrontendAppConfig,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        calculationService: CalculationService,
                                        incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val auditingService: AuditingService
                                       )(implicit mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def showTaxDueSummary(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
        calculationService.getLiabilityCalculationDetail(mtdItUser.mtditid, mtdItUser.nino, taxYear).map {
          case liabilityCalc: LiabilityCalculationResponse =>
            val viewModel = TaxDueSummaryViewModel(liabilityCalc)
            auditingService.extendedAudit(TaxDueResponseAuditModel(mtdItUser, viewModel, taxYear))
            Ok(taxCalcBreakdown(viewModel, taxYear, backUrl(taxYear), isAgent = true, class4UpliftEnabled = isEnabled(Class4UpliftEnabled)))
          case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
            Logger("application").info("[Agent][TaxDueController][showTaxDueSummary] No calculation data returned from downstream. Not Found.")
            itvcErrorHandler.showInternalServerError()
          case _: LiabilityCalculationError =>
            Logger("application").error(
              "[Agent][TaxDueController][showTaxDueSummary[" + taxYear +
                "]] No new calc deductions data error found. Downstream error")
            itvcErrorHandler.showInternalServerError()
        }
      }
  }

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
