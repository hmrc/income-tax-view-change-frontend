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

package controllers

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitCurrencyFormatter
import models.core.Nino
import models.paymentOnAccount.PaymentOnAccount
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ClaimToAdjustService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.AmendablePaymentOnAccount
import controllers.routes._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
          claimToAdjustService.getPoaTaxYearForEntryPoint(Nino(user.nino)) flatMap {
            case Right(Some(poa: PaymentOnAccount)) =>
              Future.successful(
                Ok(view(
                  isAgent = isAgent,
                  taxYearModel = poa.taxYear,
                  poaOneTransactionId = poa.poaOneTransactionId,
                  poaTwoTransactionId = poa.poaTwoTransactionId,
                  poaOneFullAmount = poa.paymentOnAccountOne.toCurrencyString,
                  poaTwoFullAmount = poa.paymentOnAccountTwo.toCurrencyString
                ))
              )
            case Right(None) =>
              Logger("application").error(s"Failed to create PaymentOnAccount model")
              Future.successful(showInternalServerError(isAgent))
            case Left(ex) =>
              Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}")
              Future.failed(ex)
          }
        } else {
          Future.successful(
            Redirect(
              if (isAgent) HomeController.showAgent
              else         HomeController.show()
            )
          )
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
            showInternalServerError(isAgent)
        }
    }
}
