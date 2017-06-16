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
import models.ObligationsErrorModel
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.TestSupport
import assets.TestConstants._
import assets.TestConstants.Obligations._


class PropertyDataConnectorSpec extends TestSupport with MockHttp {

  implicit val hc = HeaderCarrier()

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(obligationsDataResponse)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestPropertyDataConnector extends PropertyDataConnector(mockHttpGet)

  "PropertyDataConnector.getPropertyData" should {

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(TestPropertyDataConnector.getPropertyDataUrl(testNino))(successResponse)
      val result = TestPropertyDataConnector.getPropertyData(testNino)
      await(result) shouldBe obligationsDataResponse
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(TestPropertyDataConnector.getPropertyDataUrl(testNino))(badResponse)
      val result = TestPropertyDataConnector.getPropertyData(testNino)
      await(result) shouldBe ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ObligationsErrorModel when bad JSON is received" in {
      setupMockHttpGet(TestPropertyDataConnector.getPropertyDataUrl(testNino))(successResponseBadJson)
      val result = TestPropertyDataConnector.getPropertyData(testNino)
      await(result) shouldBe ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Property Obligation Data Response.")
    }
  }
}
