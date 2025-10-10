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

package models.chargeHistory

import models.chargeHistory.ChargesHistoryResponse.ChargesHistoryResponseReads
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import testConstants.ChargeHistoryTestConstants._
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

import java.time.{LocalDate, LocalDateTime, LocalTime}

class ChargeHistoryResponseModelSpec extends UnitSpec with Matchers {

  val testHttpVerb = "GET"
  val testUri = "/test"

  val testChargeHistoryModelSuccess: ChargeHistoryModel = ChargeHistoryModel(
    taxYear = "2017", documentId = "123456789", documentDate = LocalDate.of(2020, 1, 29),
    documentDescription = "Balancing Charge", totalAmount = 123456789012345.67,
    reversalDate = LocalDateTime.of(LocalDate.of(2020, 2, 24), LocalTime.of(9, 30, 45)),
    reversalReason = "amended return", poaAdjustmentReason = Some("005"))

  val successResponseModel: ChargeHistoryResponseModel =
    ChargesHistoryModel(idType = "NINO", idValue = "AB123456C", regimeType = "ITSA", chargeHistoryDetails = Some(List(testChargeHistoryModelSuccess)))

  val notFoundResponseModel: ChargeHistoryResponseModel = ChargesHistoryModel("", "", "", None)

  val forbiddenResponseModel: ChargeHistoryResponseModel = ChargesHistoryModel("", "", "", None)

  val chargesHistoryErrorModel: ChargeHistoryResponseModel = ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "{ }")

  "ChargesHistoryResponseReads read" should {
    "parse OK status" when {
      "json is valid" in {
        val httpResponse = HttpResponse(OK, json = testValidChargeHistoryDetailsModelJson, headers = Map.empty)
        lazy val result = ChargesHistoryResponseReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe successResponseModel
      }
      "json is invalid" in {
        val httpResponse = HttpResponse(OK, json = testInvalidChargeHistoryDetailsModelJson, headers = Map.empty)
        lazy val result = ChargesHistoryResponseReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing ChargeHistory Data Response")
      }
    }
    "parse NOT_FOUND or FORBIDDEN status" when {
      "no charge history is found" in {
        val httpResponse = HttpResponse(NOT_FOUND, json = testValidChargeHistoryDetailsModelJson, headers = Map.empty)
        lazy val result = ChargesHistoryResponseReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe notFoundResponseModel
      }
      "user is forbidden" in {
        val httpResponse = HttpResponse(FORBIDDEN, json = testValidChargeHistoryDetailsModelJson, headers = Map.empty)
        lazy val result = ChargesHistoryResponseReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe forbiddenResponseModel
      }
    }
    "parse error statuses" when {
      "there is an error in the response" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, json = Json.obj(), headers = Map.empty)
        lazy val result = ChargesHistoryResponseReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe chargesHistoryErrorModel
      }
    }
  }
}
