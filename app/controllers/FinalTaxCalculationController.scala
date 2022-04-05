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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys.{clientFirstName, clientLastName}
import controllers.predicates._
import forms.utils.SessionKeys
import forms.utils.SessionKeys.summaryData
import javax.inject.Inject
import models.finalTaxCalculation.TaxReturnRequestModel
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.FinalTaxCalculationView

import scala.concurrent.{ExecutionContext, Future}

class FinalTaxCalculationController @Inject()(implicit val cc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              view: FinalTaxCalculationView,
                                              checkSessionTimeout: SessionTimeoutPredicate,
                                              authenticate: AuthenticationPredicate,
                                              retrieveNino: NinoPredicate,
                                              retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                              calcService: CalculationService,
                                              itvcErrorHandler: ItvcErrorHandler,
                                              val authorisedFunctions: FrontendAuthorisedFunctions,
                                              implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                              val incomeSourceDetailsService: IncomeSourceDetailsService,
                                              val retrieveBtaNavBar: BtaNavBarPredicate,
                                              implicit val appConfig: FrontendAppConfig
                                             ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen
    retrieveIncomeSources andThen retrieveBtaNavBar


  def handleShowRequest(taxYear: Int,
                        itvcErrorHandler: ShowInternalServerError,
                        isAgent: Boolean,
                        origin: Option[String] = None)
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calculationResponse: LiabilityCalculationResponse =>
        lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
        Ok(view(TaxYearSummaryViewModel(calculationResponse), taxYear, isAgent = isAgent, backUrl))
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
        Logger("application").info("[FinalTaxCalculationController][show] No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("[FinalTaxCalculationController][show] Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }


  def show(taxYear: Int, origin: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleShowRequest(
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        taxYear = taxYear,
        origin = origin
      )
  }

  def showAgent(taxYear: Int): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit agent =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleShowRequest(
              itvcErrorHandler = itvcErrorHandlerAgent,
              isAgent = true,
              taxYear = taxYear
            )
        }
  }

  def submit(taxYear: Int, origin: Option[String]): Action[AnyContent] = action.async { implicit user =>
    val fullNameOptional = user.userName.map { nameModel =>
      (nameModel.name.getOrElse("") + " " + nameModel.lastName.getOrElse("")).trim
    }

    finalDeclarationSubmit(taxYear, fullNameOptional)

  }

  def agentSubmit(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap { user =>
        val fullName = user.session.get(clientFirstName).getOrElse("") + " " + user.session.get(clientLastName).getOrElse("")
        agentFinalDeclarationSubmit(taxYear, fullName)(user, hc)
      }
  }


  private def agentFinalDeclarationSubmit(taxYear: Int, fullName: String)
                                         (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(calcResponse)
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
            Logger("application").error("[Agent][FinalTaxCalculationController][submit] Name or UTR missing.")
            itvcErrorHandler.showInternalServerError()
        }
      case calcError: LiabilityCalculationError if calcError.status == NOT_FOUND =>
        Logger("application").info("[Agent][FinalTaxCalculationController][submit] No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("[Agent][FinalTaxCalculationController][submit] Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def finalDeclarationSubmit(taxYear: Int, fullNameOptional: Option[String])
                                    (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: TaxYearSummaryViewModel = TaxYearSummaryViewModel(calcResponse)
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
            Logger("application").error("[FinalTaxCalculationController][submit] Name or UTR missing.")
            itvcErrorHandler.showInternalServerError()
        }
      case calcError: LiabilityCalculationError if calcError.status == NOT_FOUND =>
        Logger("application").info("[FinalTaxCalculationController][submit] No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("[FinalTaxCalculationController][submit] Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }
}
