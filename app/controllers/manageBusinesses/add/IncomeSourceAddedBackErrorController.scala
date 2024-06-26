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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{CannotGoBackPage, IncomeSourceType}
import enums.JourneyType.{Add, JourneyType}
import play.api.Logger
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.IncomeSourceAddedBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedBackErrorController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                     val cannotGoBackError: IncomeSourceAddedBackError,
                                                     val sessionService: SessionService,
                                                     auth: AuthenticatorPredicate)
                                                    (implicit val appConfig: FrontendAppConfig,
                                                     mcc: MessagesControllerComponents,
                                                     val ec: ExecutionContext,
                                                     val itvcErrorHandler: ItvcErrorHandler,
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils with JourneyCheckerManageBusinesses{


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withSessionData(JourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { data =>
    val cannotGoBackRedirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType)
    else controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType)
    if (data.addIncomeSourceData.exists(addData => addData.journeyIsComplete.contains(true))) {
      Future.successful(Redirect(cannotGoBackRedirectUrl))
    }
    else {
      val postAction = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
      else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.submit(incomeSourceType)
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

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] =
    withSessionData(JourneyType(Add, incomeSourceType), CannotGoBackPage) {
      _.addIncomeSourceData.map(_.incomeSourceId) match {
        case Some(_) =>
          Future.successful {
            Redirect(routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType))
          }
        case None => Logger("application").error(
          "Error: Unable to find id in session")
          Future.successful {
            (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
          }
      }
    }
}

