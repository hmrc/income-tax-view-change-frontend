/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import models.calculation.{CalcDisplayModel, CalcOverview}
import models.finalTaxCalculation.TaxReturnRequestModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import services.CalculationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.FinalTaxCalculationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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
                                               appConfig: FrontendAppConfig
                                             ) extends FrontendController(cc) with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  def show(taxYear: Int): Action[AnyContent] = action.async { implicit user =>
    calcService.getCalculationDetail(user.nino, taxYear).map {
      case CalcDisplayModel(_, _, calcDataModel, _) =>
        val calcOverview = CalcOverview(calcDataModel, None)
        Ok(view(calcOverview, taxYear))
      case _ => itvcErrorHandler.showInternalServerError()
    }
  }
  
  def submit(taxYear: Int): Action[AnyContent] = action.async { implicit user =>
    val fullNameOptional = user.userName.map { nameModel =>
      (nameModel.name.getOrElse("") + " " + nameModel.lastName.getOrElse("")).trim
    }
    
    calcService.getCalculationDetail(user.nino, taxYear).map {
      case CalcDisplayModel(_, _, calcDataModel, _) =>
        val calcOverview = CalcOverview(calcDataModel, None)

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
            
          case _ => itvcErrorHandler.showInternalServerError()
        }
      case _ => itvcErrorHandler.showInternalServerError()
    }
    
  }

}
