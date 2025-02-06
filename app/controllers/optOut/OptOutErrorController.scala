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

import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optOut.OptOutError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutErrorController @Inject() (
    val view:                  OptOutError,
    val authActions:           AuthActions,
    val itvcErrorHandler:      ItvcErrorHandler,
    val itvcErrorHandlerAgent: AgentItvcErrorHandler
  )(
    implicit val appConfig: FrontendAppConfig,
    val ec:                 ExecutionContext,
    val mcc:                MessagesControllerComponents)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      Future.successful(Ok(view(isAgent)))
    }

}
