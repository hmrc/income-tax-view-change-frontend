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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels._
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{AuthenticatorPredicate, TaxCalcFallBackBackLink}
import views.html.TaxCalcBreakdown

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                        val calculationService: CalculationService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        val taxCalcBreakdown: TaxCalcBreakdown,
                                        val auditingService: AuditingService,
                                        val auth: AuthenticatorPredicate)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        implicit val ec: ExecutionContext
                                       ) extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport with TaxCalcFallBackBackLink {

  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean
                   )
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = TaxDueSummaryViewModel(liabilityCalc)
        auditingService.extendedAudit(TaxDueResponseAuditModel(user, viewModel, taxYear))
        val fallbackBackUrl = getFallbackUrl(user.session.get(calcPagesBackPage),
          isAgent, liabilityCalc.metadata.crystallised.getOrElse(false), taxYear, origin)
        Ok(taxCalcBreakdown(viewModel, taxYear, backUrl = fallbackBackUrl, isAgent = isAgent, btaNavPartial = user.btaNavPartial))
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream. Not Found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[$taxYear] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }


  def showTaxDueSummary(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleRequest(
          origin = origin,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false
        )
  }

  def showTaxDueSummaryAgent(taxYear: Int): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        itcvErrorHandler = itvcErrorHandlerAgent,
        taxYear = taxYear,
        isAgent = true
      )
  }
}
