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

import audit.AuditingService
import audit.models.{AllowanceAndDeductionsRequestAuditModel, AllowanceAndDeductionsResponseAuditModel}
import config.featureswitch.{AgentViewer, DeductionBreakdown, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalcDisplayError, CalcDisplayModel, CalcDisplayNoDataFound}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.DeductionBreakdown
import views.html.errorPages.notFound

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionsSummaryController @Inject()(deductionBreakdown: DeductionBreakdown,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            incomeSourceDetailsService: IncomeSourceDetailsService,
                                            auditingService: AuditingService,
                                            calculationService: CalculationService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            val languageUtils: LanguageUtils,
                                            mcc: MessagesControllerComponents,
                                            dateFormatter: ImplicitDateFormatterImpl,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {


  def showDeductionsSummary(taxYear: Int): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (isEnabled(AgentViewer)) {
          if (isEnabled(DeductionBreakdown)) {
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              auditingService.extendedAudit(AllowanceAndDeductionsRequestAuditModel(mtdItUser))
              calculationService.getCalculationDetail(getClientNino, taxYear).map {
                case calcDisplayModel: CalcDisplayModel =>
                  auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(mtdItUser,
                    calcDisplayModel.calcDataModel.allowancesAndDeductions))
                  Ok(deductionBreakdown(calcDisplayModel, taxYear, backUrl(taxYear)))

                case CalcDisplayNoDataFound =>
                  Logger.warn(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Not found")
                  itvcErrorHandler.showInternalServerError()

                case CalcDisplayError =>
                  Logger.error(s"[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data could be retrieved. Downstream error")
                  itvcErrorHandler.showInternalServerError()
              }
            }
          }
          else {
            Future.successful(Redirect(controllers.agent.routes.TaxYearOverviewController.show(taxYear).url))
          }
        }
        else Future.successful(NotFound(notFound()))
    }

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
