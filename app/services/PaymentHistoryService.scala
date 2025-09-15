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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import connectors.{FinancialDetailsConnector, RepaymentHistoryConnector}
import models.core.Nino
import models.financialDetails.{Payment, Payments, PaymentsError}
import models.incomeSourceDetails.TaxYear
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryErrorModel, RepaymentHistoryModel}
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import services.PaymentHistoryService.PaymentHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PaymentHistoryService @Inject()(repaymentHistoryConnector: RepaymentHistoryConnector,
                                      financialDetailsConnector: FinancialDetailsConnector,
                                      implicit val dateService: DateServiceInterface,
                                      val appConfig: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) {

  def getPaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[Payment]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    Future.sequence(orderedTaxYears.map(year => financialDetailsConnector.getPayments(TaxYear(year-1, year)))) map { paymentResponses =>
      val paymentsContainsFailure: Boolean = paymentResponses.exists {
        case Payments(_) => false
        case PaymentsError(status, _) if status == NOT_FOUND => false
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

  def getPaymentHistoryV2(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, Seq[Payment]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    val (from, to) = (orderedTaxYears.min, orderedTaxYears.max)
    Logger("application").debug(s"Getting payment history for TaxYears: ${from} - ${to}")

    for {
      response <- financialDetailsConnector.getPayments(TaxYear.forYearEnd(from), TaxYear.forYearEnd(to))
    } yield response match {
      case Payments(payments) => Right(payments.distinct)
      case PaymentsError(_, _) => Left(PaymentHistoryError)
    }
  }

  def getRepaymentHistory(paymentHistoryAndRefundsEnabled: Boolean)
                         (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[RepaymentHistoryErrorModel.type, List[RepaymentHistory]]] = {

    if (paymentHistoryAndRefundsEnabled)
      repaymentHistoryConnector.getRepaymentHistoryByNino(Nino(user.nino)).map {
        case RepaymentHistoryModel(repaymentsViewerDetails) => Right(repaymentsViewerDetails)
        case RepaymentHistoryErrorModel(status, _) if status == 404 => Right(List())
        case RepaymentHistoryErrorModel(_, _) => Left(RepaymentHistoryErrorModel)
      }
    else Future(Right(Nil))
  }
}

object PaymentHistoryService {
  case object PaymentHistoryError
}
