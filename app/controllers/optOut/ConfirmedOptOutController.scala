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

package controllers.optOut

import auth.FrontendAuthorisedFunctions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.optOut.ConfirmedOptOutController.IsAgent.{IsAgentNo, IsAgentYes}
import controllers.predicates._
import models.optOut.OptOutUpdateRequestModel.OptOutUpdateResponseSuccess
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeSourceDetailsService
import utils.AuthenticatorPredicate
import views.html.errorPages.CustomNotFoundError
import views.html.optOut.ConfirmedOptOut
import services.optout.OptOutService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

object ConfirmedOptOutController {
  object IsAgent {
    val IsAgentYes: Boolean = true
    val IsAgentNo: Boolean = false
  }
}

class ConfirmedOptOutController @Inject()(val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val confirmedOptOut: ConfirmedOptOut,
                                          val optOutService: OptOutService,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val customNotFoundErrorView: CustomNotFoundError,
                                          val auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                         )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] = auth.authenticatedAction(IsAgentNo) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Ok(confirmedOptOut(IsAgentNo))
        case _ => itvcErrorHandler.showInternalServerError()//todo is that the correct error page
      }
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(IsAgentYes) {
    implicit mtdItUser =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Ok(confirmedOptOut(IsAgentYes))//todo is that the same for agant?
        case _ => itvcErrorHandler.showInternalServerError()//todo is that the correct error page
      }
  }

}