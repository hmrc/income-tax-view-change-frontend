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
import config.featureswitch.FeatureSwitching
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitCurrencyFormatter
import models.paymentOnAccount.PaymentOnAccount
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculationListService, ClaimToAdjustService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.AmendablePaymentOnAccount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendablePOAController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                       calculationListService: CalculationListService,
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
        claimToAdjustService.getPaymentsOnAccount flatMap {
          case Right(poa: PaymentOnAccount) =>
            calculationListService.isTaxYearCrystallised(poa.taxYear.endYear) map {
              case Some(false) | None =>
                Ok(view(
                  isAgent = isAgent,
                  taxYearModel = poa.taxYear,
                  poaOneTransactionId = poa.poaOneTransactionId,
                  poaTwoTransactionId = poa.poaTwoTransactionId,
                  poaOneFullAmount = poa.paymentOnAccountOne.toCurrencyString,
                  poaTwoFullAmount = poa.paymentOnAccountTwo.toCurrencyString
                ))
              case _ =>
                Logger("application").error("Tax Year of return is crystallized - Payment on Account cannot be adjusted")
                showInternalServerError(isAgent)
            }
          case Left(ex) =>
            Logger("application").error(s"Failed to retrieve PaymentOnAccount model: ${ex.getMessage} - ${ex.getCause}")
            Future.successful(showInternalServerError(isAgent))
        }
    }
}
