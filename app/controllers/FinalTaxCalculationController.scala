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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import forms.utils.SessionKeys
import forms.utils.SessionKeys.{calcPagesBackPage, summaryData}
import models.finalTaxCalculation.TaxReturnRequestModel
import models.liabilitycalculation.viewmodels.CalculationSummary
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.FinalTaxCalculationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinalTaxCalculationController @Inject()(authActions: AuthActions,
                                              view: FinalTaxCalculationView,
                                              calcService: CalculationService,
                                              itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                              val incomeSourceDetailsService: IncomeSourceDetailsService
                                             )(implicit val appConfig: FrontendAppConfig,
                                               val mcc: MessagesControllerComponents,
                                               ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleShowRequest(taxYear: Int,
                        itvcErrorHandler: ShowInternalServerError,
                        isAgent: Boolean,
                        origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calculationResponse: LiabilityCalculationResponse =>
        lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
        val calculationSummary: CalculationSummary = CalculationSummary(calculationResponse)
        Ok(view(calculationSummary, taxYear, isAgent = isAgent, backUrl))
          .addingToSession(calcPagesBackPage -> "submission")
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }


  def show(taxYear: Int, origin: Option[String]): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        taxYear = taxYear,
        origin = origin
      )
  }

  def showAgent(taxYear: Int): Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true,
        taxYear = taxYear
      )
  }

  def submit(taxYear: Int): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>
    val fullNameOptional = user.userName.map { nameModel =>
      (nameModel.name.getOrElse("") + " " + nameModel.lastName.getOrElse("")).trim
    }
    finalDeclarationSubmit(taxYear, fullNameOptional)
  }

  def agentSubmit(taxYear: Int): Action[AnyContent] = authActions.asMTDPrimaryAgent.async { implicit user =>
    val fullName = user.optClientNameAsString.getOrElse("").trim
    agentFinalDeclarationSubmit(taxYear, fullName)(user, hc)
  }

  def agentFinalDeclarationSubmit(taxYear: Int, fullName: String)
                                 (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: CalculationSummary = CalculationSummary(calcResponse)
        user.saUtr match {
          case Some(saUtr) =>
            val submissionOverview = TaxReturnRequestModel(
              fullName,
              calcOverview.taxDue,
              saUtr,
              calcOverview.income,
              calcOverview.deductions,
              calcOverview.totalTaxableIncome
            )

            Redirect(appConfig.submissionFrontendFinalDeclarationUrl(taxYear)).addingToSession(
              summaryData -> submissionOverview.asJsonString
            )
          case _ =>
            Logger("application").error("[Agent]UTR missing.")
            itvcErrorHandlerAgent.showInternalServerError()
        }
      case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT =>
        Logger("application").info("[Agent]No calculation data returned from downstream.")
        itvcErrorHandlerAgent.showInternalServerError()
      case _ =>
        Logger("application").error("[Agent]Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandlerAgent.showInternalServerError()
    }
  }

  def finalDeclarationSubmit(taxYear: Int, fullNameOptional: Option[String])
                            (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: CalculationSummary = CalculationSummary(calcResponse)
        (fullNameOptional, user.saUtr) match {
          case (Some(fullName), Some(saUtr)) =>
            val submissionOverview = TaxReturnRequestModel(
              fullName,
              calcOverview.taxDue,
              saUtr,
              calcOverview.income,
              calcOverview.deductions,
              calcOverview.totalTaxableIncome
            )

            Redirect(appConfig.submissionFrontendFinalDeclarationUrl(taxYear)).addingToSession(
              SessionKeys.summaryData -> submissionOverview.asJsonString
            )
          case _ =>
            Logger("application").error("Name or UTR missing.")
            itvcErrorHandler.showInternalServerError()
        }
      case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT =>
        Logger("application").info("No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }
}
