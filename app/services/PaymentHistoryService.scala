/*
 * Copyright 2022 HM Revenue & Customs
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
import models.core.Nino

import javax.inject.Inject
import models.financialDetails.{Payment, Payments, PaymentsError}
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryErrorModel, RepaymentHistoryModel}
import services.PaymentHistoryService.PaymentHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
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
        }.flatten.distinct)
      }
    }
  }

  def getRepaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[RepaymentHistoryErrorModel.type, List[RepaymentHistory]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    Future.sequence(orderedTaxYears.map(year =>
      incomeTaxViewChangeConnector.getRepaymentHistoryByRepaymentDate(Nino(user.nino),
        fromDate = s"${year - 1}-04-06",
        toDate = s"$year-04-05"
      ))) map { repaymentResponses =>
      val repaymentsContainsFailure: Boolean = repaymentResponses.exists {
        case RepaymentHistoryModel(_) => false
        case RepaymentHistoryErrorModel(status, _) if status == 404 => false
        case RepaymentHistoryErrorModel(_, _) => true
      }
      if (repaymentsContainsFailure) {
        Left(RepaymentHistoryErrorModel)
      } else {
        Right(repaymentResponses.collect {
          case RepaymentHistoryModel(repaymentsViewerDetails) => repaymentsViewerDetails
        }.flatten.distinct)
      }
    }
  }
}

object PaymentHistoryService {
  case object PaymentHistoryError
}