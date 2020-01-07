/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDateTime

import mocks.MockHttp
import models.calculation.{CalculationErrorModel, CalculationItem, CalculationResponseModel, ListCalculationItems}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IndividualCalculationsConnectorSpec extends TestSupport with MockHttp {

  class TestConnector(nino: String, taxYear: String, response: HttpResponse) {
    val connector = new IndividualCalculationsConnector(mockHttpGet, frontendAppConfig)

    setupMockHttpGetWithParams(connector.url(nino), Seq(("taxYear", taxYear)))(response)
  }

  "IndividualCalculationsConnector .getLatestCalculationId" should {

    "return the id of the most recent calculation" when {

      "receiving an OK with only one valid data item" in new TestConnector("AA123456A", "2019/20",
        HttpResponse(OK, Some(Json.toJson(ListCalculationItems(Seq(CalculationItem("testId", LocalDateTime.now()))))))) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Right("testId")
      }

      "receiving an OK with multiple valid data items" in new TestConnector("AA123456A", "2019/20",
        HttpResponse(OK, Some(Json.toJson(ListCalculationItems(Seq(
          CalculationItem("correctId", LocalDateTime.now()),
          CalculationItem("invalidId", LocalDateTime.now().minusSeconds(1))
        )))))) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Right("correctId")
      }
    }

    "return a NOT FOUND calculation error" when {

      "receiving a not found response" in new TestConnector("AA123456A", "2019/20", HttpResponse(NOT_FOUND)) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Left(CalculationErrorModel(NOT_FOUND, "No calculation found for tax year 2019/20"))
      }
    }

    "return an INTERNAL_SERVER_ERROR calculation error" when {

      "receiving a 500+ response" in new TestConnector("AA123456A", "2019/20", HttpResponse(INTERNAL_SERVER_ERROR, Some(Json.toJson("Error message")))) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Left(CalculationErrorModel(INTERNAL_SERVER_ERROR, """"Error message""""))
      }

      "receiving a 499- response" in new TestConnector("AA123456A", "2019/20", HttpResponse(499, Some(Json.toJson("Error message")))) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Left(CalculationErrorModel(499, """"Error message""""))
      }

      "receiving an OK with invalid json" in new TestConnector("AA123456A", "2019/20", HttpResponse(OK, Some(Json.toJson("")))) {
        val result: Future[Either[CalculationResponseModel, String]] = connector.getLatestCalculationId("AA123456A", "2019/20")

        await(result) shouldBe Left(CalculationErrorModel(INTERNAL_SERVER_ERROR,
          "Json validation error parsing calculation list response"))
      }
    }
  }
}
