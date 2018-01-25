/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.FinancialTransactionsConnector
import models.FinancialTransactionsResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class FinancialTransactionsService @Inject()(val financialTransactionsConnector: FinancialTransactionsConnector){

  def getFinancialTransactions(nino: String)(implicit headCarrier: HeaderCarrier): Future[FinancialTransactionsResponseModel] = {
    Logger.debug(
      s"[FinancialTransactionsService][getFinancialTransactions] - Requesting Financial Transactions from connector for nino: $nino"
    )
    financialTransactionsConnector.getFinancialTransactions(nino)
  }

}
