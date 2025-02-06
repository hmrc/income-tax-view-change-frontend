/*
 * Copyright 2025 HM Revenue & Customs
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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesErrorModel, OutstandingChargesModel, OutstandingChargesResponseModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

import java.time.LocalDate

class OutstandingChargesConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: OutstandingChargesConnector = app.injector.instanceOf[OutstandingChargesConnector]

  "OutstandingChargesConnector" when {
    ".getOutstandingCharges()" when {

      "OK" should {

        "return a successful OutstandingChargesModel" in {

          val idType   = "fakeId"
          val idNumber = "1337"
          val taxYear: String = "2023"
          val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

          val response: OutstandingChargesModel =
            OutstandingChargesModel(
              List(
                OutstandingChargeModel("BCD", Some(LocalDate.of(2025, 1, 1)), 123456789012345.67, 1234),
                OutstandingChargeModel("ACI", None, 12.67, 1234)
              )
            )

          WiremockHelper.stubGet(url, OK, Json.toJson(response).toString())

          val result: OutstandingChargesResponseModel =
            connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

          result shouldBe response
          WiremockHelper.verifyGet(uri = url)
        }

        "request returns json validation errors" should {

          "return a OutstandingChargesErrorModel" in {

            val idType   = "fakeId"
            val idNumber = "1337"
            val taxYear: String = "2023"
            val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

            WiremockHelper.stubGet(url, OK, """{"bad_key":"bad_value"}""")

            val result: OutstandingChargesResponseModel =
              connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

            result shouldBe OutstandingChargesErrorModel(
              Status.INTERNAL_SERVER_ERROR,
              "Json Validation Error. Parsing OutstandingCharges Data Response"
            )
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }

      "INTERNAL_SERVER_ERROR" when {

        "request returns an error json response" should {

          "return a OutstandingChargesErrorModel with response body message" in {

            val idType   = "fakeId"
            val idNumber = "1337"
            val taxYear: String = "2023"
            val url = s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear-04-05"

            val response = """{"fake_error_key: "fake_error_value"}"""

            WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, response)

            val result: OutstandingChargesResponseModel =
              connector.getOutstandingCharges(idType, idNumber, taxYear).futureValue

            result shouldBe OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, response)
            WiremockHelper.verifyGet(uri = url)
          }
        }
      }
    }
  }

}
