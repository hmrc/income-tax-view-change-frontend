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
import audit.models.{ViewInYearTaxEstimateAuditBody, ViewInYearTaxEstimateAuditModel}
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels.CalculationSummary
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CalculationService, DateServiceInterface}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.InYearTaxCalculationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InYearTaxCalculationController @Inject()(val executionContext: ExecutionContext,
                                                view: InYearTaxCalculationView,
                                                calcService: CalculationService,
                                                dateService: DateServiceInterface,
                                                auditingService: AuditingService,
                                                itvcErrorHandler: ItvcErrorHandler,
                                                implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                val authorisedFunctions: FrontendAuthorisedFunctions,
                                                val languageUtils: LanguageUtils,
                                                implicit val appConfig: FrontendAppConfig,
                                                implicit override val mcc: MessagesControllerComponents,
                                                implicit val ec: ExecutionContext,
                                                val auth: AuthenticatorPredicate
                                              ) extends ClientConfirmedController with FeatureSwitching with I18nSupport with ImplicitDateFormatter {


  def handleRequest(isAgent: Boolean, currentDate: LocalDate, timeStamp: String, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val taxYear = if (currentDate.isAfter(toTaxYearEndDate(currentDate.getYear))) {
      currentDate.getYear + 1
    }
    else currentDate.getYear
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calculationResponse: LiabilityCalculationResponse =>

        val taxCalc: CalculationSummary = CalculationSummary(calculationResponse)

        val auditModel = ViewInYearTaxEstimateAuditModel(
          user.nino,
          user.mtditid,
          if (isAgent) "agent" else "individual",
          taxYear,
          ViewInYearTaxEstimateAuditBody(taxCalc)
        )

        auditingService.audit(auditModel)

        lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
        Ok(view(taxCalc, taxYear, isAgent, backUrl, timeStamp)(messages, user, appConfig))
          .addingToSession(calcPagesBackPage -> "submission")
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String]): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      val currentDate = dateService.getCurrentDate
      handleRequest(
        isAgent = false,
        currentDate,
        currentDate.toLongDate,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      val currentDate = dateService.getCurrentDate
      handleRequest(
        isAgent = true,
        currentDate,
        currentDate.toLongDate
      )
  }

}
