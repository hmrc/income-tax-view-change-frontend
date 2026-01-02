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
import config._
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation._
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import utils.TaxCalcFallBackBackLink
import views.html.DeductionBreakdownView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeductionsSummaryController @Inject()(val authActions: AuthActions,
                                            val calculationService: CalculationService,
                                            val auditingService: AuditingService,
                                            val deductionBreakdownView: DeductionBreakdownView,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val languageUtils: LanguageUtils)
  extends FrontendController(mcc) with ImplicitDateFormatter with I18nSupport with TaxCalcFallBackBackLink {

  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean,
                    previousCalculation: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    calculationService.getCalculationDetailsWithFlag(user.mtditid, user.nino, taxYear, previousCalculation).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = AllowancesAndDeductionsViewModel(liabilityCalc.calculation)
        auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(user, viewModel))
        val fallbackBackUrl = getFallbackUrl(user.session.get(calcPagesBackPage), isAgent,
          liabilityCalc.metadata.isCalculationCrystallised, taxYear, origin)
        Ok(deductionBreakdownView(viewModel, taxYear, backUrl = fallbackBackUrl, btaNavPartial = user.btaNavPartial, isAgent = isAgent)(implicitly, messages))
      case error: LiabilityCalculationError if error.status == NO_CONTENT =>
        Logger("application").info(s"${if (isAgent) "[Agent]"}[$taxYear] No deductions data found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"${if (isAgent) "[Agent]"}[$taxYear] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def showDeductionsSummary(taxYear: Int, origin: Option[String] = None, previousCalculation: Boolean = false): Action[AnyContent] =
    authActions.asMTDIndividual.async {
      implicit user =>
        handleRequest(
          origin = origin,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false,
          previousCalculation = previousCalculation
        )
    }

  def showDeductionsSummaryAgent(taxYear: Int, previousCalculation: Boolean = false): Action[AnyContent] = {
    authActions.asMTDPrimaryAgent.async { implicit response =>
      handleRequest(
        itcvErrorHandler = itvcErrorHandlerAgent,
        taxYear = taxYear,
        isAgent = true,
        previousCalculation = previousCalculation
      )
    }
  }

}