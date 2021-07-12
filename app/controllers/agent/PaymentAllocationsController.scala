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

package controllers.agent

import config.featureswitch.{FeatureSwitching, PaymentAllocation}
import config.{FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitDateFormatterImpl
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.PaymentAllocation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(paymentAllocation: PaymentAllocation,
                                             incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                             val authorisedFunctions: AuthorisedFunctions
                                            )(implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              dateFormatter: ImplicitDateFormatterImpl,
                                              implicit val ec: ExecutionContext,
                                              val itvcErrorHandler: ItvcErrorHandler
                                            ) extends ClientConfirmedController with FeatureSwitching {

  lazy val backUrl: String = controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url

  def viewPaymentAllocation(documentNumber: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(PaymentAllocation)) {
        incomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(getClientNino(request), documentNumber) map {
          case paymentAllocations: FinancialDetailsWithDocumentDetailsModel =>
            Ok(paymentAllocation(paymentAllocations, dateFormatter, backUrl = backUrl))
          case _ => itvcErrorHandler.showInternalServerError()
        }
      } else Future.failed(new NotFoundException("[PaymentAllocationsController] - PaymentAllocation is disabled"))
  }

}
