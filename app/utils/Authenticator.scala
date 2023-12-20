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

package utils

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.{BaseAgentController, ClientConfirmedController}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Authenticator @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                              val authenticate: AuthenticationPredicate,
                              val authorisedFunctions: AuthorisedFunctions,
                              val retrieveBtaNavBar: NavBarPredicate,
                              val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                              val incomeSourceDetailsService: IncomeSourceDetailsService)
                             (implicit mcc: MessagesControllerComponents,
                              val appConfig: FrontendAppConfig,
                              val itvcErrorHandler: AgentItvcErrorHandler,
                              val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport {


  def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}
