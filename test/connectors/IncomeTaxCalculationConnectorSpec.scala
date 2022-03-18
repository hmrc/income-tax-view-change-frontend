/*
 * Copyright 2022 HM Revenue & Customs
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

import mocks.MockHttp
import models.liabilitycalculation.{Inputs, LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel, Metadata, PersonalInformation}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IncomeTaxCalculationConnectorSpec extends TestSupport with MockHttp {

  class GetCalculationResponseTest(nino: String, taxYear: String, response: HttpResponse) {
    val connector = new IncomeTaxCalculationConnector(mockHttpGet, appConfig)

    setupMockHttpGetWithParams(connector.getCalculationResponseUrl(nino), Seq(("taxYear", taxYear)))(response)
  }

  class GetCalculationResponseByCalcIdTest(nino: String, calcId: String, response: HttpResponse) {
    val connector = new IncomeTaxCalculationConnector(mockHttpGet, appConfig)

    setupMockHttpGet(connector.getCalculationResponseByCalcIdUrl(nino, calcId))(response)
  }

  val mtditid = "XAIT0000123456"
  val nino: String = "AA123456A"
  val taxYear: String = "2019"
  val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  val calculation: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
    messages = None,
    metadata = Metadata(Some("2019-02-15T09:35:15.094Z"), Some(false)),
    calculation = None)
  val calculationJson: JsObject = Json.obj("inputs" -> Json.obj("personalInformation" ->
    Json.obj("taxRegime" -> "UK")),
    "metadata" -> Json.obj("calculationTimestamp" -> "2019-02-15T09:35:15.094Z", "crystallised" -> false))

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
        json = calculationJson, headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId)

        result.futureValue shouldBe calculation
      }
    }
    "return an error" when {
      "receiving a 500+ response" in new GetCalculationResponseByCalcIdTest(nino, calculationId, HttpResponse(
        status = INTERNAL_SERVER_ERROR, json = Json.toJson("Error message"), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, """"Error message"""")
      }
      "receiving a 499- response" in new GetCalculationResponseByCalcIdTest(nino, calculationId, HttpResponse(
        status = 499, json = Json.toJson("Error message"), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId)

        result.futureValue shouldBe LiabilityCalculationError(499, """"Error message"""")
      }
      "receiving OK with invalid json" in new GetCalculationResponseByCalcIdTest(
        nino, calculationId, HttpResponse(status = OK, json = Json.toJson(""), headers = Map.empty)) {
        val result: Future[LiabilityCalculationResponseModel] = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId)

        result.futureValue shouldBe LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
      }
    }
  }

}
