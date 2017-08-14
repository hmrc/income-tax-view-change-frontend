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

import assets.TestConstants.Obligations._
import assets.TestConstants._
import mocks.MockHttp
import models.{LastTaxCalculationError, ObligationsErrorModel, ObligationsResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future


class PropertyObligationDataConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(obligationsDataSuccessModel)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestPropertyObligationDataConnector extends PropertyObligationDataConnector(mockHttpGet)

  "PropertyObligationDataConnector.getPropertyData" should {

    lazy val testUrl = TestPropertyObligationDataConnector.getPropertyDataUrl(testNino)
    def result: Future[ObligationsResponseModel] = TestPropertyObligationDataConnector.getPropertyObligationData(testNino)

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe obligationsDataSuccessModel
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ObligationsErrorModel when bad JSON is received" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Property Obligation Data Response")
    }

    "return ObligationsErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
