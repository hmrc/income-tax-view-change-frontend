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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.core.Nino
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Injecting

class CalculationListConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: CalculationListConnector = app.injector.instanceOf[CalculationListConnector]

  val nino = "AA123456A"
  val taxYearEnd = "2024"

  "CalculationListConnector" when {
    ".getLegacyCalculationList()" when {
      "sending a request" should {
        "return a successful response" in {
          val responseBody =
            """
              |{
              |  "calculationId": "TEST_ID",
              |  "calculationTimestamp": "TEST_STAMP",
              |  "calculationType": "TEST_TYPE",
              |  "crystallised": false
              |}
              |""".stripMargin

          WiremockHelper.stubGet(s"/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd", OK, responseBody)

          val result = connector.getLegacyCalculationList(nino, taxYearEnd).futureValue

          result shouldBe CalculationListModel("TEST_ID", "TEST_STAMP", "TEST_TYPE", Some(false))
          WiremockHelper.verifyGet(s"/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd")
        }
        "return an error when the request fails" in {
          val responseBody = "{}"

          WiremockHelper.stubGet(s"/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd", INTERNAL_SERVER_ERROR, responseBody)

          val result = connector.getLegacyCalculationList(nino, taxYearEnd).futureValue

          result shouldBe CalculationListErrorModel(INTERNAL_SERVER_ERROR, "{}")
          WiremockHelper.verifyGet(s"/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd")
        }
      }
    }
    ".getCalculationList()" when {
      "sending a request" should {
        "return a successful response" in {
          val responseBody =
            """
              |{
              |  "calculationId": "TEST_ID",
              |  "calculationTimestamp": "TEST_STAMP",
              |  "calculationType": "TEST_TYPE",
              |  "crystallised": false
              |}
              |""".stripMargin

          WiremockHelper.stubGet(s"/income-tax-view-change/calculation-list/$nino/$taxYearEnd", OK, responseBody)

          val result = connector.getCalculationList(Nino(nino), taxYearEnd).futureValue

          result shouldBe CalculationListModel("TEST_ID", "TEST_STAMP", "TEST_TYPE", Some(false))
          WiremockHelper.verifyGet(s"/income-tax-view-change/calculation-list/$nino/$taxYearEnd")
        }
        "return an error when the request fails" in {
          val responseBody = "{}"

          WiremockHelper.stubGet(s"/income-tax-view-change/calculation-list/$nino/$taxYearEnd", INTERNAL_SERVER_ERROR, responseBody)

          val result = connector.getCalculationList(Nino(nino), taxYearEnd).futureValue

          result shouldBe CalculationListErrorModel(INTERNAL_SERVER_ERROR, "{}")
          WiremockHelper.verifyGet(s"/income-tax-view-change/calculation-list/$nino/$taxYearEnd")
        }
      }
    }
  }
}
