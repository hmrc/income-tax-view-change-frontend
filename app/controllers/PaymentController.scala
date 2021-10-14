/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.InitiatePayNowAuditModel
import config.FrontendAppConfig
import connectors.PayApiConnector
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentController @Inject()(implicit val config: FrontendAppConfig,
                                  mcc: MessagesControllerComponents,
                                  implicit val executionContext: ExecutionContext,
                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                  val authenticate: AuthenticationPredicate,
                                  payApiConnector: PayApiConnector,
                                  val auditingService: AuditingService
                                 ) extends BaseController {

  val action = checkSessionTimeout andThen authenticate

  val paymentHandoff: Long => Action[AnyContent] = paymentAmountInPence => action.async {
    implicit user =>
      auditingService.extendedAudit(
        InitiatePayNowAuditModel(user.mtditid, user.nino, user.saUtr, user.credId, user.userType)
      )
      user.saUtr match {
        case Some(utr) =>
          payApiConnector.startPaymentJourney(utr, paymentAmountInPence).map {
          case model: PaymentJourneyModel => Redirect(model.nextUrl)
          case _: PaymentJourneyErrorResponse => throw new Exception("Failed to start payments journey due to downstream response")
        }
        case _ =>
          Logger("application").error("Failed to start payments journey due to missing UTR")
          Future.failed( new Exception("Failed to start payments journey due to missing UTR"))
      }
  }
}
