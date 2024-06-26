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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes
import implicits.ImplicitCurrencyFormatter
import models.admin.AdjustPaymentsOnAccount
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import utils.claimToAdjust.ClaimToAdjustUtils
import views.html.claimToAdjustPoa.ApiFailureSubmittingPoaView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiFailureSubmittingPoaController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                  val auth: AuthenticatorPredicate,
                                                  view: ApiFailureSubmittingPoaView,
                                                  implicit val itvcErrorHandler: ItvcErrorHandler,
                                                  implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                 (implicit val appConfig: FrontendAppConfig,
                                                  implicit override val mcc: MessagesControllerComponents,
                                                  val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ClaimToAdjustUtils {

  def show(isAgent: Boolean): Action[AnyContent] = {
    auth.authenticatedAction(isAgent) {
      implicit user =>
        ifAdjustPoaIsEnabled(isAgent) {
          Future.successful(Ok(view(isAgent)))
        } recover {
          case ex: Throwable =>
            Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
    }
  }
}
