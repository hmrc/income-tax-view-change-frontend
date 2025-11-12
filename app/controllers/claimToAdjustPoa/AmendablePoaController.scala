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

package controllers.claimToAdjustPoa

import auth.authV2.AuthActions
import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.InitialPage
import implicits.ImplicitCurrencyFormatter
import models.core.Nino
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ErrorRecovery
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendablePoaController @Inject()(val authActions: AuthActions,
                                       val claimToAdjustService: ClaimToAdjustService,
                                       val poaSessionService: PaymentOnAccountSessionService,
                                       view: AmendablePaymentOnAccount)
                                      (implicit val appConfig: FrontendAppConfig,
                                       val individualErrorHandler: ItvcErrorHandler,
                                       val agentErrorHandler: AgentItvcErrorHandler,
                                       val mcc: MessagesControllerComponents,
                                       val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport
    with ImplicitCurrencyFormatter
    with WithSessionAndPoa with ErrorRecovery {

  def show(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
      implicit user =>
        withSessionData(journeyState = InitialPage) { _ => {
          for {
            poaMaybe <- EitherT(claimToAdjustService.getAmendablePoaViewModel(Nino(user.nino)))
          } yield poaMaybe
        }.value.flatMap {
          case Right(viewModel) =>
            Future.successful(
              Ok(view(user.isAgent(), viewModel))
            )
          case Left(ex) =>
            Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}")
            Future.failed(ex)
        }
      } recover logAndRedirect
    }
}
