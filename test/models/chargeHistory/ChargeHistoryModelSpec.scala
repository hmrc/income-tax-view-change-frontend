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

import enums.{AdjustmentReversalReason, AmendedReturnReversalReason, CustomerRequestReason, UnknownReversalReason}
import org.scalatest.matchers.should.Matchers
import testUtils.UnitSpec

import java.time.LocalDate

class ChargeHistoryModelSpec extends UnitSpec with Matchers {

  def testChargeHistoryModel(reversalReason: String): ChargeHistoryModel = {
    ChargeHistoryModel("2021", "DOCID01", LocalDate.parse("2020-07-08"), "docDescription", 15000.0,
      LocalDate.of(2021, 9, 9), reversalReason, None)
  }
  def testPoaChargeHistoryModel(reversalReason: String): ChargeHistoryModel = {
    ChargeHistoryModel("2021", "DOCID01", LocalDate.parse("2020-07-08"), "docDescription", 15000.0,
      LocalDate.of(2021, 9, 9), reversalReason, Some("001"))
  }

  "chargeHistoryModel" when {

    "calling .reasonCode" should {

      "return a valid message key for a poa adjustment" in {
        testPoaChargeHistoryModel("Reversal").reasonCode shouldBe AdjustmentReversalReason
      }

      "return a valid message key for an amended reversal" in {
        testChargeHistoryModel("amended return").reasonCode shouldBe AmendedReturnReversalReason
      }

      "return a valid message key for a customer requested reversal" in {
        testChargeHistoryModel("Customer Request").reasonCode shouldBe CustomerRequestReason
      }

      "return an unknown message key for a non matching reversal reason" in {
        testChargeHistoryModel("Unknown").reasonCode shouldBe UnknownReversalReason
      }
    }
  }
}
