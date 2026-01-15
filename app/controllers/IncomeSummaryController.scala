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
import auth.MtdItUser
import auth.authV2.AuthActions
import config._
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.TaxCalcFallBackBackLink
import views.html.IncomeBreakdownView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSummaryController @Inject()(val incomeBreakdown: IncomeBreakdownView,
                                        val authActions: AuthActions,
                                        val calculationService: CalculationService,
                                        val auditingService: AuditingService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                       (implicit val ec: ExecutionContext,
                                        val languageUtils: LanguageUtils,
                                        val appConfig: FrontendAppConfig,
                                        val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with ImplicitDateFormatter with TaxCalcFallBackBackLink {


  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean,
                    previousCalculation: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    calculationService.getCalculationDetailsWithFlag(user.mtditid, user.nino, taxYear, isPrevious = previousCalculation).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = IncomeBreakdownViewModel(liabilityCalc.calculation)
        val fallbackBackUrl = getFallbackUrl(user.session.get(calcPagesBackPage), isAgent,
          liabilityCalc.metadata.isCalculationCrystallised, taxYear, origin)
        viewModel match {
          case Some(model) => Ok(incomeBreakdown(model, taxYear, backUrl = fallbackBackUrl, isAgent = isAgent,
            btaNavPartial = user.btaNavPartial))
          case _ =>
            Logger("application").warn(s"[$taxYear] No income data could be retrieved. Not found")
            itvcErrorHandler.showInternalServerError()
        }
      case error: LiabilityCalculationError if error.status == NO_CONTENT =>
        Logger("application").info(s"[$taxYear] No income data found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[$taxYear] No new calc income data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def showIncomeSummary(taxYear: Int, origin: Option[String] = None, previousCalculation: Boolean = false): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        origin = origin,
        itcvErrorHandler = itvcErrorHandler,
        taxYear = taxYear,
        isAgent = false,
        previousCalculation = previousCalculation
      )
  }

  def showIncomeSummaryAgent(taxYear: Int, previousCalculation: Boolean = false): Action[AnyContent] = authActions.asMTDPrimaryAgent().async {
    implicit mtdItUser =>
      handleRequest(
        itcvErrorHandler = itvcErrorHandlerAgent,
        taxYear = taxYear,
        isAgent = true,
        previousCalculation = previousCalculation
      )
  }
}
