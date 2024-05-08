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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SingleYearOptOutConfirmationController @Inject()(implicit val appConfig: FrontendAppConfig,
                                                       val ec: ExecutionContext,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] = {
    Action(Ok(""))
  }

}