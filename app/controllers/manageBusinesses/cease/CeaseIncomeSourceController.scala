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

package controllers.manageBusinesses.cease

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.JourneyType.Cease
import models.admin.DisplayBusinessStartDate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.cease.CeaseIncomeSourcesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseIncomeSourceController @Inject()(val ceaseIncomeSources: CeaseIncomeSourcesView,
                                            val authActions: AuthActions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val sessionService: SessionService)
                                           (implicit val ec: ExecutionContext,
                                            val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def show(): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async  {
    implicit mtdItUser =>
      handleRequest(
        sources = mtdItUser.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.showAgent().url
      )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    showCeaseIncomeSourceView(sources, isAgent, backUrl)
  }

  private def showCeaseIncomeSourceView(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)(implicit user: MtdItUser[_]): Future[Result] = {
    incomeSourceDetailsService.getCeaseIncomeSourceViewModel(sources, isEnabled(DisplayBusinessStartDate)) match {
      case Right(viewModel) =>
        sessionService.deleteSession(Cease).map { _ =>
          Ok(ceaseIncomeSources(
            viewModel,
            isAgent = isAgent,
            backUrl = backUrl
          ))
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Session Error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
      case Left(ex) =>
        Logger("application").error(
          s"Error: ${ex.getMessage} - ${ex.getCause}")
        Future(showInternalServerError(isAgent))
    }
  }

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
  }
}