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
import auth.{FrontendAuthorisedFunctions, MtdItUser, MtdItUserBase, MtdItUserWithNino}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ForecastIncomeSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForecastIncomeSummaryController @Inject()(val forecastIncomeSummaryView: ForecastIncomeSummary,
                                                val checkSessionTimeout: SessionTimeoutPredicate,
                                                val authenticate: AuthenticationPredicate,
                                                val retrieveNino: NinoPredicate,
                                                val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                val calculationService: CalculationService,
                                                val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                                val auditingService: AuditingService,
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

  def handleRequest(mtditid: String, nino: String, taxYear: Int, btaNavPartial: Option[Html], isAgent: Boolean)
                   (implicit user: MtdItUserBase[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result]= {
    val errorPrefix: String = s"[ForecastIncomeSummaryController]${if(isAgent) "[agent]" else ""}[showIncomeSummary[$taxYear]]"

    calculationService.getLiabilityCalculationDetail(mtditid, nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = liabilityCalc.calculation.flatMap(calc => calc.endOfYearEstimate)
        viewModel match {
          case Some(model) => Ok(forecastIncomeSummaryView(model, taxYear, backUrl(taxYear), isAgent,
            btaNavPartial = btaNavPartial))
          case _ =>
            Logger("application").warn(s"$errorPrefix No income data could be retrieved. Not found")
            if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
        }
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        Logger("application").error(s"$errorPrefix No income data found.")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"$errorPrefix No new calc income data error found. Downstream error")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError() else itvcErrorHandler.showInternalServerError()
    }
  }

  def show(taxYear: Int): Action[AnyContent] =
    action.async {
      implicit user =>
        handleRequest(user.mtditid, user.nino, taxYear, user.btaNavPartial, isAgent = false)
    }

  def showAgent(taxYear: Int): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
            handleRequest(getClientMtditid, getClientNino, taxYear, mtdItUser.btaNavPartial, isAgent = true)
          }
    }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url

}
