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

import assets.BaseTestConstants._
import assets.FinancialTransactionsTestConstants._
import mocks.connectors.MockFinancialTransactionsConnector
import utils.TestSupport


class FinancialTransactionsServiceSpec extends TestSupport with MockFinancialTransactionsConnector{

  object TestFinancialTransactionsService extends FinancialTransactionsService(mockFinancialTransactionsConnector)

  "The FinancialTransactionsService.getFinancialTransactions method" when {

    "a successful financial transaction repsonse is returned from the connector" should {

      "return a valid FinancialTransactions model" in {
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsModel())
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsModel()
      }

    }

    "a error model is returned from the connector" should {

      "return a FinancialTransactionsError model" in {
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsErrorModel)
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsErrorModel
      }

    }



  }

}
