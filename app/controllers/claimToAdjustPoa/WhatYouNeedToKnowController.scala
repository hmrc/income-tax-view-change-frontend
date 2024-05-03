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

import auth.MtdItUser
import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes.HomeController
import models.core.Nino
import models.paymentOnAccount.PaymentOnAccount
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.ClaimToAdjustService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouNeedToKnowController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            val view: WhatYouNeedToKnow,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            auth: AuthenticatorPredicate,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  private def getRedirect(isAgent: Boolean, totalAmountLessThanPoa: Boolean): String = {
    (if (totalAmountLessThanPoa) {
      controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent)
    } else {
      controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent)
    }).url
  }

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      if (isEnabled(AdjustPaymentsOnAccount)) {
        claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)).flatMap {
          case Right(Some(poa)) =>
            Future.successful(Ok(view(isAgent, poa.taxYear, getRedirect(isAgent, totalAmountLessThanPoa(poa)))))
          case Right(None) => Logger("application").error(s"[WhatYouNeedToKnowController][show]")
            Future.successful(showInternalServerError(isAgent))
          case Left(ex) =>
            Logger("application").error(s"[WhatYouNeedToKnowController][show] ${ex.getMessage} - ${ex.getCause}")
            Future.successful(showInternalServerError(isAgent))
        }
      }else {
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

  private def totalAmountLessThanPoa(poaModel: PaymentOnAccount): Boolean = {
    (poaModel.paymentOnAccountOne + poaModel.paymentOnAccountTwo) < (poaModel.poARelevantAmountOne + poaModel.poARelevantAmountTwo)
  }
}
