/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import helpers.WiremockHelper
import models.FinancialTransactionsModel
import play.api.http.Status
import play.api.libs.json.Json

object FinancialTransactionsStub {

  val financialTransactionsUrl: String => String = nino => s"/financial-transactions/it/$nino"

  //Financial Transactions
  def stubGetFinancialTransactions(nino: String, ftData: FinancialTransactionsModel) : Unit =
    WiremockHelper.stubGet(financialTransactionsUrl(nino), Status.OK, Json.toJson(ftData).toString())

  def stubFinancialTransactionsError(nino: String) : Unit =
    WiremockHelper.stubGet(financialTransactionsUrl(nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  //Verifications
  def verifyGetFinancialTransactions(nino: String): Unit =
    WiremockHelper.verifyGetWithHeader(financialTransactionsUrl(nino), "Accept", "application/vnd.hmrc.1.0+json")

}
