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
import auth.MtdItUserWithNino
import config.featureswitch.FeatureSwitching
import config._
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.liabilitycalculation._
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.DeductionBreakdown

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeductionsSummaryController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val calculationService: CalculationService,
                                            val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                            val auditingService: AuditingService,
                                            val deductionBreakdownView: DeductionBreakdown,
                                            val retrieveBtaNavBar: BtaNavFromNinoPredicate,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val languageUtils: LanguageUtils)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def handleRequest(backUrl: String,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean)
                   (implicit user: MtdItUserWithNino[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = AllowancesAndDeductionsViewModel(liabilityCalc.calculation)
        auditingService.extendedAudit(AllowanceAndDeductionsResponseAuditModel(user, viewModel))
        Ok(deductionBreakdownView(viewModel, taxYear, backUrl, btaNavPartial = user.btaNavPartial, isAgent = isAgent)(implicitly, messages))
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        Logger("application").info(s"${if (isAgent) "[Agent]"}[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No deductions data found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"${if (isAgent) "[Agent]"}[DeductionsSummaryController][showDeductionsSummary[$taxYear]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def showDeductionsSummary(taxYear: Int, origin: Option[String]): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear, origin).url,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false
        )
    }

  def showDeductionsSummaryAgent(taxYear: Int): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit agent =>
        handleRequest(
          backUrl = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url,
          itcvErrorHandler = itvcErrorHandlerAgent,
          taxYear = taxYear,
          isAgent = true
        )(getMtdItUserWithNino()(agent, request, implicitly), implicitly, implicitly, implicitly)
    }
  }

}