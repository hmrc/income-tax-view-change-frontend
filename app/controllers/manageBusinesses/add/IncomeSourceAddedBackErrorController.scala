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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.CannotGoBackPage
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceAddedBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedBackErrorController @Inject()(val authActions: AuthActions,
                                                     val cannotGoBackError: IncomeSourceAddedBackError,
                                                     val sessionService: SessionService)
                                                    (implicit val appConfig: FrontendAppConfig,
                                                     mcc: MessagesControllerComponents,
                                                     val ec: ExecutionContext,
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends FrontendController(mcc) with JourneyCheckerManageBusinesses with I18nSupport {


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { data =>
    val cannotGoBackRedirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType)
    else controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType)
    if (data.addIncomeSourceData.exists(addData => addData.incomeSourceCreatedJourneyComplete.contains(true))) {
      Future.successful(Redirect(cannotGoBackRedirectUrl))
    }
    else {
      val postAction = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
      else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submit(incomeSourceType)
      Future.successful(Ok(cannotGoBackError(isAgent, incomeSourceType, postAction)))
    }
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType
      )
  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleSubmit(isAgent = false, incomeSourceType)(implicitly, itvcErrorHandler)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleSubmit(isAgent = true, incomeSourceType)(implicitly, itvcErrorHandlerAgent)

  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                          (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), CannotGoBackPage) {
      _.addIncomeSourceData.map(_.incomeSourceId) match {
        case Some(_) =>
          Future.successful {
            Redirect(routes.IncomeSourceReportingFrequencyController.show(isAgent, false, incomeSourceType))
          }
        case None => Logger("application").error(
          "Error: Unable to find id in session")
          Future.successful {
            errorHandler.showInternalServerError()
          }
      }
    }
}