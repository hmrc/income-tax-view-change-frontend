/*
 * Copyright 2024 HM Revenue & Customs
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
import views.html.manageBusinesses.cease.ViewAllCeasedBusinesses

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewAllCeasedBusinessesController @Inject()(val viewAllCeasedBusinesses: ViewAllCeasedBusinesses,
                                                  val authActions: AuthActions,
                                                  val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                  val sessionService: SessionService,
                                                  val itvcErrorHandler: ItvcErrorHandler,
                                                  val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                 (implicit val ec: ExecutionContext,
                                             val mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with IncomeSourcesUtils {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    val backUrl = if(isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
    }
    handleRequest(
      sources = user.incomeSources,
      isAgent = isAgent,
      backUrl = backUrl
    )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    lazy val errorHandler = if(isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    incomeSourceDetailsService.getCeaseIncomeSourceViewModel(sources, isEnabled(DisplayBusinessStartDate)) match {
      case Right(viewModel) =>
        sessionService.deleteSession(Manage).map { _ =>
          Ok(viewAllCeasedBusinesses(
            sources = viewModel,
            isAgent = isAgent,
            backUrl = backUrl
          ))
        } recover {
          case ex: Exception =>
            Logger("application").error(
              s"Session Error: ${ex.getMessage} - ${ex.getCause}")
            errorHandler.showInternalServerError()
        }
      case Left(ex) =>
        Logger("application").error(
          s"Error: ${ex.getMessage} - ${ex.getCause}")
        Future(errorHandler.showInternalServerError())
    }
  }
}