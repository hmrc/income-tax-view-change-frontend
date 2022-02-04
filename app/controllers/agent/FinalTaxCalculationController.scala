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

package controllers.agent

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, NewTaxCalcProxy}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys.{clientFirstName, clientLastName}
import forms.utils.SessionKeys.summaryData
import models.calculation.{CalcDisplayModel, CalcDisplayNoDataFound, CalcOverview}
import models.finalTaxCalculation.TaxReturnRequestModel
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{FinalTaxCalculationView, FinalTaxCalculationViewOld}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinalTaxCalculationController @Inject()(
                                               implicit val cc: MessagesControllerComponents,
                                               val ec: ExecutionContext,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               view: FinalTaxCalculationView,
                                               viewOld: FinalTaxCalculationViewOld,
                                               calcService: CalculationService,
                                               itvcErrorHandler: AgentItvcErrorHandler,
                                               val appConfig: FrontendAppConfig,
                                               incomeSourceDetailsService: IncomeSourceDetailsService
                                             )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap { user =>
        if (isEnabled(NewTaxCalcProxy)) {
          calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
            case calculationResponse: LiabilityCalculationResponse =>
              lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
              Ok(view(TaxYearOverviewViewModel(calculationResponse), taxYear, isAgent = true, backUrl))
            case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
              Logger("application").info("[Agent][FinalTaxCalculationController][show] No calculation data returned from downstream.")
              itvcErrorHandler.showInternalServerError()
            case _ =>
              Logger("application").error("[Agent][FinalTaxCalculationController][show] Unexpected error has occurred while retrieving calculation data.")
              itvcErrorHandler.showInternalServerError()
          }
        } else {
          calcService.getCalculationDetail(user.nino, taxYear).map {
            case CalcDisplayModel(_, _, calcDataModel, _) =>
              val calcOverview = CalcOverview(calcDataModel)
              lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
              Ok(viewOld(calcOverview, taxYear, isAgent = true, backUrl))
            case CalcDisplayNoDataFound =>
              Logger("application").info("[Agent][FinalTaxCalculationController][show] No calculation data returned from downstream.")
              itvcErrorHandler.showInternalServerError()
            case _ =>
              Logger("application").error("[Agent][FinalTaxCalculationController][show] Unexpected error has occurred while retrieving calculation data.")
              itvcErrorHandler.showInternalServerError()
          }
        }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap { user =>
        val fullName = user.session.get(clientFirstName).getOrElse("") + " " + user.session.get(clientLastName).getOrElse("")
        if (isEnabled(NewTaxCalcProxy)) {
          finalDeclarationSubmit(taxYear, fullName)(user, hc)
        } else {
          oldFinalDeclarationSubmit(taxYear, fullName)(user)
        }

      }
  }

  private def oldFinalDeclarationSubmit(taxYear: Int, fullName: String)
                                       (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    calcService.getCalculationDetail(user.nino, taxYear).map {
      case CalcDisplayModel(_, _, calcDataModel, _) =>
        val calcOverview = CalcOverview(calcDataModel)

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
      case CalcDisplayNoDataFound =>
        Logger("application").info("[Agent][FinalTaxCalculationController][submit] No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("[Agent][FinalTaxCalculationController][submit] Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def finalDeclarationSubmit(taxYear: Int, fullName: String)
                                    (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: TaxYearOverviewViewModel = TaxYearOverviewViewModel(calcResponse)
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
}
