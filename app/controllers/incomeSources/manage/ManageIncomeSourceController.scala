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

package controllers.incomeSources.manage

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.JourneyType.Manage
import models.admin.DisplayBusinessStartDate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val sessionService: SessionService,
                                             val authActions: AuthActions,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                            (implicit val ec: ExecutionContext,
                                             val mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    handleRequest(
      sources = user.incomeSources,
      isAgent = isAgent,
      backUrl = {
        if(isAgent) controllers.routes.HomeController.showAgent()
        else controllers.routes.HomeController.show()
      }.url
    )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    withIncomeSourcesFS {
      incomeSourceDetailsService.getViewIncomeSourceViewModel(sources, isEnabled(DisplayBusinessStartDate)) match {
        case Right(viewModel) =>
          sessionService.deleteSession(Manage).map { _ =>
            Ok(manageIncomeSources(
              sources = viewModel,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          } recover {
            case ex: Exception =>
              Logger("application").error(
                s"Session Error: ${ex.getMessage} - ${ex.getCause}")
              showInternalServerError(isAgent)
          }
        case Left(ex) =>
          Logger("application").error(
            s"Error: ${ex.getMessage} - ${ex.getCause}")
          Future(showInternalServerError(isAgent))
      }
    }
  }
  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler)
      .showInternalServerError()
  }
}