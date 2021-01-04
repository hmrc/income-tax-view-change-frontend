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
import config.featureswitch.{API5, FeatureSwitching}
import connectors.FinancialTransactionsConnector
import javax.inject.{Inject, Singleton}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, FinancialTransactionsResponseModel}
import play.api.Logger
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialTransactionsService @Inject()(val financialTransactionsConnector: FinancialTransactionsConnector)
                                            (implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def getFinancialTransactions(mtditid: String, taxYear: Int)(implicit headCarrier: HeaderCarrier): Future[FinancialTransactionsResponseModel] = {
    Logger.debug(s"[FinancialTransactionsService][getFinancialTransactions] - Requesting Financial Transactions from connector for mtditid: $mtditid")
    financialTransactionsConnector.getFinancialTransactions(mtditid, taxYear)
  }

  def getAllFinancialTransactions(implicit user: MtdItUser[AnyContent],
                                  hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[List[(Int, FinancialTransactionsResponseModel)]] = {
    Logger.debug(
      s"[FinancialTransactionsService][getAllFinancialTransactions] - Requesting Financial Transactions for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYears(isEnabled(API5)).map {
      taxYear =>
        financialTransactionsConnector.getFinancialTransactions(user.mtditid, taxYear).map {
          case transaction: FinancialTransactionsModel => Some((taxYear, transaction))
          case error: FinancialTransactionsErrorModel if error.code != 404 => Some((taxYear, error))
          case _ => None
        }
    }
    ).map(_.flatten)
  }

  def getAllUnpaidFinancialTransactions(implicit user: MtdItUser[AnyContent],
                                        hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialTransactionsResponseModel]] = {
    getAllFinancialTransactions.map { transactionsWithYears =>
      transactionsWithYears.collect {
        case (_, transactionModel: FinancialTransactionsErrorModel) => transactionModel
        case (taxYear, transactionModel: FinancialTransactionsModel) if !transactionModel.isAllPaid(taxYear) =>
          transactionModel.copy(
            financialTransactions = transactionModel.financialTransactions map { transactions =>
              transactions.filterNot(transaction => transaction.originalAmount.exists(_ <= 0) || transaction.outstandingAmount.isEmpty)
            }
          )
      }
    }
  }

}

