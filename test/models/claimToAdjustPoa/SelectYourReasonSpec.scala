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

import models.claimToAdjustPoa.SelectYourReason._
import play.api.libs.json.{JsError, JsString, Json}
import testUtils.UnitSpec

class SelectYourReasonSpec extends UnitSpec {

  "SelectYourReason" should {

    "read and write from JSON correctly" when {

      "MainIncomeLower" in {
        val mainIncomeLower: SelectYourReason = MainIncomeLower
        val json = Json.toJson(mainIncomeLower)
        json shouldBe JsString("001")
        json.as[SelectYourReason] shouldBe mainIncomeLower
      }

      "OtherIncomeLower" in {
        val otherIncomeLower: SelectYourReason = OtherIncomeLower
        val json = Json.toJson(otherIncomeLower)
        json shouldBe JsString("002")
        json.as[SelectYourReason] shouldBe otherIncomeLower
      }

      "AllowanceOrReliefHigher" in {
        val allowanceOrReliefHigher: SelectYourReason = AllowanceOrReliefHigher
        val json = Json.toJson(allowanceOrReliefHigher)
        json shouldBe JsString("003")
        json.as[SelectYourReason] shouldBe allowanceOrReliefHigher
      }

      "MoreTaxedAtSource" in {
        val moreTaxedAtSource: SelectYourReason = MoreTaxedAtSource
        val json = Json.toJson(moreTaxedAtSource)
        json shouldBe JsString("004")
        json.as[SelectYourReason] shouldBe moreTaxedAtSource
      }

      "Increase" in {
        val increase: SelectYourReason = Increase
        val json = Json.toJson(increase)
        json shouldBe JsString("005")
        json.as[SelectYourReason] shouldBe increase
      }
    }

    "return error if code is invalid" in {
      JsString("006").validate[SelectYourReason] shouldBe JsError(
        "Could not parse SelectYourReason from value: \"006\""
      )
    }
  }
}
