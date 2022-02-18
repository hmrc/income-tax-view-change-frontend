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
import audit.models.AllowanceAndDeductionsResponseAuditModel
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CalculationService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.DeductionBreakdown

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeductionsSummaryController @Inject()(deductionBreakdownView: DeductionBreakdown,
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
        calculationService.getLiabilityCalculationDetail(getClientMtditid, getClientNino, taxYear).map {
          case liabilityCalc: LiabilityCalculationResponse =>
            val viewModel = AllowancesAndDeductionsViewModel(liabilityCalc.calculation)
            auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(getMtdItUserWithNino(), viewModel))
            Ok(deductionBreakdownView(viewModel, taxYear, backUrl(taxYear), isAgent = true))
          case error: LiabilityCalculationError if error.status == NOT_FOUND =>
            Logger("application").info(
              s"[Agent][DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data found.")
            itvcErrorHandler.showInternalServerError()
          case _: LiabilityCalculationError =>
            Logger("application").error(
              s"[Agent][DeductionsSummaryController][showDeductionsSummary[$taxYear]] No new calc deductions data error found. Downstream error")
            itvcErrorHandler.showInternalServerError()
        }
    }

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
