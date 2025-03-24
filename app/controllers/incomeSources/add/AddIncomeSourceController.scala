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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.JourneyType.Add
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.AddIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class AddIncomeSourceController @Inject()(val authActions: AuthActions,
                                          val addIncomeSources: AddIncomeSources,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val sessionService: SessionService,
                                          val mcc: MessagesControllerComponents) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  lazy val homePageCall: Call = controllers.routes.HomeController.show()
  lazy val homePageCallAgent: Call = controllers.routes.HomeController.showAgent()

  def show(): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      withIncomeSourcesFS {
        handleRequest(
          isAgent = false,
          homePageCall = homePageCall,
          sources = user.incomeSources,
          backUrl = controllers.routes.HomeController.show().url
        )(implicitly, itvcErrorHandler)
      }
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      withIncomeSourcesFS {
        handleRequest(
          isAgent = true,
          homePageCall = homePageCallAgent,
          sources = mtdItUser.incomeSources,
          backUrl = controllers.routes.HomeController.showAgent.url
        )(implicitly, itvcErrorHandlerAgent)
      }
  }

  def handleRequest(sources: IncomeSourceDetailsModel,
                    homePageCall: Call,
                    isAgent: Boolean,
                    backUrl: String)
                   (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
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
            Logger("application").error(s"Session Error: ${ex.getMessage} - ${ex.getCause}")
            errorHandler.showInternalServerError()
        }
      case Failure(ex) =>
        Logger("application").error(s"Error: ${ex.getMessage} - ${ex.getCause}")
        Future(errorHandler.showInternalServerError())
    }
  }
}
