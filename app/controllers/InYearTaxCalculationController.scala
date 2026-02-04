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
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import models.liabilitycalculation.viewmodels.CalculationSummary
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CalculationService, DateServiceInterface}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.InYearTaxCalculationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InYearTaxCalculationController @Inject()(authActions: AuthActions,
                                               view: InYearTaxCalculationView,
                                               calcService: CalculationService,
                                               dateService: DateServiceInterface,
                                               auditingService: AuditingService,
                                               itvcErrorHandler: ItvcErrorHandler,
                                               itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                              (implicit val mcc: MessagesControllerComponents,
                                               val appConfig: FrontendAppConfig,
                                               val languageUtils: LanguageUtils,
                                               val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with ImplicitDateFormatter {


  def handleRequest(isAgent: Boolean, currentDate: LocalDate, timeStamp: String, origin: Option[String] = None)
                   (implicit user: MtdItUser[_],
                    hc: HeaderCarrier,
                    errorHandler: ShowInternalServerError): Future[Result] = {

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
        Ok(view(taxCalc, taxYear, isAgent, backUrl, timeStamp))
          .addingToSession(calcPagesBackPage -> "submission")
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream.")
        errorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("Unexpected error has occurred while retrieving calculation data.")
        errorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String]): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      val currentDate = dateService.getCurrentDate
      handleRequest(
        isAgent = false,
        currentDate,
        currentDate.toLongDate,
        origin = origin
      )(implicitly, implicitly, itvcErrorHandler)
  }

  def showAgent: Action[AnyContent] = authActions.asMTDPrimaryAgent().async {
    implicit mtdItUser =>
      val currentDate = dateService.getCurrentDate
      handleRequest(
        isAgent = true,
        currentDate,
        currentDate.toLongDate
      )(implicitly, implicitly, itvcErrorHandlerAgent)
  }

}
