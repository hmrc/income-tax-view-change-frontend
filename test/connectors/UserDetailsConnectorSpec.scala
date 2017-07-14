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

import assets.TestConstants._
import mocks.MockHttp
import models._
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future


class UserDetailsConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, responseJson = Some(Json.toJson(testUserDetails)))
  val badJsonResponse = HttpResponse(Status.OK, responseJson = Some(Json.toJson("{}")))
  val badResponse = HttpResponse(Status.INTERNAL_SERVER_ERROR, responseString = Some("Error Message"))

  object TestUserDetailsConnector extends UserDetailsConnector(mockHttpGet)

  "The UserDetailsConnector.getUserDetails() method" should {

    def result: Future[UserDetailsResponseModel] = TestUserDetailsConnector.getUserDetails(testUserDetailsUrl)

    "return a User Details Model in case of success" in {
      setupMockHttpGet(testUserDetailsUrl)(successResponse)
      await(result) shouldBe testUserDetails
    }

    "return User Details Error model in case of JSON parse error" in {
      setupMockHttpGet(testUserDetailsUrl)(badJsonResponse)
      await(result) shouldBe UserDetailsError
    }

    "return User Details Error model in case of failure" in {
      setupMockHttpGet(testUserDetailsUrl)(badResponse)
      await(result) shouldBe UserDetailsError
    }

    "return PropertyDetailsErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUserDetailsUrl)(successResponse)
      await(result) shouldBe UserDetailsError
    }
  }
}
