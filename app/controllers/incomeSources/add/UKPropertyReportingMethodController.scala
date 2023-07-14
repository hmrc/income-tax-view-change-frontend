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

package controllers.incomeSources.add

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UKPropertyReportingMethodController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                                    val authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    val retrieveNino: NinoPredicate,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveBtaNavBar: NavBarPredicate)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with I18nSupport with FeatureSwitching {

  def show(id: String): Action[AnyContent] = Action {
    Ok("UK Property Reporting Method - Individual")
  }

  def showAgent(id: String): Action[AnyContent] = Action {
    Ok("UK Property Reporting Method - Agent")
  }


}

