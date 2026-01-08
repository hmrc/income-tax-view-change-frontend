/*
 * Copyright 2024 HM Revenue & Customs
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

import _root_.helpers.servicemocks.AuditStub
import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock
import models.liabilitycalculation._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Injecting

class IncomeTaxCalculationConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: IncomeTaxCalculationConnector = app.injector.instanceOf[IncomeTaxCalculationConnector]

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  val nino = "AA123456A"
  val taxYear = "2024"
  val mtditid = "XAIT0000123456"
  val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  val jsonResponse: String =
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata": {
      |    "calculationTimestamp": "2024-02-15T09:35:15.094Z",
      |    "calculationType": "inYear",
      |    "calculationReason": "customerRequest"
      |  }
      |}
      |""".stripMargin

  "IncomeTaxCalculationConnector" when {
    ".getCalculationResponse()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear", OK, jsonResponse)

          val result = connector.getCalculationResponse(mtditid, nino, taxYear, None).futureValue

          result shouldBe LiabilityCalculationResponse(Inputs(PersonalInformation("UK", None)), Metadata(Some("2024-02-15T09:35:15.094Z"), calculationType = "inYear", Some("customerRequest"), None, None, None), None, None)
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear")
        }

        "return an error when the request returns an invalid JSON" in {
          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear", OK, "{}")

          val result = connector.getCalculationResponse(mtditid, nino, taxYear, None).futureValue

          result shouldBe LiabilityCalculationError(500, "Json validation error parsing calculation response")
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear")
        }

        "return an error when the request returns an INTERNAL SERVER ERROR" in {
          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}")

          val result = connector.getCalculationResponse(mtditid, nino, taxYear, None).futureValue

          result shouldBe LiabilityCalculationError(500, "{}")
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear")
        }
      }
    }
    "getCalculationResponseByCalcId()" when {

      "sending a request" should {

        "return a successful response" in {

          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear", OK, jsonResponse)

          val result = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYear.toInt).futureValue

          result shouldBe LiabilityCalculationResponse(Inputs(PersonalInformation("UK", None)), Metadata(Some("2024-02-15T09:35:15.094Z"), calculationType = "inYear", Some("customerRequest"), None, None, None), None, None)
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear")
        }

        "return an error when the request returns an invalid JSON" in {
          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear", OK, "{}")

          val result = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYear.toInt).futureValue

          result shouldBe LiabilityCalculationError(500, "Json validation error parsing calculation response")
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear")
        }

        "return an error when the request returns an INTERNAL SERVER ERROR" in {
          WiremockHelper.stubGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}")

          val result = connector.getCalculationResponseByCalcId(mtditid, nino, calculationId, taxYear.toInt).futureValue

          result shouldBe LiabilityCalculationError(500, "{}")
          WiremockHelper.verifyGet(s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calculationId/calculation-details?taxYear=$taxYear")
        }
      }
    }
  }
}
