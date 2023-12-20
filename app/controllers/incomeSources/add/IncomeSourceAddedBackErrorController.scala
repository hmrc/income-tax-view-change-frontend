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

package controllers.incomeSources.add

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{CannotGoBackPage, IncomeSourceType}
import enums.JourneyType.{Add, JourneyType}
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{Authenticator, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.IncomeSourceAddedBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedBackErrorController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                     val cannotGoBackError: IncomeSourceAddedBackError,
                                                     val sessionService: SessionService,
                                                     auth: Authenticator)
                                                    (implicit val appConfig: FrontendAppConfig,
                                                     mcc: MessagesControllerComponents,
                                                     val ec: ExecutionContext,
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils with JourneyChecker{


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withSessionData(JourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { data =>
    val cannotGoBackRedirectUrl = if (isAgent) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType)
    else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType)
    if (data.addIncomeSourceData.exists(addData => addData.journeyIsComplete.contains(true))) {
      Future.successful(Redirect(cannotGoBackRedirectUrl))
    }
    else {
      val postAction = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
      else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.submit(incomeSourceType)
      Future.successful(Ok(cannotGoBackError(isAgent, incomeSourceType, postAction)))
    }
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        incomeSourceType = incomeSourceType
      )
  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleSubmit(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmit(isAgent = true, incomeSourceType)

  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    sessionService.getMongoKeyTyped[String](AddIncomeSourceData.incomeSourceIdField, JourneyType(Add, incomeSourceType)) map {
      case Right(Some(_)) =>
        Redirect(controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType))
      case _ => Logger("application").error(
        s"[IncomeSourceAddedBackErrorController][handleSubmit] - Error: Unable to find id in session")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
        else itvcErrorHandler.showInternalServerError()
    }
  }
}

