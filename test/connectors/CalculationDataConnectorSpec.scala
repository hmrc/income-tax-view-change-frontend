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

package connectors

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import mocks.MockHttp
import models.calculation.{CalculationDataErrorModel, CalculationDataResponseModel, CalculationErrorModel, CalculationResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future

class CalculationDataConnectorSpec extends TestSupport with MockHttp {

  object TestCalculationDataConnector extends CalculationDataConnector(mockHttpGet, frontendAppConfig)

  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{\"incomeTaxYTD\":\"somethingBad\"}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  "CalculationDataConnector" should {

    "have the correct URL for the getCalculationData endpoint" in {
      TestCalculationDataConnector.getCalculationDataUrl(testNino, testTaxCalculationId) shouldBe
        "http://localhost:9084/ni/AB123456C/calculations/CALCID"
    }

    "have the correct URL for the getLatestCalculation endpoint" in {
      TestCalculationDataConnector.getLatestCalculationUrl(testNino, testTaxYear.toString) shouldBe
        "http://localhost:9082/income-tax-view-change/previous-tax-calculation/AB123456C/2018"
    }
  }

  "CalculationDataConnector.getCalculationData" should {

    val successResponse = HttpResponse(Status.OK, Some(mandatoryCalculationDataSuccessJson))

    lazy val url = TestCalculationDataConnector.getCalculationDataUrl(testNino, testTaxCalculationId)
    def result: Future[CalculationDataResponseModel] = TestCalculationDataConnector.getCalculationData(testNino, testTaxCalculationId)

    "return a CalculationDataModel with JSON in case of success" in {
      setupMockHttpGet(url)(successResponse)
      await(result) shouldBe mandatoryOnlyDataModel
    }

    "return CalculationDataErrorModel model in case of failure" in {
      setupMockHttpGet(url)(badResponse)
      await(result) shouldBe CalculationDataErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return CalculationDataErrorModel model when bad JSON is received" in {
      setupMockHttpGet(url)(successResponseBadJson)
      await(result) shouldBe CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Calc Breakdown Response")
    }

    "return CalculationDataErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(url)(badResponse)
      await(result) shouldBe CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

  "CalculationDataConnector.getLatestCalculation" should {

    val successResponse = HttpResponse(Status.OK, Some(testCalculationInputJson))

    lazy val url = TestCalculationDataConnector.getLatestCalculationUrl(testNino, testTaxYear.toString)
    def result: Future[CalculationResponseModel] = TestCalculationDataConnector.getLatestCalculation(testNino, testTaxYear)

    "return a CalculationModel with JSON in case of success" in {
      setupMockHttpGet(url)(successResponse)
      await(result) shouldBe testCalcModel
    }

    "return a CalculationErrorModel in case of failure" in {
      setupMockHttpGet(url)(badResponse)
      await(result) shouldBe CalculationErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return a CalculationErrorModel when bad JSON is received" in {
      setupMockHttpGet(url)(successResponseBadJson)
      await(result) shouldBe CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing calculation model response")
    }

    "return a CalculationErrorModel in case of failed GET request" in {
      setupMockFailedHttpGet(url)(badResponse)
      await(result) shouldBe CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
    }
  }
}
