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
import mocks.MockHttpV2
import models.createIncomeSource.CreateIncomeSourceErrorResponse.format
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class CreateIncomeSourceConnectorSpec extends TestSupport with MockHttpV2 with IncomeSourcesDataHelper {

  object UnderTestConnector extends CreateIncomeSourceConnector(mockHttpClientV2, appConfig)

  "call create business" should {

    "return success" when {
      "return expected status code OK" in {
        val expectedIncomeSourceId = "123123123"
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
          List(CreateIncomeSourceResponse(expectedIncomeSourceId))),
          headers = Map.empty)

        setupMockHttpV2Post(url)(expectedResponse)

        val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] =
          UnderTestConnector.createBusiness(createBusinessDetailsRequestObject)

        result.futureValue shouldBe Right(List(CreateIncomeSourceResponse(expectedIncomeSourceId)))
      }


      "return expected status code OK - but invalid json" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson("Error message"), headers = Map.empty)

        setupMockHttpV2Post(url)(expectedResponse)
        val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = UnderTestConnector.createBusiness(createBusinessDetailsRequestObject)
        result.futureValue shouldBe Left(CreateIncomeSourceErrorResponse(Status.OK, s"Not valid json: \"Error message\""))
      }
    }

    "return failure" when {
      "return error" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateIncomeSourceErrorResponse(status = 500, "Some error message")),
          headers = Map.empty)

        setupMockHttpV2Post(url)(expectedResponse)

        val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = UnderTestConnector.createBusiness(createBusinessDetailsRequestObject)

        result.futureValue match {
          case Left(CreateIncomeSourceErrorResponse(500, _)) =>
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

      val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
      val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
        List(CreateIncomeSourceResponse(expectedIncomeSourceId))),
        headers = Map.empty)

      setupMockHttpV2Post(url)(expectedResponse)

      val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] =
        UnderTestConnector.createForeignProperty(createForeignPropertyRequestObject)

      result.futureValue shouldBe Right(List(CreateIncomeSourceResponse(expectedIncomeSourceId)))
    }

    "return failure" when {
      "return status error 500" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateIncomeSourceErrorResponse(status = Status.INTERNAL_SERVER_ERROR, "Some error message")),
          headers = Map.empty)

        setupMockHttpV2Post(url)(expectedResponse)

        val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = UnderTestConnector.createForeignProperty(createForeignPropertyRequestObject)

        result.futureValue match {
          case Left(CreateIncomeSourceErrorResponse(Status.INTERNAL_SERVER_ERROR, _)) =>
            succeed
          case _ =>
            fail("We expect error code 500 to be returned")
        }
      }

    }
  }

  "call create UK property" should {
    "return expected status code OK" in {
      val expectedIncomeSourceId = "5656"
      val mtdId = individualUser.mtditid

      val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
      val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
        List(CreateIncomeSourceResponse(expectedIncomeSourceId))),
        headers = Map.empty)

      setupMockHttpV2Post(url)(expectedResponse)

      val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] =
        UnderTestConnector.createUKProperty(createUKPropertyRequestObject)

      result.futureValue shouldBe Right(List(CreateIncomeSourceResponse(expectedIncomeSourceId)))
    }

    "return failure" when {
      "return status error 500" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector.createBusinessIncomeSourcesUrl()
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateIncomeSourceErrorResponse(status = Status.INTERNAL_SERVER_ERROR, "Some error message")),
          headers = Map.empty)

        setupMockHttpV2Post(url)(expectedResponse)

        val result: Future[Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]] = UnderTestConnector.createUKProperty(createUKPropertyRequestObject)

        result.futureValue match {
          case Left(CreateIncomeSourceErrorResponse(Status.INTERNAL_SERVER_ERROR, _)) =>
            succeed
          case _ =>
            fail("We expect error code 500 to be returned")
        }
      }

    }
  }
}