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

package models.claimToAdjustPoa

import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.{JsObject, Json}
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

class ClaimToAdjustPoaResponseSpec extends UnitSpec {

  val testHttpVerb = "POST"
  val testUri = "/test"

  val testValidRequest: JsObject = Json.obj(
      "processingDate" -> "2024-01-31T09:27:17Z"
  )

  val testInvalidRequest: JsObject = Json.obj(
    "invalid" -> "json"
  )

  val testFailureJson: JsObject = Json.obj(
    "message" -> "INVALID_PAYLOAD, INVALID_CORRELATION_ID"
  )

  val testInvalidFailureJson: JsObject = Json.obj(
    "invalid" -> "json"
  )

  "ClaimToAdjustPoaReads" when {
    "read" should {
      "parse CREATED status and valid json data" when {
        "json is valid" in {
          val httpResponse = HttpResponse(CREATED, json = testValidRequest, headers = Map.empty)
          lazy val result = ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponseReads.read(testHttpVerb, testUri, httpResponse)
          result shouldBe Right(ClaimToAdjustPoaSuccess(processingDate = "2024-01-31T09:27:17Z"))
        }
        "json is invalid" in {
          val httpResponse = HttpResponse(CREATED, json = testInvalidRequest, headers = Map.empty)
          lazy val result = ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponseReads.read(testHttpVerb, testUri, httpResponse)
          result shouldBe Left(ClaimToAdjustPoaInvalidJson)
        }
      }

      "parse error status and valid json data" when {
        "json is valid" in {
          val httpResponse = HttpResponse(BAD_REQUEST, json = testFailureJson, headers = Map.empty)
          lazy val result = ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponseReads.read(testHttpVerb, testUri, httpResponse)
          result shouldBe Left(ClaimToAdjustPoaError("INVALID_PAYLOAD, INVALID_CORRELATION_ID"))
        }

        "json is invalid" in {
          val httpResponse = HttpResponse(BAD_REQUEST, json = testInvalidFailureJson, headers = Map.empty)
          lazy val result = ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponseReads.read(testHttpVerb, testUri, httpResponse)
          result shouldBe Left(ClaimToAdjustPoaInvalidJson)
        }
      }
    }
  }
}
