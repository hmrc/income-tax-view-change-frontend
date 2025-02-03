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
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel, Payment, Payments, PaymentsError}
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

  def getPaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, Seq[Payment]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    val (from, to) = (orderedTaxYears.min, orderedTaxYears.max)
    Logger("application").debug(s"Getting payment history for TaxYears: ${from} - ${to}")

    val maxYears = 5
    val listOfCalls = orderedTaxYears.grouped(maxYears).toList

    Future.sequence(listOfCalls.map { years =>
      val (from, to) = (years.min, years.max)
      Logger("application").debug(s"Getting payment history for TaxYears: ${from} - ${to}")

      for {
        response <- financialDetailsConnector.getPayments(TaxYear.forYearEnd(from), TaxYear.forYearEnd(to))
      } yield response match {
        case Payments(payments) => Right(payments.distinct)
        case PaymentsError(_, _) => Left(PaymentHistoryError)
      }
    }).map(x => {
      x.foldLeft[Either[PaymentHistoryError.type, Seq[Payment]]](Left(PaymentHistoryError)){(acc, next) =>
        combineTwoResponses(acc, next)
      }
    })
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

  def combineTwoResponses(response1: Either[PaymentHistoryError.type, Seq[Payment]],
                          response2: Either[PaymentHistoryError.type, Seq[Payment]]): Either[PaymentHistoryError.type, Seq[Payment]] = {
    (response1, response2) match {
      case (Right(model1: Seq[Payment]), Right(model2: Seq[Payment])) => Right(model1 ++ model2)
      case (Right(model: Seq[Payment]), _) => Right(model)
      case (_, Right(model: Seq[Payment])) => Right(model)
      case _ => Left(PaymentHistoryError)
    }
  }
}

object PaymentHistoryService {
  case object PaymentHistoryError
}
