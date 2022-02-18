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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys
import models.finalTaxCalculation.TaxReturnRequestModel
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.FinalTaxCalculationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinalTaxCalculationController @Inject()(
                                               implicit val cc: MessagesControllerComponents,
                                               val executionContext: ExecutionContext,
                                               view: FinalTaxCalculationView,
                                               checkSessionTimeout: SessionTimeoutPredicate,
                                               authenticate: AuthenticationPredicate,
                                               retrieveNino: NinoPredicate,
                                               retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               calcService: CalculationService,
                                               itvcErrorHandler: ItvcErrorHandler,
                                               val retrieveBtaNavBar: BtaNavBarPredicate,
                                               val appConfig: FrontendAppConfig
                                             ) extends FrontendController(cc) with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen
    retrieveIncomeSources andThen retrieveBtaNavBar


  def show(taxYear: Int): Action[AnyContent] = action.async { implicit user =>
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calculationResponse: LiabilityCalculationResponse =>
        lazy val backUrl: String = appConfig.submissionFrontendTaxOverviewUrl(taxYear)
        Ok(view(TaxYearOverviewViewModel(calculationResponse), taxYear, isAgent = false, backUrl))
      case calcErrorResponse: LiabilityCalculationError if calcErrorResponse.status == NOT_FOUND =>
        Logger("application").info("[FinalTaxCalculationController][show] No calculation data returned from downstream.")
        itvcErrorHandler.showInternalServerError()
      case _ =>
        Logger("application").error("[FinalTaxCalculationController][show] Unexpected error has occurred while retrieving calculation data.")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = action.async { implicit user =>
    val fullNameOptional = user.userName.map { nameModel =>
      (nameModel.name.getOrElse("") + " " + nameModel.lastName.getOrElse("")).trim
    }

    finalDeclarationSubmit(taxYear, fullNameOptional)

  }

  private def finalDeclarationSubmit(taxYear: Int, fullNameOptional: Option[String])
                                    (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier): Future[Result] = {
    calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map {
      case calcResponse: LiabilityCalculationResponse =>
        val calcOverview: TaxYearOverviewViewModel = TaxYearOverviewViewModel(calcResponse)
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
