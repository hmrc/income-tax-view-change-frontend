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

import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.InitialPage
import implicits.ImplicitCurrencyFormatter
import models.core.Nino
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendablePOAController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                       val claimToAdjustService: ClaimToAdjustService,
                                       val auth: AuthenticatorPredicate,
                                       val poaSessionService: PaymentOnAccountSessionService,
                                       view: AmendablePaymentOnAccount,
                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                      (implicit val appConfig: FrontendAppConfig,
                                       implicit override val mcc: MessagesControllerComponents,
                                       val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with ClaimToAdjustUtils with ImplicitCurrencyFormatter with WithSessionAndPoa {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        withSessionData(journeyState = InitialPage) { _ => {
          for {
            poaMaybe <- EitherT(claimToAdjustService.getAmendablePoaViewModel(Nino(user.nino)))
          } yield poaMaybe
        }.value.flatMap {
          case Right(viewModel) =>
            Future.successful(
              Ok(view(isAgent, viewModel))
            )
          case Left(ex) =>
            Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}")
            Future.failed(ex)
        }
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
    }
}
