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
import assets.NinoLookupTestConstants._
import mocks.MockHttp
import models.core.{NinoResponse, NinoResponseError}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future

class NinoLookupConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(testNinoModelJson))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestNinoLookupConnector extends NinoLookupConnector(mockHttpGet, frontendAppConfig)

  "NinoLookupConnector.getNino" should {

    lazy val testUrl = TestNinoLookupConnector.getNinoLookupUrl(testMtditid)
    def result: Future[NinoResponse] = TestNinoLookupConnector.getNino(testMtditid)

    "return a Nino model when successful JSON is received" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe testNinoModel
    }

    "return NinoResponseError model in case of bad/malformed JSON response" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
    }

    "return NinoResponseError model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe NinoResponseError(Status.BAD_REQUEST, "Error Message")
    }

    "return NinoResponseError model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
