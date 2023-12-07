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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.YouCannotGoBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YouCannotGoBackErrorController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                               val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val cannotGoBackError: YouCannotGoBackError)
                                              (implicit val appConfig: FrontendAppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils {


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val subheadingContent = getSubheadingContent(incomeSourceType)
    Future.successful(Ok(cannotGoBackError(isAgent, subheadingContent))) //TODO: fix this
  }

  def getSubheadingContent(incomeSourceType: IncomeSourceType)(implicit request: Request[_]): String = {
    incomeSourceType match {
      case SelfEmployment => messagesApi.preferred(request)("cannotGoBack.soleTraderAdded")
      case UkProperty => messagesApi.preferred(request)("cannotGoBack.ukPropertyAdded")
      case ForeignProperty => messagesApi.preferred(request)("cannotGoBack.foreignPropertyAdded")
    }
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }
}
