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
import models.{ErrorResponse, SuccessResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.TestSupport


class EstimatedTaxLiabilityConnectorSpec extends TestSupport with MockHttp {

  implicit val hc = HeaderCarrier()

  val testMtditid = "XAITSA0000123456"

  val successResponse = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestEstimatedTaxLiabilityConnector extends EstimatedTaxLiabilityConnector(mockHttpGet)

  "EstimatedTaxLiabilityConnector.getEstimatedTaxLiability" should {

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiabilityUrl(testMtditid))(successResponse)
      val result = TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(testMtditid)
      await(result) shouldBe SuccessResponse(Json.parse("{}"))
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiabilityUrl(testMtditid))(badResponse)
      val result = TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(testMtditid)
      await(result) shouldBe ErrorResponse(Status.BAD_REQUEST, "Error Message")
    }
  }
}
