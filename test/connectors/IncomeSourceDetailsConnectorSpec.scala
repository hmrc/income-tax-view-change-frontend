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

import mocks.MockHttp
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse
import utils.TestSupport
import assets.TestConstants.NewIncomeSourceDetails._
import assets.TestConstants.testNino
import models.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}

import scala.concurrent.Future

class IncomeSourceDetailsConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(incomeSourceDetails)))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestIncomeSourceDetailsConnector extends IncomeSourceDetailsConnector(mockHttpGet, frontendAppConfig)

  "IncomeSourceDetailsConnector.getIncomeSources" should {

    lazy val testUrl = TestIncomeSourceDetailsConnector.getIncomeSourcesUrl(testNino)
    def result: Future[IncomeSourceDetailsResponse] = TestIncomeSourceDetailsConnector.getIncomeSources(testNino)

    "return an IncomeSourceDetailsModel when successful JSON is received" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe incomeSourceDetails
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe IncomeSourceDetailsError
    }

    "return IncomeSourceDetailsError model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe IncomeSourceDetailsError
    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe IncomeSourceDetailsError
    }
  }

}
