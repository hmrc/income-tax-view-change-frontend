/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import assets.FinancialDetailsTestConstants._
import models.financialDetails._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class FinancialDetailsResponseModelSpec  extends UnitSpec with Matchers {

  "The ChargesModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsModel](testValidFinancialDetailsModel) shouldBe testValidFinancialDetailsModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsModel](testValidFinancialDetailsModelJson).fold(
        invalid => invalid,
        valid => valid) shouldBe testValidFinancialDetailsModel
    }

  }

  "The ChargesErrorModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModel) shouldBe testFinancialDetailsErrorModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModelJson) shouldBe JsSuccess(testFinancialDetailsErrorModel)
    }
  }

  "dunningLockExists" should {
    val dunningLock = Some("Stand over order")
    val unsupportedLock = Some("Disputed debt")

    def financialDetailsModelWithDunningLock: FinancialDetailsModel = financialDetailsModel(dunningLock = dunningLock)
    def financialDetailsModelWithUnsupportedLock: FinancialDetailsModel = financialDetailsModel(dunningLock = unsupportedLock)
    def financialDetailsModelWithoutDunningLock: FinancialDetailsModel = financialDetailsModel()

    "return true when there is a dunningLock against a charge" in {
      financialDetailsModelWithDunningLock.dunningLockExists(id1040000123) shouldBe true
    }

    "return false when there is an unsupported dunningLock against a charge" in {
      financialDetailsModelWithUnsupportedLock.dunningLockExists(id1040000123) shouldBe false
    }

    "return true when there is not a dunningLock against a charge" in {
      financialDetailsModelWithoutDunningLock.dunningLockExists(id1040000123) shouldBe false
    }
  }

}
