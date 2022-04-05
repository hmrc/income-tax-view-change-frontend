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

import auth.{FrontendAuthorisedFunctions, MtdItUserWithNino}
import config.featureswitch.{FeatureSwitching, ForecastCalculation}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ForecastTaxCalcSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForecastTaxCalcSummaryController @Inject()(val forecastTaxCalcSummaryView: ForecastTaxCalcSummary,
                                                 val checkSessionTimeout: SessionTimeoutPredicate,
                                                 val authenticate: AuthenticationPredicate,
                                                 val retrieveNino: NinoPredicate,
                                                 val calculationService: CalculationService,
                                                 val retrieveBtaNavBar: BtaNavFromNinoPredicate,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                 val authorisedFunctions: FrontendAuthorisedFunctions)
                                                (implicit val ec: ExecutionContext,
                                                 val languageUtils: LanguageUtils,
                                                 val appConfig: FrontendAppConfig,
                                                 mcc: MessagesControllerComponents,
                                                 implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUserWithNino, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveBtaNavBar

  def onError(message: String, isAgent: Boolean, taxYear: Int)(implicit request: Request[_]): Result = {
    val errorPrefix: String = s"[ForecastTaxCalcSummaryController]${if (isAgent) "[Agent]" else ""}[showForecastTaxCalcSummary[$taxYear]]"
    Logger("application").error(s"$errorPrefix $message")
    if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
  }

  def handleRequest(mtditid: String, nino: String, taxYear: Int, btaNavPartial: Option[Html], isAgent: Boolean, origin: Option[String] = None)
                   (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    if (isDisabled(ForecastCalculation)) {
      val errorTemplate = if (isAgent) itvcErrorHandlerAgent.notFoundTemplate else itvcErrorHandler.notFoundTemplate
      Future.successful(NotFound(errorTemplate))
    } else {
      calculationService.getLiabilityCalculationDetail(mtditid, nino, taxYear).map {
        case liabilityCalc: LiabilityCalculationResponse =>
          val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
          viewModel match {
            case Some(model) => Ok(forecastTaxCalcSummaryView(model, taxYear, backUrl(isAgent, taxYear, origin), isAgent, btaNavPartial))
            case _ => onError(s"No tax calculation data could be retrieved. Not found", isAgent, taxYear)
          }
        case error: LiabilityCalculationError if error.status == NOT_FOUND =>
          onError(s"No tax calculation data found.", isAgent, taxYear)
        case _: LiabilityCalculationError =>
          onError(s"No new tax calculation data found. Downstream error", isAgent, taxYear)
      }
    }
  }

  def show(taxYear: Int, origin: Option[String] = None): Action[AnyContent] =
    action.async {
      implicit user =>
        handleRequest(user.mtditid, user.nino, taxYear, user.btaNavPartial, isAgent = false, origin)
    }

  def showAgent(taxYear: Int): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          handleRequest(getClientMtditid, getClientNino, taxYear, None, isAgent = true)
    }

  def backUrl(isAgent: Boolean, taxYear: Int, origin: Option[String]): String =
    if (isAgent) controllers.agent.routes.TaxYearSummaryController.show(taxYear).url
    else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url
}
