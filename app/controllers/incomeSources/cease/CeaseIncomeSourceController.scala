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

package controllers.incomeSources.cease

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.cease.CeaseIncomeSources

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseIncomeSourceController @Inject()(val ceaseIncomeSources: CeaseIncomeSources,
                                            val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val retrieveBtaNavBar: NavBarPredicate)
                                           (implicit val ec: ExecutionContext,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils{

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
    )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.routes.HomeController.showAgent.url
            )
        }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS{
    showCeaseIncomeSourceView(sources, isAgent, backUrl)
  }

  private def showCeaseIncomeSourceView(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String) (implicit user: MtdItUser[_]) = {
    Future {
      incomeSourceDetailsService.getCeaseIncomeSourceViewModel(sources) match {
        case Right(viewModel) =>
          Ok(ceaseIncomeSources(
            viewModel,
            isAgent = isAgent,
            backUrl = backUrl
          ))
        case Left(ex) =>
          if (isAgent) {
            Logger("application").error(
              s"[Agent][CeaseIncomeSourceController][handleRequest] - Error: ${ex.getMessage}")
            itvcErrorHandlerAgent.showInternalServerError()
          } else {
            Logger("application").error(
              s"[CeaseIncomeSourceController][handleRequest] - Error: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()
          }
      }
    }
  }
}