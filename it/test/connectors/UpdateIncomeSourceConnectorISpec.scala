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
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Injecting

import java.time.LocalDate

class UpdateIncomeSourceConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: UpdateIncomeSourceConnector = app.injector.instanceOf[UpdateIncomeSourceConnector]

  val nino = "AA123456A"
  val incomeSourceId = "X123"
  val cessationDate: Option[LocalDate] = None
  val taxYearSpecific: TaxYearSpecific = TaxYearSpecific("2024", latencyIndicator = false)

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  "UpdateIncomeSourceConnector" when {
    ".updateCessationDate()" when {
      "sending a request" should {
        val requestBody =
          """
            |{
            |  "nino": "AA123456A",
            |  "incomeSourceID": "X123",
            |  "cessation": {
            |    "cessationIndicator": true
            |  }
            |}
            |""".stripMargin

        "return a successful response" in {
          val responseBody =
            """
              |{
              |   "processingDate": "2024-01-01"
              |}
              |""".stripMargin

          WiremockHelper.stubPut("/income-tax-view-change/update-income-source", OK, requestBody, responseBody)

          val result = connector.updateCessationDate(nino, incomeSourceId, cessationDate).futureValue

          result shouldBe UpdateIncomeSourceResponseModel("2024-01-01")
          WiremockHelper.verifyPut("/income-tax-view-change/update-income-source")
        }
        "return an error when the request fails" in {
          WiremockHelper.stubPut("/income-tax-view-change/update-income-source", INTERNAL_SERVER_ERROR, requestBody, "{}")

          val result = connector.updateCessationDate(nino, incomeSourceId, cessationDate).futureValue

          result shouldBe UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
          WiremockHelper.verifyPut("/income-tax-view-change/update-income-source")
        }
      }
    }
    ".updateIncomeSourceTaxYearSpecific()" when {
      "sending a request" should {
        val requestBody =
          """
            |{
            |  "nino": "AA123456A",
            |  "incomeSourceID": "X123",
            |  "taxYearSpecific": {
            |    "taxYear": "2024",
            |    "latencyIndicator": false
            |  }
            |}
            |""".stripMargin

        "return a successful response" in {
          val responseBody =
            """
              |{
              |   "processingDate": "2024-01-01"
              |}
              |""".stripMargin

          WiremockHelper.stubPut("/income-tax-view-change/update-income-source", OK, requestBody, responseBody)

          val result = connector.updateIncomeSourceTaxYearSpecific(nino, incomeSourceId, taxYearSpecific).futureValue

          result shouldBe UpdateIncomeSourceResponseModel("2024-01-01")
          WiremockHelper.verifyPut("/income-tax-view-change/update-income-source")
        }
        "return an error when the request fails" in {
          WiremockHelper.stubPut("/income-tax-view-change/update-income-source", INTERNAL_SERVER_ERROR, requestBody, "{}")

          val result = connector.updateIncomeSourceTaxYearSpecific(nino, incomeSourceId, taxYearSpecific).futureValue

          result shouldBe UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
          WiremockHelper.verifyPut("/income-tax-view-change/update-income-source")
        }
      }
    }
  }
}
