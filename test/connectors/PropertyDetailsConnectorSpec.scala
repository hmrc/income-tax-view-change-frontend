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


class PropertyDetailsConnectorSpec extends TestSupport with MockHttp {

  val propertySuccessModel = PropertyDetailsModel(AccountingPeriodModel("2017-04-06", "2018-04-05"))
  val successResponse = HttpResponse(Status.OK, responseJson = Some(Json.toJson("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestPropertyDetailsConnector extends PropertyDetailsConnector(mockHttpGet)

  "The PropertyDetailsConnector.getPropertyDetails method" should {

    lazy val testUrl = TestPropertyDetailsConnector.getPropertyDetailsUrl(testNino)
    def result: Future[PropertyDetailsResponseModel] = TestPropertyDetailsConnector.getPropertyDetails(testNino)

    "return a PropertyIncomeModel with JSON in case of success" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe propertySuccessModel
    }

    "return PropertyDetailsErrorModel model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe PropertyDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return PropertyDetailsErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $testUrl.")
    }
  }
}
