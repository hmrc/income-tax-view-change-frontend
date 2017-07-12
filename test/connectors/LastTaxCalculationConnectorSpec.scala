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

package connectors


import assets.TestConstants.Estimates._
import assets.TestConstants._
import mocks.MockHttp
import models.{LastTaxCalculationError, LastTaxCalculationResponseModel, NoLastTaxCalculation}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future

class LastTaxCalculationConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(lastTaxCalcSuccess)))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val noDataFound = HttpResponse(Status.NOT_FOUND)
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestLastTaxCalculationConnector extends LastTaxCalculationConnector(mockHttpGet)

  "EstimatedTaxLiabilityConnector.redirectToEarliestEstimatedTaxLiability" should {

    lazy val testUrl = TestLastTaxCalculationConnector.getEstimatedTaxLiabilityUrl(testNino, testYear.toString)
    def result: Future[LastTaxCalculationResponseModel] = TestLastTaxCalculationConnector.getLastEstimatedTax(testNino, testYear)

    "return a EstimatedTaxLiability model when successful JSON is received" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe lastTaxCalcSuccess
    }

    "return EstimatedTaxLiabilityError model in case of bad/malformed JSON response" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Estimated Tax Liability Response.")
    }

    "return EstimatedTaxLiabilityError model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe LastTaxCalculationError(Status.BAD_REQUEST, "Error Message")
    }

    "return NoLastTaxCalculation case object in case of No Data Found (NOT_FOUND) scenario" in {
      setupMockHttpGet(testUrl)(noDataFound)
      await(result) shouldBe NoLastTaxCalculation
    }

    "return LastTaxCalculationError model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe LastTaxCalculationError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $testUrl.")
    }
  }
}
