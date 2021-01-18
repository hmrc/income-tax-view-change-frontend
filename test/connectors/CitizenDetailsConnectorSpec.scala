/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.BaseTestConstants.{testMtditid, testSaUtr, testTaxYear}
import assets.CitizenDetailsTestConstants._
import mocks.MockHttp
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel, CitizenDetailsResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends TestSupport with MockHttp{

  val successResponse = HttpResponse(Status.OK, Some(testValidCitizenDetailsModelJson))
  val successResponseBadJson = HttpResponse(Status.OK, Some(testInvalidCitizenDetailsJson))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))


  object TestCitizenDetailsConnector extends CitizenDetailsConnector(mockHttpGet, appConfig)

  "CitizenDetailsConnector.getCitizenDetailsBySaUtrUrl" should {

    lazy val testUrl = TestCitizenDetailsConnector.getCitizenDetailsBySaUtrUrl(testSaUtr)
    def result: Future[CitizenDetailsResponseModel] = TestCitizenDetailsConnector.getCitizenDetailsBySaUtr(testSaUtr)

    "return a CitizenDetailsModel with JSON in case of success" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe testValidCitizenDetailsModel
    }

    "return CitizenDetailsErrorModel when bad Json is recieved" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe CitizenDetailsModel(None, None, None)
    }

    "return CitizenDetailErrorModel when bad request recieved" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe CitizenDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return CitizenDetailErrorModel when GET fails" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe CitizenDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed, unknown error")
    }

  }

}
