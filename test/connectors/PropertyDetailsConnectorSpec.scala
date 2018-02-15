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

import assets.TestConstants._
import mocks.MockHttp
import models._
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.TestSupport

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse


class PropertyDetailsConnectorSpec extends TestSupport with MockHttp {

  val propertySuccessModel = PropertyDetailsModel(AccountingPeriodModel("2017-04-06", "2018-04-05"))
  val featureDisabledSuccessResponse = HttpResponse(Status.OK, responseJson = Some(Json.obj()))
  val featureEnabledSuccessResponse = HttpResponse(Status.OK, responseJson = Some(Json.obj(
    "accountingPeriod" -> Json.obj(
      "start" -> "2017-04-06",
      "end" -> "2018-04-05"
    )
  )))
  val badJsonResponse = HttpResponse(Status.OK, responseJson = Some(Json.obj("foo" -> "bar")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))
  val notFound = HttpResponse(Status.NOT_FOUND)

  object TestPropertyDetailsConnector extends PropertyDetailsConnector(mockHttpGet, frontendAppConfig)

  "The PropertyDetailsConnector.getPropertyDetails method" when {

    lazy val testUrl = TestPropertyDetailsConnector.getPropertyDetailsUrl(testNino)
    def result: Future[PropertyDetailsResponseModel] = TestPropertyDetailsConnector.getPropertyDetails(testNino)

    "the feature.propertyDetailsEnabled is disabled" should {

      "return a dummy PropertyIncomeModel with JSON in case of success" in {
        frontendAppConfig.features.propertyDetailsEnabled(false)
        setupMockHttpGet(testUrl)(featureDisabledSuccessResponse)
        await(result) shouldBe propertySuccessModel
      }

      "return a NoPropertyIncomeDetails response when a NOT_FOUND is returned" in {
        setupMockHttpGet(testUrl)(notFound)
        await(result) shouldBe NoPropertyIncomeDetails
      }

      "return PropertyDetailsErrorModel model in case of failure" in {
        setupMockHttpGet(testUrl)(badResponse)
        await(result) shouldBe PropertyDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return PropertyDetailsErrorModel model in case of future failed scenario" in {
        setupMockFailedHttpGet(testUrl)(badResponse)
        await(result) shouldBe PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
      }
    }

    "the feature.propertyDetailsEnabled is enabled" should {

      "return a PropertyIncomeModel with JSON in case of success" in {
        frontendAppConfig.features.propertyDetailsEnabled(true)
        setupMockHttpGet(testUrl)(featureEnabledSuccessResponse)
        await(result) shouldBe propertySuccessModel
      }

      "return a NoPropertyIncomeDetails response when a NOT_FOUND is returned" in {
        setupMockHttpGet(testUrl)(notFound)
        await(result) shouldBe NoPropertyIncomeDetails
      }

      "return PropertyDetailsErrorModel model in case of bad Json" in {
        setupMockHttpGet(testUrl)(badJsonResponse)
        await(result) shouldBe PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Failed to parse JSON body of response to PropertyDetailsModel.")
      }

      "return PropertyDetailsErrorModel model in case of failure" in {
        setupMockHttpGet(testUrl)(badResponse)
        await(result) shouldBe PropertyDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return PropertyDetailsErrorModel model in case of future failed scenario" in {
        setupMockFailedHttpGet(testUrl)(badResponse)
        await(result) shouldBe PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
      }
    }
  }
}
