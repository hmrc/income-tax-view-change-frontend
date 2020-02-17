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

package connectors

import assets.BaseTestConstants.{testMtditid, testTaxYear}
import assets.FinancialTransactionsTestConstants._
import mocks.MockHttp
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class FinancialTransactionsConnectorSpec extends TestSupport with MockHttp{

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(financialTransactionsModel())))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))


  object TestFinancialTransactionsConnector extends FinancialTransactionsConnector(mockHttpGet, appConfig)

  "FinancialTransactionsConnector.getFinancialTransactions" should {

    lazy val testUrl = TestFinancialTransactionsConnector.getFinancialTransactionsUrl(testMtditid)
    lazy val testParams = Seq("dateFrom" -> "2017-04-06", "dateTo" -> "2018-04-05")
    def result: Future[FinancialTransactionsResponseModel] = TestFinancialTransactionsConnector.getFinancialTransactions(testMtditid, testTaxYear)

    "return a FinancialTransactionsModel with JSON in case of success" in {
      setupMockHttpGetWithParams(testUrl,testParams)(successResponse)
      await(result) shouldBe financialTransactionsModel()
    }

    "return FinancialTransactionsErrorModel when bad Json is recieved" in {
      setupMockHttpGetWithParams(testUrl,testParams)(successResponseBadJson)
      await(result) shouldBe FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Financial Transactions response")
    }

    "return FinancialTransactionErrorModel when bad request recieved" in {
      setupMockHttpGetWithParams(testUrl,testParams)(badResponse)
      await(result) shouldBe FinancialTransactionsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return FinancialTransactionErrorModel when GET fails" in {
      setupMockFailedHttpGetWithParams(testUrl,testParams)(badResponse)
      await(result) shouldBe FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed, unknown error")
    }

  }

}
