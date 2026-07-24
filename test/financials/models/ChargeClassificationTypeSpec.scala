/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.models

import common.testUtils.UnitSpec
import financials.enums.ChargeClassificationType
import org.scalatest.prop.TableDrivenPropertyChecks.*

class ChargeClassificationTypeSpec extends UnitSpec {

  "ChargeClassificationType.fromString" should {
    "return correct ChargeClassificationType based on the String value passed" in {
      val chargeClassificationTypesTable = Table(
        ("String Value", "Charge Classification Type"),
        ("AC", ChargeClassificationType.AutoCorrection),
        ("MC", ChargeClassificationType.ManualCorrection),
        ("RC", ChargeClassificationType.RejectedCorrection),
        ("RA", ChargeClassificationType.RevenueAmendments),
        ("AF", ChargeClassificationType.AnnualFinancialAdjustment)
      )

      forAll(chargeClassificationTypesTable) { (stringValue, chargeClassificationType) =>
        ChargeClassificationType.fromString(stringValue) shouldBe Some(chargeClassificationType)
      }
    }

    "return None based on the invalid String value passed" in {
      val invalidChargeClassificationTypesTable = Table(
        "String Value",
        "",
        "  ",
        "in",
        "er",
        "AA",
        "Bz",
        "S ",
        "!!",
        "12"
      )

      forAll(invalidChargeClassificationTypesTable) { value =>
        ChargeClassificationType.fromString(value) shouldBe None
      }
    }
  }
}
