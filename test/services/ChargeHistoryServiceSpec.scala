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

package services

import enums.{AdjustmentReversalReason, AmendedReturnReversalReason, CreateReversalReason}
import mocks.connectors.MockChargeHistoryConnector
import models.chargeHistory._
import models.claimToAdjustPoa.{Increase, MainIncomeLower}
import models.financialDetails.DocumentDetail
import play.api.http.Status.INTERNAL_SERVER_ERROR
import testConstants.BaseTestConstants.{docNumber, taxYear, testNino}
import testUtils.TestSupport

import java.time.LocalDate

class ChargeHistoryServiceSpec extends TestSupport with MockChargeHistoryConnector {

  object TestChargeHistoryService extends ChargeHistoryService(
    mockChargeHistoryConnector
  )

  val testChargesHistoryModel: ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", None)

  val testChargeHistoryErrorModel: ChargesHistoryErrorModel = ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure")

  val testChargeHistory: List[ChargeHistoryModel] = List(ChargeHistoryModel(
    taxYear = taxYear.toString, documentId = docNumber, documentDate = LocalDate.of(2021, 1, 1), documentDescription = "desc", totalAmount = 1000,
    reversalDate = LocalDate.of(2021, 1, 1), reversalReason = "", poaAdjustmentReason = Some(MainIncomeLower.code)
  ))

  val chargesHistoryWithHistory: ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", Some(testChargeHistory))

