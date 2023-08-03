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

import auth.FrontendAuthorisedFunctions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeSourceDetailsService
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CeaseUKProperty

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmReportingMethodSharedController @Inject()(val authenticate: AuthenticationPredicate,
                                                       val authorisedFunctions: FrontendAuthorisedFunctions,
                                                       val checkSessionTimeout: SessionTimeoutPredicate,
                                                       val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                       val retrieveBtaNavBar: NavBarPredicate,
                                                       val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                       val retrieveNino: NinoPredicate,
                                                       val view: CeaseUKProperty,
                                                       val customNotFoundErrorView: CustomNotFoundError)
                                                      (implicit val appConfig: FrontendAppConfig,
                                        mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                       )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def showSoleTraderBusiness(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm Sole Trader - WIP")
  }

  def showSoleTraderBusinessAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm Sole Trader - WIP")
  }

  def showUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm UK Property - WIP")
  }

  def showUKPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm UK Property - WIP")
  }

  def showForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm Foreign Property - WIP")
  }

  def showForeignPropertyAgent(taxYear: String, changeTo: String): Action[AnyContent] = Action {
    Ok("Confirm Foreign Property - WIP")
  }

}
