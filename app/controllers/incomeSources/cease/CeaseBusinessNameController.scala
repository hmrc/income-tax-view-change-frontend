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

import auth.FrontendAuthorisedFunctions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeSourceDetailsService
import views.html.incomeSources.cease.CeaseUKProperty
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CeaseBusinessNameController @Inject()(val authenticate: AuthenticationPredicate,
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

  def show(origin: Option[String] = None): Action[AnyContent] = Action {
    Ok("")
  }

  def showAgent(): Action[AnyContent] = Action {
    Ok("")
  }

}
