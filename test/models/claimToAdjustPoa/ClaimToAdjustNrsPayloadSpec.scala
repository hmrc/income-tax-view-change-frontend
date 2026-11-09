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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}

class ClaimToAdjustNrsPayloadSpec extends AnyWordSpec with Matchers {

  "ClaimToAdjustNrsPayload JSON writer" should {

    "generate the correct JSON structure with all expected detail fields" in {
      val payload = ClaimToAdjustNrsPayload(
        credId                          = Some("dL2tPznmbSS0KWDt"),
        saUtr                           = Some("3688209917"),
        nino                            = "PA000002A",
        userType                        = Some("Individual"),
        generatedAt                     = "2024-11-01T10:08:07.116Z",
        isDecreased                     = true,
        previousPaymentOnAccountAmount  = BigDecimal(5000),
        requestedPaymentOnAccountAmount = BigDecimal(3000),
        adjustmentReasonCode            = "001",
        adjustmentReasonDescription     = "My main income will be lower",
        mtditId                         = "XTIT85957249655"
      )

      val expectedJson = Json.obj(
        "detail" -> Json.obj(
          "adjustmentReasonCode"            -> "001",
          "adjustmentReasonDescription"     -> "My main income will be lower",
          "credId"                          -> "dL2tPznmbSS0KWDt",
          "isDecreased"                     -> true,
          "mtditid"                         -> "XTIT85957249655",
          "nino"                            -> "PA000002A",
          "outcome"                         -> Json.obj("isSuccessful" -> true),
          "previousPaymentOnAccountAmount"  -> 5000,
          "requestedPaymentOnAccountAmount" -> 3000,
          "saUtr"                           -> "3688209917",
          "generatedAt"                     -> "2024-11-01T10:08:07.116Z",
          "userType"                        -> "Individual"
        )
      )

      Json.toJson(payload) shouldBe expectedJson
    }

    "exclude any audit or tag fields from the final output" in {
      val payload = ClaimToAdjustNrsPayload(
        credId = Some("foo"), saUtr = Some("123"), nino = "AA000000A",
        userType = Some("Individual"), generatedAt = "2024-01-01T00:00:00Z",
        isDecreased = true, previousPaymentOnAccountAmount = 1,
        requestedPaymentOnAccountAmount = 1, adjustmentReasonCode = "001",
        adjustmentReasonDescription = "reason", mtditId = "XMIT123"
      )

      val json = Json.toJson(payload).as[JsObject]
      json.keys shouldBe Set("detail")
      json.keys should not contain ("auditSource")
      json.keys should not contain ("auditType")
      json.keys should not contain ("dataPipeline")
      json.keys should not contain ("eventId")
      json.keys should not contain ("metadata")
      json.keys should not contain ("tags")
    }
  }
}
