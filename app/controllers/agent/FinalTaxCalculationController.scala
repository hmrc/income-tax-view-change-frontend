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

package controllers.agent

import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import controllers.predicates.agent.AgentAuthenticationPredicate
import controllers.agent.utils.SessionKeys
import forms.utils.SessionKeys.{calculationId, summaryData}
import models.calculation.{CalcDisplayModel, CalcOverview}
import models.finalTaxCalculation.TaxReturnRequestModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Request}
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.FinalTaxCalculationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FinalTaxCalculationController @Inject()(
                                               implicit val cc: MessagesControllerComponents,
                                               val ec: ExecutionContext,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               view: FinalTaxCalculationView,
                                               checkSessionTimeout: SessionTimeoutPredicate,
                                               retrieveNino: NinoPredicate,
                                               retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               calcService: CalculationService,
                                               itvcErrorHandler: ItvcErrorHandler,
                                               appConfig: FrontendAppConfig,
                                               incomeSourceDetailsService: IncomeSourceDetailsService
                                             ) extends ClientConfirmedController with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
    getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { user =>
      calcService.getCalculationDetail(user.nino, taxYear).map {
        case CalcDisplayModel(_, _, calcDataModel, _) =>
          val calcOverview = CalcOverview(calcDataModel, None)
          Ok(view(calcOverview, taxYear, isAgent = true))
        case _ => itvcErrorHandler.showInternalServerError()
      }
    }
  }
  
  def submit(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { user =>
        val fullName = user.session.get(SessionKeys.clientFirstName).getOrElse("") + " " + user.session.get(SessionKeys.clientLastName).getOrElse("")

        calcService.getCalculationDetail(user.nino, taxYear).map {
          case CalcDisplayModel(_, _, calcDataModel, _) =>
            val calcOverview = CalcOverview(calcDataModel, None)
            
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

              case _ => itvcErrorHandler.showInternalServerError()
            }
          case _ => itvcErrorHandler.showInternalServerError()
        }
        
      }
    
  }

}
