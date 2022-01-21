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
import audit.models.{AllowanceAndDeductionsResponseAuditModel, AllowanceAndDeductionsResponseAuditModelNew}
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy, TxmEventsApproved}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import models.calculation.{CalcDisplayError, CalcDisplayModel, CalcDisplayNoDataFound}
import models.liabilitycalculation.view.AllowancesAndDeductionsViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CalculationService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.DeductionBreakdown
import views.html.DeductionBreakdownNew

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeductionsSummaryController @Inject()(deductionBreakdown: DeductionBreakdown,
																						deductionBreakdownViewNew: DeductionBreakdownNew,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            auditingService: AuditingService,
                                            calculationService: CalculationService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {


  def showDeductionsSummary(taxYear: Int): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
				if (isEnabled(NewTaxCalcProxy)) {
					calculationService.getLiabilityCalculationDetail(getClientNino, taxYear).map {
						case liabilityCalc: LiabilityCalculationResponse =>
							val viewModel = AllowancesAndDeductionsViewModel(liabilityCalc.calculation)
							auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModelNew(getMtdItUserWithNino(), viewModel))
							Ok(deductionBreakdownViewNew(viewModel, taxYear, backUrl(taxYear), isAgent = true))
						case _: LiabilityCalculationError =>
							Logger("application").error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No new calc deductions data error found. Downstream error")
							itvcErrorHandler.showInternalServerError()
					}
				} else {
					calculationService.getCalculationDetail(getClientNino, taxYear).map {
						case calcDisplayModel: CalcDisplayModel =>
							auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(getMtdItUserWithNino(),
								calcDisplayModel.calcDataModel.allowancesAndDeductions, isEnabled(TxmEventsApproved)))
							Ok(deductionBreakdown(calcDisplayModel, taxYear, backUrl(taxYear), isAgent = true))

						case CalcDisplayNoDataFound =>
							Logger("application").warn(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Not found")
							itvcErrorHandler.showInternalServerError()

						case CalcDisplayError =>
							Logger("application").error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Downstream error")
							itvcErrorHandler.showInternalServerError()
					}
				}
    }

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
