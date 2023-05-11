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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import config.featureswitch.FeatureSwitching
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseIncomeSourceController @Inject()(val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: FrontendAuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            val checkSessionTimeout: SessionTimeoutPredicate)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest()
  }

  def showAgent(): Action[AnyContent] = Action {
    Ok("")
  }

  def handleRequest()(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    Future {
      Ok("Hello World")
    }
  }
}
