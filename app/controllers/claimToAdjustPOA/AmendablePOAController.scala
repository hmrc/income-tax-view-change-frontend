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

package controllers.claimToAdjustPOA

import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes
import implicits.ImplicitCurrencyFormatter
import models.claimToAdjustPOA.PaymentOnAccountViewModel
import models.core.Nino
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ClaimToAdjustService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.AmendablePaymentOnAccount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import utils.Utilities.ToFutureSuccessful

@Singleton
class AmendablePOAController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                       claimToAdjustService: ClaimToAdjustService,
                                       val auth: AuthenticatorPredicate,
                                       view: AmendablePaymentOnAccount,
                                       implicit val itvcErrorHandler: ItvcErrorHandler,
                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                      (implicit val appConfig: FrontendAppConfig,
                                       implicit override val mcc: MessagesControllerComponents,
                                       val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitCurrencyFormatter {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        if (isEnabled(AdjustPaymentsOnAccount)) {
          claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)) flatMap {
            case Right(Some(paymentOnAccount: PaymentOnAccountViewModel)) =>
              Future.successful(
                Ok(view(
                  isAgent = isAgent,
                  paymentOnAccount = paymentOnAccount
                ))
              )
            case Right(None) =>
              Logger("application").error(s"Failed to create PaymentOnAccount model")
              ( (showInternalServerError(isAgent)) ).asFuture 
            case Left(ex) =>
              Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}")
              Future.failed(ex)
          }
        } else {
          ( (
            Redirect(
              if (isAgent) routes.HomeController.showAgent
              else         routes.HomeController.show()
            )
          ) ).asFuture 
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
    }
}
