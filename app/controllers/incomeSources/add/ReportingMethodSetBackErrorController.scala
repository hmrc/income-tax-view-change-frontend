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
import enums.IncomeSourceJourney.{CannotGoBackPage, ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.YouCannotGoBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodSetBackErrorController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                      val cannotGoBackError: YouCannotGoBackError,
                                                      val auth: AuthenticatorPredicate)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                      val sessionService: SessionService) extends ClientConfirmedController with IncomeSourcesUtils with JourneyChecker {


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleRequest(isAgent, incomeSourceType)
    }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] =
    withSessionData(JourneyType(Add, incomeSourceType), journeyState = CannotGoBackPage) { _ =>
      Future.successful(
        Ok(
          cannotGoBackError(
            isAgent,
            messagesApi.preferred(user)(incomeSourceType.cannotGoBack)
          )
        )
      )
    }
}
