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

package controllers

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.{Logger, Logging}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.JourneyStartAgainView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyRestartController @Inject()(authenticate: AuthenticationPredicate,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         checkSessionTimeout: SessionTimeoutPredicate,
                                         retrieveNino: NinoPredicate,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                         val retrieveBtaNavBar: NavBarPredicate,
                                         val sessionService: SessionService,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         startAgainView: JourneyStartAgainView
                                        )(implicit val ec: ExecutionContext,
                                          override val mcc: MessagesControllerComponents,
                                          itvcErrorHandler: ItvcErrorHandler,
                                          itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with I18nSupport with Logging {

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async { implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
            authenticatedCodeBlock(mtdItUser)
          }
      }
    } else {
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
    }
  }

  def show(isAgent: Boolean): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit request =>
      sessionService.remove("addBusinessTrade", Ok(startAgainView())).flatMap {
        //TODO: remove all income sources data
        case Left(exception) => Future.failed(exception)
        case Right(result) => Future.successful(result)
      }.recover {
        case exception =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[JourneyRestartController][show] ${exception.getMessage}")
          errorHandler.showInternalServerError()
      }
  }
}