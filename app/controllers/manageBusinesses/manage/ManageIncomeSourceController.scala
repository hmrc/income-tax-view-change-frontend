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

package controllers.manageBusinesses.manage

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.JourneyType.Manage
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.manageBusinesses.manage.ManageIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val sessionService: SessionService,
                                             val auth: AuthenticatorPredicate)
                                            (implicit val ec: ExecutionContext,
                                             implicit override val mcc: MessagesControllerComponents,
                                             implicit val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
    handleRequest(
      sources = user.incomeSources,
      isAgent = isAgent,
      backUrl = {
        if(isAgent) controllers.routes.HomeController.showAgent
        else controllers.routes.HomeController.show()
      }.url
    )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    withIncomeSourcesFS {
      incomeSourceDetailsService.getViewIncomeSourceViewModel(sources) match {
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
                s"[ManageIncomeSourceController][handleRequest] - Session Error: ${ex.getMessage} - ${ex.getCause}")
              showInternalServerError(isAgent)
          }
        case Left(ex) =>
          Logger("application").error(
            s"[ManageIncomeSourceController][handleRequest] - Error: ${ex.getMessage} - ${ex.getCause}")
          Future(showInternalServerError(isAgent))
      }
    }
  }
}