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

import assets.TestConstants.Estimates.successModel
import assets.TestConstants._
import mocks.MockHttp
import models.{ErrorResponse, EstimatedTaxLiabilityError}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.TestSupport

class EstimatedTaxLiabilityConnectorSpec extends TestSupport with MockHttp {

  implicit val hc = HeaderCarrier()

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(successModel)))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestEstimatedTaxLiabilityConnector extends EstimatedTaxLiabilityConnector(mockHttpGet)

  "EstimatedTaxLiabilityConnector.getEstimatedTaxLiability" should {

    "return a EstimatedTaxLiability model when successful JSON is received" in {
      setupMockHttpGet(TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiabilityUrl(testMtditid))(successResponse)
      val result = TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(testMtditid)
      await(result) shouldBe successModel
    }

    "return EstimatedTaxLiabilityError model in case of bad/malformed JSON response" in {
      setupMockHttpGet(TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiabilityUrl(testMtditid))(successResponseBadJson)
      val result = TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(testMtditid)
      await(result) shouldBe EstimatedTaxLiabilityError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Estimated Tax Liability Response.")
    }

    "return EstimatedTaxLiabilityError model in case of failure" in {
      setupMockHttpGet(TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiabilityUrl(testMtditid))(badResponse)
      val result = TestEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(testMtditid)
      await(result) shouldBe EstimatedTaxLiabilityError(Status.BAD_REQUEST, "Error Message")
    }
  }
}
