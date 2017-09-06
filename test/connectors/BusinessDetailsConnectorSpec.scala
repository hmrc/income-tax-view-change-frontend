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
import models.{BusinessDetailsErrorModel, BusinessListResponseModel, NoBusinessIncomeDetails, PropertyDetailsErrorModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future


class BusinessDetailsConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, responseJson = Some(Json.toJson(businessesSuccessResponse)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))
  val successNoBusiness = HttpResponse(Status.OK, responseJson = Some(Json.parse(businessSuccessEmptyResponse)))

  object TestBusinessDetailsConnector extends BusinessDetailsConnector(mockHttpGet)

  "BusinessDetailsConnector.getBusinessList" should {

    lazy val testUrl = TestBusinessDetailsConnector.getBusinessListUrl(testNino)
    def result: Future[BusinessListResponseModel] = TestBusinessDetailsConnector.getBusinessList(testNino)

    "return a BusinessDetailsModel with JSON in case of success" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe businessesSuccessModel
    }

    "return a NoBusinessIncomeDetails case object when no business are returned" in {
      setupMockHttpGet(testUrl)(successNoBusiness)
      await(result) shouldBe NoBusinessIncomeDetails
    }

    "return BusinessListError model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe BusinessDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return BusinessListError model when bad JSON is received" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Business Details Response")
    }

    "return BusinessDetailsErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}
