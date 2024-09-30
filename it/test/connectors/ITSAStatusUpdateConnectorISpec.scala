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
import connectors.itsastatus.ITSAStatusUpdateConnector
import models.core.CorrelationId
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json

class ITSAStatusUpdateConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  val optOutUpdateReason = "10"
  val taxYear = TaxYear.forYearEnd(2024)
  val taxableEntityId = "AB123456A"

  "ITSAStatusUpdateConnector" when {

    ".makeITSAStatusUpdate()" when {

      "provided with a correlationID as a header" should {

        "return a successful response and use the header" in {
          val responseBody =
            """{
              | "taxYear":"2023-24",
              | "updateReason":"10"
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", NO_CONTENT, responseBody)

          val correlationId = CorrelationId()
          val updatedHc = hc.copy(otherHeaders = hc.otherHeaders ++ Seq(correlationId.asHeader()))

          val result = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason)(updatedHc).futureValue

          result shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)

          WiremockHelper.verifyPut(
            uri = s"/income-tax-view-change/itsa-status/update/$taxableEntityId",
            optBody = Some(Json.stringify(Json.parse(responseBody)))
          )
        }
      }

      "return an INTERNAL_SERVER_ERROR" when {

        "status is BAD_REQUEST" in {

          val failureResponseBody =
            """{
              |  "failures": [
              |    {
              |      "code": "INTERNAL_SERVER_ERROR",
              |      "reason": "Request failed due to an unknown error"
              |    }
              |  ]
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", BAD_REQUEST, failureResponseBody)

          val result = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe
            ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to an unknown error")))
        }

        "status is INTERNAL_SERVER_ERROR" in {
          val failureResponseBody =
            """{
              |  "failures": [
              |    {
              |      "code": "INTERNAL_SERVER_ERROR",
              |      "reason": "Request failed due to an unknown error"
              |    }
              |  ]
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", INTERNAL_SERVER_ERROR, failureResponseBody)

          val result = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe
            ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to an unknown error")))
        }

        "invalid failure response structure is returned" in {
          val failureResponseBody =
            """{
              |  "bad stucture": [
              |    {
              |      "lmao": "INTERNAL_SERVER_ERROR",
              |      "bruh": "Request failed due to an unknown error"
              |    }
              |  ]
              |}""".stripMargin

          WiremockHelper.stubPut(s"/income-tax-view-change/itsa-status/update/$taxableEntityId", INTERNAL_SERVER_ERROR, failureResponseBody)

          val result = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason).futureValue

          result shouldBe
            ITSAStatusUpdateResponseFailure(List(
              ErrorItem(
                code = "INTERNAL_SERVER_ERROR",
                reason = "Request failed due to json response: List((/failures,List(JsonValidationError(List(error.path.missing),List()))))"
              )
            ))
        }
      }
    }
  }
}
