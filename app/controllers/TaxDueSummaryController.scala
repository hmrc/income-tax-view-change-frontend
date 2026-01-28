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
import audit.models._
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import connectors.ObligationsConnector
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels._
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import models.obligations.{ObligationsErrorModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.TaxCalcFallBackBackLink
import views.html.TaxCalcBreakdownView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(val authActions: AuthActions,
                                        val calculationService: CalculationService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        val obligationsConnector: ObligationsConnector,
                                        val taxCalcBreakdown: TaxCalcBreakdownView,
                                        val auditingService: AuditingService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext
                                       ) extends FrontendController(mcc)
  with ImplicitDateFormatter with FeatureSwitching with I18nSupport with TaxCalcFallBackBackLink {

  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean,
                    previousCalculation: Boolean
                   )
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    for {
      liabilityCalc <- calculationService.getCalculationDetailsWithFlag(user.mtditid, user.nino, taxYear, isPrevious = previousCalculation)
      taxYearModel   = TaxYear.makeTaxYearWithEndYear(taxYear)
      obligations   <- obligationsConnector.getAllObligationsDateRange(taxYearModel.toFinancialYearStart, taxYearModel.toFinancialYearEnd)
    } yield (liabilityCalc, obligations) match {
      case (calcErrorResponse: LiabilityCalculationError, _) if calcErrorResponse.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream. Not Found.")
        itvcErrorHandler.showInternalServerError()
      case (_: LiabilityCalculationError, _) =>
        Logger("application").error(s"[$taxYear] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
      case (_, _: ObligationsErrorModel) =>
        Logger("application").error(s"[$taxYear] Failed to retrieve obligations. Downstream error")
        itvcErrorHandler.showInternalServerError()
      case (liabilityCalc: LiabilityCalculationResponse, obligations: ObligationsModel) =>

        val viewModel = TaxDueSummaryViewModel(liabilityCalc, obligations)

        auditingService.extendedAudit(TaxDueResponseAuditModel(user, viewModel, taxYear))

        val fallbackBackUrl = getFallbackUrl(user.session.get(calcPagesBackPage), isAgent, liabilityCalc.metadata.isCalculationCrystallised, taxYear, origin)

        val startAVRTYear = 2024

        Ok(taxCalcBreakdown(
          viewModel,
          taxYear,
          startAVRTYear,
          backUrl = fallbackBackUrl,
          isAgent = isAgent,
          btaNavPartial = user.btaNavPartial
        ))
    }
  }


  def showTaxDueSummary(taxYear: Int, origin: Option[String] = None, previousCalculation: Boolean = false): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        origin = origin,
        itcvErrorHandler = itvcErrorHandler,
        taxYear = taxYear,
        isAgent = false,
        previousCalculation = previousCalculation
      )
  }

  def showTaxDueSummaryAgent(taxYear: Int, previousCalculation: Boolean = false): Action[AnyContent] = authActions.asMTDPrimaryAgent().async {
    implicit mtdItUser =>
      handleRequest(
        itcvErrorHandler = itvcErrorHandlerAgent,
        taxYear = taxYear,
        isAgent = true,
        previousCalculation = previousCalculation
      )
  }
}
