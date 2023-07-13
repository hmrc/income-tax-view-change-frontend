/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.helpers.IncomeSourcesDataHelper
import mocks.MockHttp
import models.createIncomeSource.CreateIncomeSourceErrorResponse.format
import models.createIncomeSource.{CreateIncomeSourcesErrorResponse, CreateIncomeSourcesResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class CreateIncomeSourcesConnectorSpec extends TestSupport with MockHttp with IncomeSourcesDataHelper {

  object UnderTestConnector extends CreateIncomeSourceConnector(mockHttpGet, appConfig)

  "call create business" should {

    "return success" when {
      "return expected status code OK" in {
        val expectedIncomeSourceId = "123123123"
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
          List(CreateIncomeSourcesResponse(expectedIncomeSourceId))),
          headers = Map.empty)

        val testBody = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )

        setupMockHttpPost(url, testBody)(response = expectedResponse)

        val result: Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] =
          UnderTestConnector.createBusiness(mtdId, createBusinessDetailsRequestObject)

        result.futureValue shouldBe Right(List(CreateIncomeSourcesResponse(expectedIncomeSourceId)))
      }


      "return expected status code OK - but invalid json" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson("Error message"), headers = Map.empty)

        val testBody = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )
        setupMockHttpPost(url, testBody)(response = expectedResponse)
        val result: Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = UnderTestConnector.createBusiness(mtdId, createBusinessDetailsRequestObject)
        result.futureValue shouldBe Left(CreateIncomeSourcesErrorResponse(Status.OK, s"Not valid json: \"Error message\""))
      }
    }

    "return failure" when {
      "return error" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateIncomeSourcesErrorResponse(status = 500, "Some error message")),
          headers = Map.empty)


        val testBody2 = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )
        setupMockHttpPost(url, testBody2)(response = expectedResponse)

        val result: Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = UnderTestConnector.createBusiness(mtdId, createBusinessDetailsRequestObject)

        result.futureValue match {
          case Left(CreateIncomeSourcesErrorResponse(500, _)) =>
            succeed
          case _ =>
            fail("We expect error code 500 to be returned")
        }
      }
    }

  }

  "call create foreign property" should {
    "return expected status code OK" in {
      val expectedIncomeSourceId = "5656"
      val mtdId = individualUser.mtditid

      val url = UnderTestConnector.createBusinessIncomeSourcesUrl(mtdId)
      val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
        List(CreateIncomeSourcesResponse(expectedIncomeSourceId))),
        headers = Map.empty)

      val testBody = Json.parse(
        """
          |{"foreignPropertyDetails": {"tradingStartDate":"2011-01-01","cashOrAccrualsFlag":"CASH","startDate":"2011-01-01"}}
        """.stripMargin)

      setupMockHttpPost(url, testBody)(response = expectedResponse)

      val result: Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] =
        UnderTestConnector.createForeignProperty(mtdId, createForeignPropertyRequestObject)

      result.futureValue shouldBe Right(List(CreateIncomeSourcesResponse(expectedIncomeSourceId)))
    }

    "return failure" when {
      "return status error 500" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateIncomeSourcesErrorResponse(status = Status.INTERNAL_SERVER_ERROR, "Some error message")),
          headers = Map.empty)

        val testBody = Json.parse(
          """
            |{"foreignPropertyDetails": {"tradingStartDate":"2011-01-01","cashOrAccrualsFlag":"CASH","startDate":"2011-01-01"}}
        """.stripMargin)

        setupMockHttpPost(url, testBody)(response = expectedResponse)

        val result: Future[Either[CreateIncomeSourcesErrorResponse, List[CreateIncomeSourcesResponse]]] = UnderTestConnector.createForeignProperty(mtdId, createForeignPropertyRequestObject)

        result.futureValue match {
          case Left(CreateIncomeSourcesErrorResponse(Status.INTERNAL_SERVER_ERROR, _)) =>
            succeed
          case _ =>
            fail("We expect error code 500 to be returned")
        }
      }

    }
  }

}
