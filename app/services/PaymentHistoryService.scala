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

package services
import auth.MtdItUser
import config.FrontendAppConfig
import connectors.IncomeTaxViewChangeConnector
import javax.inject.Inject
import models.financialDetails.{Payment, Payments, PaymentsError}
import services.PaymentHistoryService.PaymentHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PaymentHistoryService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector, val appConfig: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) {

  def getPaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[Payment]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    Future.sequence(orderedTaxYears.map(incomeTaxViewChangeConnector.getPayments)) map { paymentResponses =>
      val paymentsContainsFailure: Boolean = paymentResponses.exists {
        case Payments(_) => false
        case PaymentsError(status, _) if status == 404 => false
        case PaymentsError(_, _) => true
      }
      if (paymentsContainsFailure) {
        Left(PaymentHistoryError)
      } else {
        Right(paymentResponses.collect {
          case Payments(payments) => payments
        }.flatten)
      }
    }
  }
}
  object PaymentHistoryService {
  case object PaymentHistoryError
}