  val chargeHistoryList: List[ChargeHistoryModel] = List(
    ChargeHistoryModel("A", "12345", LocalDate.of(2024, 2, 10), "A", 2500, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2024, 3, 15), "A", 2000, LocalDate.of(2024, 3, 15), "Reversal", Some(Increase.code))
  )
  val jumbledChargeHistoryList: List[ChargeHistoryModel] = List(
    ChargeHistoryModel("A", "12345", LocalDate.of(2024, 2, 10), "A", 2500, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2024, 10, 20), "A", 2300, LocalDate.of(2024, 10, 20), "Reversal", Some(MainIncomeLower.code)),
      ChargeHistoryModel("A", "77777", LocalDate.of(2024, 7, 15), "A", 2000, LocalDate.of(2024, 7, 15), "Reversal", Some(Increase.code)))
  val chargeHistoryWithAmended: List[ChargeHistoryModel] = List(
    ChargeHistoryModel("A", "77777", LocalDate.of(2024, 1, 15), "TRM Amend Charge", 2500, LocalDate.of(2024, 1, 15), "amended return", None),
    ChargeHistoryModel("A", "12345", LocalDate.of(2024, 2, 10), "A", 2000, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2024, 3, 15), "A", 2300, LocalDate.of(2024, 3, 15), "Reversal", Some(Increase.code))
  )
  val unchangedDocumentDetail: DocumentDetail = DocumentDetail(
    1, "A", Some("PoA1"), None, 2500, 2500, LocalDate.of(2024, 1, 10)
  )
  val adjustedDocumentDetail: DocumentDetail = DocumentDetail(
    1, "A", Some("PoA1"), None, 2200, 2200, LocalDate.of(2024, 3, 15)
  )

  "ChargeHistoryService.chargeHistoryResponse" should {
    "return a Right(Nil)" when {
      "the conditions are not met" in {
        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = true, isPayeSelfAssessment = true,
          None)

        res.futureValue shouldBe Right(Nil)
      }
      "the chargeHistory has no details" in {
        setupGetChargeHistory(testNino, None)(testChargesHistoryModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None)

        res.futureValue shouldBe Right(Nil)
      }
    }
    "return an error response" when {
      "the controller returns an error" in {
        setupGetChargeHistoryError(testNino, None)(testChargeHistoryErrorModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None)

        res.futureValue shouldBe Left(testChargeHistoryErrorModel)
      }
    }
    "return a valid ChargesHistoryModel" when {
      "the controller returns a valid charge history" in {
        setupGetChargeHistory(testNino, None)(chargesHistoryWithHistory)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None)

        res.futureValue shouldBe Right(testChargeHistory)
      }
    }
  }
  "ChargeHistoryService.getAdjustmentHistory" should {
    "return the adjustments in the correct list" when {
      "there is no charge history" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, Some(LocalDate.of(2024, 1, 10)), CreateReversalReason),
          adjustments = List.empty
        )
        val res = TestChargeHistoryService.getAdjustmentHistory(Nil, unchangedDocumentDetail)
        res shouldBe desiredAdjustments
      }
      "there is a charge history" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, Some(LocalDate.of(2024, 2, 10)), CreateReversalReason),
          adjustments = List(
            AdjustmentModel(2000, Some(LocalDate.of(2024, 2, 10)), AdjustmentReversalReason),
            AdjustmentModel(2300, Some(LocalDate.of(2024, 7, 15)), AdjustmentReversalReason),
            AdjustmentModel(2200, Some(LocalDate.of(2024, 10, 20)), AdjustmentReversalReason),
          )
        )

        val res = TestChargeHistoryService.getAdjustmentHistory(jumbledChargeHistoryList, adjustedDocumentDetail)
        res shouldBe desiredAdjustments
      }
      "there is a charge history and charge history entries are not in chronological order" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, Some(LocalDate.of(2024, 2, 10)), CreateReversalReason),
          adjustments = List(
            AdjustmentModel(2000, Some(LocalDate.of(2024, 2, 10)), AdjustmentReversalReason),
            AdjustmentModel(2200, Some(LocalDate.of(2024, 3, 15)), AdjustmentReversalReason)
          )
        )

        val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryList, adjustedDocumentDetail)
        res shouldBe desiredAdjustments
      }
      "there is a charge history (extensive)" when {

        // assuming:
        // an initial creation on date unknown, at 1879.93
        // a change on 19-7-2024 to 1500.00
        // a change on 2-8-2024 to 1400.00
        // a change on 3-8-2024 to 1300.00

        // should have from 1554:
        val chargeHistoryList: List[ChargeHistoryModel] = List(
          ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 7, 19), "ITSA - POA 2", 1879.93, LocalDate.of(2024, 7, 19), "Reversal", Some(MainIncomeLower.code)),
          ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 8, 2), "ITSA - POA 2", 1500, LocalDate.of(2024, 8, 2), "Reversal", Some(MainIncomeLower.code)),
          ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 8, 3), "ITSA - POA 2", 1400, LocalDate.of(2024, 8, 3), "Reversal", Some(MainIncomeLower.code))
        )

        // the nth change will have a charge history model with the date of the change n, and the amount of change n-1
        // i.e. change 1 has the original amount, change 2 has the amount after change 1

        // the amount after the final change will be on the DocumentDetail from 1553:
        val adjustedDocumentDetail: DocumentDetail = DocumentDetail(
          2024, "12345", Some("ITSA - POA 2"), None, 1300, 1300, LocalDate.of(2024, 8, 3)
        )

        "with charge history in chronological order" when {
          val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryList, adjustedDocumentDetail)

          // because the first chargeHistoryModel amount is the amount from before the first adjustment
          "creation amount should match earliest ChargeHistoryModel amount" in {
            res.creationEvent.amount shouldBe 1879.93
          }

          "creation date should be the earliest document date" in {
            res.creationEvent.adjustmentDate shouldBe Some(LocalDate.of(2024, 7, 19))
          }

          "1st adjustment date should match the 1st ChargeHistoryModel date" in {
            res.adjustments.head.adjustmentDate.get shouldBe LocalDate.of(2024, 7, 19)
          }

          "1st adjustment amount should match the 2nd ChargeHistoryModel amount" in {
            res.adjustments.head.amount shouldBe 1500.0
          }

          "2nd adjustment date should match the 2nd ChargeHistoryModel date" in {
            res.adjustments(1).adjustmentDate.get shouldBe LocalDate.of(2024, 8, 2)
          }

          "2nd adjustment amount should match the 3rd ChargeHistoryModel amount" in {
            res.adjustments(1).amount shouldBe 1400.0
          }

          "3rd adjustment date should match the 3rd ChargeHistoryModel date" in {
            res.adjustments(2).adjustmentDate.get shouldBe LocalDate.of(2024, 8, 3)
          }

          "3rd adjustment amount should match the current value of the charge" in {
            res.adjustments(2).amount shouldBe 1300.0
          }
        }

        "with charge history in reverse chronological order" when {
          val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryList.reverse, adjustedDocumentDetail)

          // because the first chargeHistoryModel amount is the amount from before the first adjustment
          "creation amount should match earliest ChargeHistoryModel amount" in {
            res.creationEvent.amount shouldBe 1879.93
          }

          "creation date should be the earliest document date" in {
            res.creationEvent.adjustmentDate shouldBe Some(LocalDate.of(2024, 7, 19))
          }

          "1st adjustment date should match the 1sts ChargeHistoryModel date" in {
            res.adjustments.head.adjustmentDate.get shouldBe LocalDate.of(2024, 7, 19)
          }

          "1st adjustment amount should match the 2nd ChargeHistoryModel amount" in {
            res.adjustments.head.amount shouldBe 1500.0
          }

          "2nd adjustment date should match the 2nd ChargeHistoryModel date" in {
            res.adjustments(1).adjustmentDate.get shouldBe LocalDate.of(2024, 8, 2)
          }

          "2nd adjustment amount should match the 3rd ChargeHistoryModel amount" in {
            res.adjustments(1).amount shouldBe 1400.0
          }

          "3rd adjustment date should match the 3rd ChargeHistoryModel date" in {
            res.adjustments(2).adjustmentDate.get shouldBe LocalDate.of(2024, 8, 3)
          }

          "3rd adjustment amount should match the current value of the charge" in {
            res.adjustments(2).amount shouldBe 1300.0
          }
        }
      }
      "there is a charge history including adjustments for tax return amendments" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, Some(LocalDate.of(2024, 1, 15)), CreateReversalReason),
          adjustments = List(
            AdjustmentModel(2000, Some(LocalDate.of(2024, 1, 15)), AmendedReturnReversalReason),
            AdjustmentModel(2300, Some(LocalDate.of(2024, 2, 10)), AdjustmentReversalReason),
            AdjustmentModel(2200, Some(LocalDate.of(2024, 3, 15)), AdjustmentReversalReason),
          )
        )

        val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryWithAmended, adjustedDocumentDetail)
        res shouldBe desiredAdjustments
      }
    }
  }
}
