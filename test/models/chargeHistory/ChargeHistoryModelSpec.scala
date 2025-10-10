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

import enums.{AdjustmentReversalReason, AmendedReturnReversalReason, CustomerRequestReason}
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import testUtils.UnitSpec

import java.time.{LocalDate, LocalDateTime, LocalTime}

class ChargeHistoryModelSpec extends UnitSpec with Matchers {

  def testChargeHistoryModel(reversalReason: String): ChargeHistoryModel = {
    ChargeHistoryModel("2021", "DOCID01", LocalDate.parse("2020-07-08"), "docDescription", 15000.0,
      LocalDateTime.of(LocalDate.of(2021, 9, 9), LocalTime.of(9, 30, 45)), reversalReason, None)
  }
  def testPoaChargeHistoryModel(reversalReason: String): ChargeHistoryModel = {
    ChargeHistoryModel("2021", "DOCID01", LocalDate.parse("2020-07-08"), "docDescription", 15000.0,
      LocalDateTime.of(LocalDate.of(2021, 9, 9), LocalTime.of(9, 30, 45)), reversalReason, Some("001"))
  }

  "chargeHistoryModel" when {

    "calling .reasonCode" when {

      "the Poa adjustment reason is defined" should {

        "return AdjustmentReversalReason when the Poa adjustment reason is defined" in {
          testPoaChargeHistoryModel("Reversal").reasonCode shouldBe Right(AdjustmentReversalReason)
        }
      }

      "the Poa adjustment reason is not defined" should {

        "return AmendedReturnReversalReason when the reversal reason is 'amended return'" in {
          testChargeHistoryModel("amended return").reasonCode shouldBe Right(AmendedReturnReversalReason)
        }

        "return CustomerRequestReason when the reversal reason is 'Customer Request'" in {
          testChargeHistoryModel("Customer Request").reasonCode shouldBe Right(CustomerRequestReason)
        }

        "return an exception and message when the reversal reason is unknown" in {
          val result = testChargeHistoryModel("Unknown").reasonCode
          inside(result) { case Left(e) =>
            e shouldBe an[Exception]
            e.leftSideValue.getMessage shouldBe "Unable to resolve reversal reason"
          }
        }
      }
    }
  }
}
