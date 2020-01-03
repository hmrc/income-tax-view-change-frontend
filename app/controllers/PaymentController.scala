/*
 * Copyright 2020 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.PayApiConnector
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

@Singleton
class PaymentController @Inject()(implicit val config: FrontendAppConfig,
                                  implicit val messagesApi: MessagesApi,
                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                  val authenticate: AuthenticationPredicate,
                                  payApiConnector: PayApiConnector) extends BaseController {

  val action = checkSessionTimeout andThen authenticate

  val paymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => action.async {
    implicit user =>
      user.saUtr match {
        case Some(utr) =>
          payApiConnector.startPaymentJourney(utr, paymentAmountInPence).map {
          case model: PaymentJourneyModel => Redirect(model.nextUrl)
          case _: PaymentJourneyErrorResponse => throw new Exception("Failed to start payments journey due to downstream response")
        }
        case _ =>
          Logger.error("Failed to start payments journey due to missing UTR")
          throw new Exception("Failed to start payments journey due to missing UTR")
      }
  }
}
