/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants.{testMtditid, testSaUtr, testTaxYear}
import testConstants.CitizenDetailsTestConstants._
import connectors.agent.CitizenDetailsConnector
import mocks.MockHttp
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel, CitizenDetailsResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(status = Status.OK, json = testValidCitizenDetailsModelJson, headers = Map.empty)
  val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidCitizenDetailsJson, headers = Map.empty)
  val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")


  object TestCitizenDetailsConnector extends CitizenDetailsConnector(mockHttpGet, appConfig)

  "CitizenDetailsConnector.getCitizenDetailsBySaUtrUrl" should {

    lazy val testUrl = TestCitizenDetailsConnector.getCitizenDetailsBySaUtrUrl(testSaUtr)

    def result: Future[CitizenDetailsResponseModel] = TestCitizenDetailsConnector.getCitizenDetailsBySaUtr(testSaUtr)

    "return a CitizenDetailsModel with JSON in case of success" in {
      setupMockHttpGet(testUrl)(successResponse)
      result.futureValue shouldBe testValidCitizenDetailsModel
    }

    "return CitizenDetailsErrorModel when bad Json is recieved" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      result.futureValue shouldBe CitizenDetailsModel(None, None, None)
    }

    "return CitizenDetailErrorModel when bad request recieved" in {
      setupMockHttpGet(testUrl)(badResponse)
      result.futureValue shouldBe CitizenDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return CitizenDetailErrorModel when GET fails" in {
      setupMockFailedHttpGet(testUrl)
      result.futureValue shouldBe CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed, unknown error")
    }

  }

}
