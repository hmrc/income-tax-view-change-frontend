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
import audit.models._
import auth.MtdItUser
import config.featureswitch.{Class4UpliftEnabled, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import implicits.ImplicitDateFormatter
import javax.inject.{Inject, Singleton}
import models.liabilitycalculation.viewmodels._
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxCalcBreakdown

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val authenticate: AuthenticationPredicate,
                                        val retrieveNino: NinoPredicate,
                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        val calculationService: CalculationService,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        val taxCalcBreakdown: TaxCalcBreakdown,
                                        val auditingService: AuditingService,
                                        val retrieveBtaNavBar: NavBarPredicate)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        implicit val ec: ExecutionContext
                                       ) extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def handleRequest(backUrl: String,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean

                   )
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = TaxDueSummaryViewModel(liabilityCalc)
        auditingService.extendedAudit(TaxDueResponseAuditModel(user, viewModel, taxYear))
        Ok(taxCalcBreakdown(viewModel, taxYear, backUrl, isAgent = isAgent, btaNavPartial = user.btaNavPartial, isEnabled(Class4UpliftEnabled)))
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
        Logger("application").info("[TaxDueController][showTaxDueSummary] No calculation data returned from downstream. Not Found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          "[TaxDueController][showTaxDueSummary[" + taxYear +
            "]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }


  def showTaxDueSummary(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false
        )
    }
  }

  def showTaxDueSummaryAgent(taxYear: Int): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            backUrl = controllers.agent.routes.TaxYearSummaryController.show(taxYear).url,
            itcvErrorHandler = itvcErrorHandlerAgent,
            taxYear = taxYear,
            isAgent = true
          )
        }
    }
  }
}
