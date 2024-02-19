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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.JourneyType.{Add, JourneyType}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, LoggerUtil}
import views.html.incomeSources.add.AddIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class AddIncomeSourceController @Inject()(val addIncomeSources: AddIncomeSources,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val ec: ExecutionContext,
                                          implicit val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit val sessionService: SessionService,
                                          implicit override val mcc: MessagesControllerComponents) extends ClientConfirmedController with LoggerUtil
  with FeatureSwitching with IncomeSourcesUtils {

  lazy val homePageCall: Call = controllers.routes.HomeController.show()
  lazy val homePageCallAgent: Call = controllers.routes.HomeController.showAgent

  def show(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        isAgent = false,
        homePageCall = homePageCall,
        sources = user.incomeSources,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        homePageCall = homePageCallAgent,
        sources = mtdItUser.incomeSources,
        backUrl = controllers.routes.HomeController.showAgent.url
      )

  }

  def handleRequest(sources: IncomeSourceDetailsModel,
                    homePageCall: Call,
                    isAgent: Boolean,
                    backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageCall))
    } else {
      incomeSourceDetailsService.getAddIncomeSourceViewModel(sources) match {
        case Success(viewModel) =>
          sessionService.deleteSession(Add).map { _ =>
            Ok(addIncomeSources(
              sources = viewModel,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          } recover {
            case ex: Exception =>
              logWithError(s"Session Error: ${ex.getMessage} - ${ex.getCause}")
          }
        case Failure(ex) =>
          Future.successful {
            logWithError(s"Error: ${ex.getMessage} - ${ex.getCause}")
          }
      }
    }
  }
}
