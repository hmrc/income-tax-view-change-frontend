/*
 * Copyright 2019 HM Revenue & Customs
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
import models.calculation.{CalculationDataErrorModel, CalculationDataResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

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
      await(result) shouldBe CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, unknown error")
    }
  }
}
