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

package services

import auth.MtdItUser
import connectors.FinancialTransactionsConnector
import javax.inject.{Inject, Singleton}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, FinancialTransactionsResponseModel, TransactionModel}
import play.api.Logger
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialTransactionsService @Inject()(val financialTransactionsConnector: FinancialTransactionsConnector){

  def getFinancialTransactions(mtditid: String, taxYear: Int)(implicit headCarrier: HeaderCarrier): Future[FinancialTransactionsResponseModel] = {
    Logger.debug(s"[FinancialTransactionsService][getFinancialTransactions] - Requesting Financial Transactions from connector for mtditid: $mtditid")
    financialTransactionsConnector.getFinancialTransactions(mtditid, taxYear)
  }

  def getAllFinancialTransactions(implicit user: MtdItUser[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialTransactionsResponseModel)]] = {
    Logger.debug(s"[FinancialTransactionsService][getAllFinancialTransactions] - Requesting Financial Transactions for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYears.map {
      taxYear => financialTransactionsConnector.getFinancialTransactions(user.mtditid, taxYear).map {
        case transaction: FinancialTransactionsModel => Some((taxYear, transaction))
        case error: FinancialTransactionsErrorModel if error.code >= 500 => Some((taxYear, error))
        case _ => None
      }
    }
    ).map (_.flatten)
  }

  def getAllUnpaidFinancialTransactions(implicit user: MtdItUser[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialTransactionsResponseModel]] = {
    Logger.debug(s"[FinancialTransactionsService][getAllUnpaidFinancialTransactions] - filtering all Financial Transactions for all periods for mtditid: ${user.mtditid}")
    getAllFinancialTransactions.map { transactionsWithYear =>
      transactionsWithYear.filter{
        case (_, transactionModel: FinancialTransactionsErrorModel) => true
        case (taxYear, transactionModel: FinancialTransactionsModel) => transactionModel.financialTransactions.getOrElse{
          Logger.info(s"[FinancialTransactionsService][getAllUnpaidFinancialTransactions] - no financial transactions for mtditid: ${user.mtditid} and taxYear: $taxYear")
          List.empty[TransactionModel]
        }.exists(!_.isPaid)
      }.map(_._2)
    }
  }

}

