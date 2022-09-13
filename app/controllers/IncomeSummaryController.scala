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
import auth.MtdItUser
import config._
import config.featureswitch.{FeatureSwitching, NewStateBenefitIncome}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.TaxCalcFallBackBackLink
import views.html.IncomeBreakdown

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSummaryController @Inject()(val incomeBreakdown: IncomeBreakdown,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                        val authenticate: AuthenticationPredicate,
                                        val retrieveNino: NinoPredicate,
                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val calculationService: CalculationService,
                                        val auditingService: AuditingService,
                                        val retrieveBtaNavBar: NavBarPredicate,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                       (implicit val ec: ExecutionContext,
                                        val languageUtils: LanguageUtils,
                                        val appConfig: FrontendAppConfig,
                                        implicit override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport with TaxCalcFallBackBackLink {



  def handleRequest(origin: Option[String] = None,
                    itcvErrorHandler: ShowInternalServerError,
                    taxYear: Int,
                    isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case liabilityCalc: LiabilityCalculationResponse =>
        val viewModel = IncomeBreakdownViewModel(liabilityCalc.calculation, isEnabled(NewStateBenefitIncome))
        val fallbackBackUrl = getFallbackUrl(user.session.get(calcPagesBackPage), isAgent,
          liabilityCalc.metadata.crystallised.getOrElse(false), taxYear, origin)
        viewModel match {
          case Some(model) => Ok(incomeBreakdown(model, taxYear, backUrl = fallbackBackUrl, isAgent = isAgent,
            btaNavPartial = user.btaNavPartial)(implicitly, messages))
          case _ =>
            Logger("application").warn(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
            itvcErrorHandler.showInternalServerError()
        }
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        Logger("application").info(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data found.")
        itvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No new calc income data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def showIncomeSummary(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          origin = origin,
          itcvErrorHandler = itvcErrorHandler,
          taxYear = taxYear,
          isAgent = false
        )
    }
  }

  def showIncomeSummaryAgent(taxYear: Int): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            itcvErrorHandler = itvcErrorHandlerAgent,
            taxYear = taxYear,
            isAgent = true
          )
        }
    }
  }
}
