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

package controllers.claimToAdjustPoa

import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claimToAdjustPoa.ApiFailureSubmittingPoaView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiFailureSubmittingPoaController @Inject()(val authActions: AuthActions,
                                                  view: ApiFailureSubmittingPoaView,
                                                  implicit val itvcErrorHandler: ItvcErrorHandler,
                                                  implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                 (implicit val appConfig: FrontendAppConfig,
                                                  val mcc: MessagesControllerComponents,
                                                  val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(isAgent: Boolean): Action[AnyContent] = {
    authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
      implicit user =>
          Future.successful(Ok(view(user.isAgent())))
    }
  }
}
