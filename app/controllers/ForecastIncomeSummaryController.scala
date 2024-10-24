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
import audit.models.ForecastIncomeAuditModel
import auth.MtdItUserWithNino
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.admin.FeatureSwitchService
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.ForecastIncomeSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForecastIncomeSummaryController @Inject()(val forecastIncomeSummaryView: ForecastIncomeSummary,
                                                val calculationService: CalculationService,
                                                val auditingService: AuditingService,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                val authorisedFunctions: AuthorisedFunctions,
                                                val featureSwitchService: FeatureSwitchService,
                                                auth: AuthenticatorPredicate)
                                               (implicit val ec: ExecutionContext,
                                                val languageUtils: LanguageUtils,
                                                val appConfig: FrontendAppConfig,
                                                mcc: MessagesControllerComponents,
                                                implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def onError(message: String, isAgent: Boolean, taxYear: Int)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ForecastIncomeSummaryController]${if (isAgent) "[agent]" else ""}[showIncomeSummary[$taxYear]]"
    Logger("application").error(s"$errorPrefix $message")
    if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
  }

  def handleRequest(taxYear: Int, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUserWithNino[_]): Future[Result] = {
    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
        viewModel match {
          case Some(model) =>
            auditingService.extendedAudit(ForecastIncomeAuditModel(user, model))
            Ok(forecastIncomeSummaryView(model, taxYear, backUrl(taxYear, origin, isAgent), isAgent,
              user.btaNavPartial))
          case _ =>
            onError("No income data could be retrieved. Not found", isAgent, taxYear)
        }
      case error: LiabilityCalculationError if error.status == NO_CONTENT =>
        onError("No income data found.", isAgent, taxYear)
      case _: LiabilityCalculationError =>
        onError("No new calc income data error found. Downstream error", isAgent, taxYear)
    }
  }

  def show(taxYear: Int, origin: Option[String] = None): Action[AnyContent] =
    auth.authenticatedActionWithNino {
      implicit user =>
        handleRequest(taxYear, isAgent = false, origin)
    }

  def showAgent(taxYear: Int): Action[AnyContent] = auth.authenticatedActionWithNinoAgent {
    implicit response =>
        handleRequest(taxYear, isAgent = true)(getMtdItUserWithNino()(response.agent, response.request))
  }

  def backUrl(taxYear: Int, origin: Option[String], isAgent: Boolean): String =
    if (isAgent) controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
    else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url

}
