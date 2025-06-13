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

import mocks.MockHttpV2
import models.liabilitycalculation._
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate
import scala.concurrent.Future

class IncomeTaxCalculationConnectorSpec extends TestSupport with MockHttpV2 {

  class GetCalculationResponseTest(nino: String, taxYear: String, response: HttpResponse) {
    val connector = new IncomeTaxCalculationConnector(mockHttpClientV2, appConfig)

    setupMockHttpV2Get(s"${connector.getCalculationResponseUrl(nino)}?taxYear=$taxYear")(response)
  }

  class GetCalculationResponseByCalcIdTest(nino: String, calcId: String, response: HttpResponse, taxYear: Int) {
    val connector = new IncomeTaxCalculationConnector(mockHttpClientV2, appConfig)

    setupMockHttpV2Get(s"${connector.getCalculationResponseByCalcIdUrl(nino, calcId)}?taxYear=${taxYear.toString}")(response)
  }

  val mtditid = "XAIT0000123456"
  val nino: String = "AA123456A"
  val taxYear: String = "2019"
  val taxYearAsInt: Int = taxYear.toInt
  val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  val calculation: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
    messages = None,
    metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), "IY", "customerRequest", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1)),
    calculation = None)
  val calculationJson: JsObject = Json.obj("inputs" -> Json.obj("personalInformation" ->
    Json.obj("taxRegime" -> "UK")),
    "metadata" -> Json.obj("calculationTimestamp" -> "2019-02-15T09:35:15.094Z", "calculationType" -> "IY",
    "calculationReason" -> "customerRequest", "periodFrom" -> "2022-01-01", "periodTo" -> "2023-01-01"))

  "IncomeTaxCalculationConnector.getCalculationResponse" should {
    "return a calculation" when {
      "receiving an OK with valid Calculation json" in new GetCalculationResponseTest(nino, taxYear, HttpResponse(status = OK,
        json = calculationJson, headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponse(mtditid, nino, taxYear)

        result.futureValue shouldBe calculation
      }
    }
    "return an error" when {
      "receiving a 500+ response" in new GetCalculationResponseTest(nino, taxYear, HttpResponse(
        status = INTERNAL_SERVER_ERROR, json = Json.toJson("Error message"), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponse(mtditid, nino, taxYear)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, """"Error message"""")
      }
      "receiving a 499- response" in new GetCalculationResponseTest(nino, taxYear, HttpResponse(
        status = 499, json = Json.toJson("Error message"), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponse(mtditid, nino, taxYear)

        result.futureValue shouldBe LiabilityCalculationError(499, """"Error message"""")
      }
      "receiving OK with invalid json" in new GetCalculationResponseTest(
        nino, taxYear, HttpResponse(status = OK, json = Json.toJson(""), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponse(mtditid, nino, taxYear)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
      }
    }
  }
  "IncomeTaxCalculationConnector.getCalculationResponseByCalcId" should {
    "return a calculation" when {
      "receiving an OK with valid Calculation json" in new GetCalculationResponseByCalcIdTest(nino, calculationId, HttpResponse(status = OK,
        json = calculationJson, headers = Map.empty), taxYearAsInt) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYearAsInt)

        result.futureValue shouldBe calculation
      }
    }
    "return an error" when {
      "receiving a 500+ response" in new GetCalculationResponseByCalcIdTest(nino, calculationId, HttpResponse(
        status = INTERNAL_SERVER_ERROR, json = Json.toJson("Error message"), headers = Map.empty), taxYearAsInt) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYearAsInt)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, """"Error message"""")
      }
      "receiving a 499- response" in new GetCalculationResponseByCalcIdTest(nino, calculationId, HttpResponse(
        status = 499, json = Json.toJson("Error message"), headers = Map.empty), taxYearAsInt) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYearAsInt)

        result.futureValue shouldBe LiabilityCalculationError(499, """"Error message"""")
      }
      "receiving OK with invalid json" in new GetCalculationResponseByCalcIdTest(
        nino, calculationId, HttpResponse(status = OK, json = Json.toJson(""), headers = Map.empty), taxYearAsInt) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYearAsInt)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
      }
    }
  }

}
