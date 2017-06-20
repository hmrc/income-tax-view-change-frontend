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

import assets.TestConstants.BusinessDetails._
import assets.TestConstants._
import mocks.MockHttp
import models.BusinessListError
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestSupport


class BusinessDetailsConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, responseJson = Some(Json.toJson(businessesSuccessModel)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestBusinessDetailsConnector extends BusinessDetailsConnector(mockHttpGet)

  "BusinessDetailsConnector.getBusinessList" should {

    "return a BusinessListModel with JSON in case of success" in {
      setupMockHttpGet(TestBusinessDetailsConnector.getBusinessListUrl(testNino))(successResponse)
      val result = TestBusinessDetailsConnector.getBusinessList(testNino)
      await(result) shouldBe businessesSuccessModel
    }

    "return BusinessListError model in case of failure" in {
      setupMockHttpGet(TestBusinessDetailsConnector.getBusinessListUrl(testNino))(badResponse)
      val result = TestBusinessDetailsConnector.getBusinessList(testNino)
      await(result) shouldBe BusinessListError(Status.BAD_REQUEST, "Error Message")
    }

    "return BusinessListError model when bad JSON is received" in {
      setupMockHttpGet(TestBusinessDetailsConnector.getBusinessListUrl(testNino))(successResponseBadJson)
      val result = TestBusinessDetailsConnector.getBusinessList(testNino)
      await(result) shouldBe BusinessListError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Business Details Response.")
    }
  }
}
