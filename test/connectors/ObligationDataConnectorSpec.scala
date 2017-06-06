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

import models.{ErrorResponse, SuccessResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.TestSupport


class ObligationDataConnectorSpec extends TestSupport with MockHttp {

  implicit val hc = HeaderCarrier()

  val testNino = "AB123456C"
  val testSelfEmploymentId = "5318008"

  val successResponse = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestObligationDataConnector extends ObligationDataConnector(mockHttpGet)

  "ObligationDataConnector.getBusinessList" should {

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(TestObligationDataConnector.getBusinessListUrl(testNino))(successResponse)
      val result = TestObligationDataConnector.getBusinessList(testNino)
      await(result) shouldBe SuccessResponse(Json.parse("{}"))
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(TestObligationDataConnector.getBusinessListUrl(testNino))(badResponse)
      val result = TestObligationDataConnector.getBusinessList(testNino)
      await(result) shouldBe ErrorResponse(Status.BAD_REQUEST, "Error Message")
    }
  }

  "ObligationDataConnector.getObligationData" should {

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(TestObligationDataConnector.getObligationDataUrl(testNino, testSelfEmploymentId))(successResponse)
      val result = TestObligationDataConnector.getObligationData(testNino, testSelfEmploymentId)
      await(result) shouldBe SuccessResponse(Json.parse("{}"))
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(TestObligationDataConnector.getObligationDataUrl(testNino, testSelfEmploymentId))(badResponse)
      val result = TestObligationDataConnector.getObligationData(testNino, testSelfEmploymentId)
      await(result) shouldBe ErrorResponse(Status.BAD_REQUEST, "Error Message")
    }
  }
}
