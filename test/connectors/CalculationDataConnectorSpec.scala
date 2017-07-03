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

import mocks.MockHttp
import models.{BusinessListResponseModel, CalculationDataErrorModel, CalculationDataResponseModel}
import utils.TestSupport
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import assets.TestConstants._
import assets.TestConstants.CalcBreakdown._

import scala.concurrent.Future

class CalculationDataConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(calculationDataSuccessModel)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{\"incomeTaxYTD\":\"somethingBad\"}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestCalculationDataConnector extends CalculationDataConnector(mockHttpGet)

  "BusinessObligationDataConnector.getObligationData" should {

    lazy val url = TestCalculationDataConnector.getCalculationDataUrl(testNino, testTaxCalculationId)
    def result: Future[CalculationDataResponseModel] = TestCalculationDataConnector.getCalculationData(testNino, testTaxCalculationId)

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(url)(successResponse)
      await(result) shouldBe calculationDataSuccessModel
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(url)(badResponse)
      await(result) shouldBe CalculationDataErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return CalculationDataError model when bad JSON is received" in {
      setupMockHttpGet(url)(successResponseBadJson)
      await(result) shouldBe CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Obligation Data Response.")
    }

    "return LastTaxCalculationError model in case of future failed scenario" in {
      setupMockFailedHttpGet(url)(badResponse)
      await(result) shouldBe CalculationDataErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $url.")
    }
  }
}
