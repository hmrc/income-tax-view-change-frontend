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

package businessDetails.controllers.manageBusinesses.add

import businessDetails.services.IncomeSourceDetailsService
import businessDetails.utils.IncomeSourcesUtils
import enums.JourneyType.Add
import models.admin.DisplayBusinessStartDate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import businessDetails.views.html.manageBusinesses.add.AddIncomeSourcesView
import common.auth.{AuthActions, MtdItUser}
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import common.services.SessionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class AddIncomeSourceController @Inject()(val authActions: AuthActions,
                                          val addIncomeSources: AddIncomeSourcesView,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val sessionService: SessionService,
                                          val mcc: MessagesControllerComponents) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  private lazy val homePageCall: Call = hub.controllers.routes.HomeController.show()
  private lazy val homePageCallAgent: Call = hub.controllers.routes.HomeController.showAgent()

  def show(): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        isAgent = false,
        homePageCall = homePageCall,
        sources = user.incomeSources,
        backUrl = hub.controllers.routes.HomeController.show().url
      )(implicitly, itvcErrorHandler)
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async  {
    implicit mtdItUser =>
      handleRequest(
        isAgent = true,
        homePageCall = homePageCallAgent,
        sources = mtdItUser.incomeSources,
        backUrl = hub.controllers.routes.HomeController.showAgent().url
      )(implicitly, itvcErrorHandlerAgent)

  }

  def handleRequest(sources: IncomeSourceDetailsModel,
                    homePageCall: Call,
                    isAgent: Boolean,
                    backUrl: String)
                   (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    incomeSourceDetailsService.getAddIncomeSourceViewModel(sources, isEnabled(DisplayBusinessStartDate)) match {
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
