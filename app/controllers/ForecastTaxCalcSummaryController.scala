/*
 * Copyright 2023 HM Revenue & Customs
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
import audit.models.ForecastTaxCalculationAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ForecastTaxCalcSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForecastTaxCalcSummaryController @Inject()(val authActions: AuthActions,
                                                 val forecastTaxCalcSummaryView: ForecastTaxCalcSummaryView,
                                                 val auditingService: AuditingService,
                                                 val calculationService: CalculationService,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                (implicit val ec: ExecutionContext,
                                                 val languageUtils: LanguageUtils,
                                                 val appConfig: FrontendAppConfig,
                                                 mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with ImplicitDateFormatter {

  def onError(message: String, isAgent: Boolean, taxYear: Int)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ForecastTaxCalcSummaryController]${if (isAgent) "[Agent]" else ""}[showForecastTaxCalcSummary[$taxYear]]"
    Logger("application").error(s"$errorPrefix $message")
    if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
  }

  def handleRequest(taxYear: Int, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
        viewModel match {
          case Some(model) =>
            if (model.incomeTaxNicAmount.isEmpty && model.incomeTaxNicAndCgtAmount.isEmpty) {
              throw MissingFieldException("incomeTaxNicAmount and incomeTaxNicAndCgtAmount missing from response")
            } else {
              auditingService.extendedAudit(ForecastTaxCalculationAuditModel(user, model))
              Ok(forecastTaxCalcSummaryView(model, taxYear, backUrl(isAgent, taxYear, origin), isAgent, user.btaNavPartial))
            }
          case _ => onError("No tax calculation data could be retrieved. Not found", isAgent, taxYear)
        }
      case error: LiabilityCalculationError if error.status == NO_CONTENT =>
        onError("No tax calculation data found.", isAgent, taxYear)
      case _: LiabilityCalculationError =>
        onError("No new tax calculation data found. Downstream error", isAgent, taxYear)
    }
  }

  def show(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(taxYear, isAgent = false, origin)
  }

  def showAgent(taxYear: Int): Action[AnyContent] = authActions.asMTDPrimaryAgent().async {
    implicit response =>
      handleRequest(taxYear, isAgent = true)
  }

  def backUrl(isAgent: Boolean, taxYear: Int, origin: Option[String]): String =
    if (isAgent) controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
    else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url
}
