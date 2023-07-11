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
import models.createIncomeSource.{CreateBusinessErrorResponse, AddIncomeSourceResponse}
import play.api.libs.json.{Format, Json}
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IncomeSourcesSpec extends TestSupport with MockHttp with IncomeSourcesDataHelper {

  object UnderTestConnector$CreateBusiness extends CreateBusinessIncomeSourcesConnector(mockHttpGet, appConfig)

  case class UnknownError(error: String)

  object UnknownError {
    implicit val format: Format[UnknownError] = Json.format
  }

  "call create method" should {

    "return success" when {
      "return expected status code OK" in {
        val expectedIncomeSourceId = "123123123"
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector$CreateBusiness.addBusinessDetailsUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson(
          List(AddIncomeSourceResponse(expectedIncomeSourceId))),
          headers = Map.empty)

        val testBody = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )

        setupMockHttpPost(url, testBody)(response = expectedResponse)

        val result: Future[Either[CreateBusinessErrorResponse, List[AddIncomeSourceResponse]]] = UnderTestConnector$CreateBusiness.create(mtdId, addBusinessDetailsRequestObject)

        result.futureValue shouldBe Right(List(AddIncomeSourceResponse(expectedIncomeSourceId)))
      }


      "return expected status code OK - but invalid json" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector$CreateBusiness.addBusinessDetailsUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.OK, json = Json.toJson("Error message"), headers = Map.empty)

        val testBody = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )
        setupMockHttpPost(url, testBody)(response = expectedResponse)
        val result: Future[Either[CreateBusinessErrorResponse, List[AddIncomeSourceResponse]]] = UnderTestConnector$CreateBusiness.create(mtdId, addBusinessDetailsRequestObject)
        result.futureValue shouldBe Left(CreateBusinessErrorResponse(Status.OK, s"Not valid json: \"Error message\""))
      }
    }

    "return failure" when {
      "return error" in {
        val mtdId = individualUser.mtditid

        val url = UnderTestConnector$CreateBusiness.addBusinessDetailsUrl(mtdId)
        val expectedResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(
          CreateBusinessErrorResponse(status = 500, "Some error message")),
          headers = Map.empty)


        val testBody2 = Json.parse(
          """
            |{"businessDetails":[{"accountingPeriodStartDate":"01-02-2023","accountingPeriodEndDate":"","tradingName":"","addressDetails":{"addressLine1":"tests test","addressLine2":"","countryCode":"UK","postalCode":""},"tradingStartDate":"","cashOrAccrualsFlag":"","cessationDate":""}]}
        """.stripMargin
        )
        setupMockHttpPost(url, testBody2)(response = expectedResponse)

        val result: Future[Either[CreateBusinessErrorResponse, List[AddIncomeSourceResponse]]] = UnderTestConnector$CreateBusiness.create(mtdId, addBusinessDetailsRequestObject)

        result.futureValue match {
          case Left(CreateBusinessErrorResponse(500, _)) =>
            succeed
          case _ =>
            fail("We expect error code 500 to be returned")
        }
      }
    }

  }

}
