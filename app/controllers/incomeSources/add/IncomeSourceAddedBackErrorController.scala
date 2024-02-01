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
import enums.IncomeSourceJourney.{BeforeSubmissionPage, CannotGoBackPage, IncomeSourceType}
import enums.JourneyType.{Add, JourneyType}
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.IncomeSourceAddedBackError

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
                                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils with JourneyChecker{


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleShowRequest(isAgent, incomeSourceType)
  }

  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleSubmitRequest(isAgent, incomeSourceType)
  }

  def handleShowRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withSessionData(JourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { data =>

    val cannotGoBackRedirectUrl = routes.ReportingMethodSetBackErrorController.show(isAgent, incomeSourceType)

    if (data.addIncomeSourceData.exists(addData => addData.journeyIsComplete.contains(true))) {
      Future.successful(Redirect(cannotGoBackRedirectUrl))
    }
    else {
      val postAction = routes.IncomeSourceAddedBackErrorController.submit(isAgent, incomeSourceType)
      Future.successful(Ok(cannotGoBackError(isAgent, incomeSourceType, postAction)))
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] =
    withSessionData(JourneyType(Add, incomeSourceType), CannotGoBackPage) {
      _.addIncomeSourceData.map(_.incomeSourceId) match {
        case Some(_) =>
          Future.successful {
            Redirect(routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType))
          }
        case None => Logger("application").error(
          "[IncomeSourceAddedBackErrorController][handleSubmit] - Error: Unable to find id in session")
          Future.successful {
            (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
          }
      }
  }
}
