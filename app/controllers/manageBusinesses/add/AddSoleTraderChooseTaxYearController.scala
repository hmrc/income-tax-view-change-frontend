/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.JourneyType.IncomeSourceReportingFrequencyJourney
import forms.manageBusinesses.add.AddSoleTraderChooseTaxYearForm
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.IncomeSourceReportingFrequencySourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.manageBusinesses.add.AddSoleTraderChooseTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddSoleTraderChooseTaxYearController @Inject()(authActions: AuthActions,
                                                     addSoleTraderChooseTaxYear: AddSoleTraderChooseTaxYear,
                                                     dateService: DateService,
                                                     sessionService: SessionService, // To be included once they are linked up
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                    (implicit val mcc: MessagesControllerComponents,
                                                     val ec: ExecutionContext,
                                                     val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private lazy val homePageCall: Call = controllers.routes.HomeController.show()
  private lazy val homePageCallAgent: Call = controllers.routes.HomeController.showAgent

  private def errorHandler(isAgent: Boolean) = if (isAgent) {
    itvcErrorHandlerAgent
  } else {
    itvcErrorHandler
  }

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user => {
      handleRequest(isAgent)
    }
  }

  private def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(IncomeSourcesNewJourney), isAgent) match {
      case (false, false) => Future.successful(Redirect(homePageCall))
      case (false, true) => Future.successful(Redirect(homePageCallAgent))
      case _ =>
        Future.successful(Ok(addSoleTraderChooseTaxYear(
          AddSoleTraderChooseTaxYearForm(),
          isAgent,
          routes.AddSoleTraderChooseTaxYearController.submit(isAgent),
          dateService.getCurrentTaxYear,
          dateService.getCurrentTaxYear.nextYear))
        )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }
  

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user => {
      AddSoleTraderChooseTaxYearForm().bindFromRequest.fold(
        formWithError => {
          Future.successful(BadRequest(addSoleTraderChooseTaxYear(
            formWithError,
            isAgent,
            routes.AddSoleTraderChooseTaxYearController.submit(isAgent),
            dateService.getCurrentTaxYear,
            dateService.getCurrentTaxYear.nextYear))
          )
        },
        _ => {

          Future.successful(Ok(addSoleTraderChooseTaxYear(
            AddSoleTraderChooseTaxYearForm(),
            isAgent,
            routes.AddSoleTraderChooseTaxYearController.submit(isAgent),
            dateService.getCurrentTaxYear,
            dateService.getCurrentTaxYear.nextYear)))
          }
      )
    }.recover {
      case ex =>
        Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }
  }
}
