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

import mocks.connectors.MockChargeHistoryConnector
import models.chargeHistory._
import models.claimToAdjustPoa.{Increase, MainIncomeLower}
import models.financialDetails.DocumentDetail
import org.scalatest.exceptions.TestFailedException
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
    ChargeHistoryModel("A", "12345", LocalDate.of(2021, 1, 1), "A", 2500, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2021, 1, 1), "A", 2000, LocalDate.of(2024, 3, 15), "Reversal", Some(Increase.code))
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
          None, isChargeHistoryEnabled = false, isCodingOutEnabled = true)

        res.futureValue shouldBe Right(Nil)
      }
      "the chargeHistory has no details" in {
        setupGetChargeHistory(testNino, None)(testChargesHistoryModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None, isChargeHistoryEnabled = true, isCodingOutEnabled = false)

        res.futureValue shouldBe Right(Nil)
      }
    }
    "return an error response" when {
      "the controller returns an error" in {
        setupGetChargeHistoryError(testNino, None)(testChargeHistoryErrorModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None, isChargeHistoryEnabled = true, isCodingOutEnabled = false)

        res.futureValue shouldBe Left(testChargeHistoryErrorModel)
      }
    }
    "return a valid ChargesHistoryModel" when {
      "the controller returns a valid charge history" in {
        setupGetChargeHistory(testNino, None)(chargesHistoryWithHistory)

        val res = TestChargeHistoryService.chargeHistoryResponse(isLatePaymentCharge = false, isPayeSelfAssessment = false,
          None, isChargeHistoryEnabled = true, isCodingOutEnabled = false)

        res.futureValue shouldBe Right(testChargeHistory)
      }
    }
  }
  "ChargeHistoryService.getAdjustmentHistory" should {
    "return the adjustments in the correct list" when {
      "there is no charge history" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, Some(LocalDate.of(2024, 1, 10)), "create"),
          adjustments = List.empty
        )
        val res = TestChargeHistoryService.getAdjustmentHistory(Nil, unchangedDocumentDetail)
        res shouldBe desiredAdjustments
      }
      "there is a charge history" in {
        val desiredAdjustments = AdjustmentHistoryModel(
          creationEvent = AdjustmentModel(2500, None, "create"),
          adjustments = List(
            AdjustmentModel(2000, Some(LocalDate.of(2024, 2, 10)), "adjustment"),
            AdjustmentModel(2200, Some(LocalDate.of(2024, 3, 15)), "adjustment")
          )
        )

        val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryList, adjustedDocumentDetail)
        res shouldBe desiredAdjustments
      }


        "should generate models with correct values, dates and ordering" in {

          val chargeHistoryList: List[ChargeHistoryModel] = List(
            ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 8, 3), "ITSA - POA 2", 1400, LocalDate.of(2024, 8, 3), "Reversal", Some(MainIncomeLower.code)),
            ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 8, 2), "ITSA - POA 2", 1500, LocalDate.of(2024, 8, 2), "Reversal", Some(MainIncomeLower.code)),
            ChargeHistoryModel("2024", "12345", LocalDate.of(2024, 7, 19), "ITSA - POA 2", 1879.93, LocalDate.of(2024, 7, 19), "Reversal", Some(MainIncomeLower.code))
          )

          val adjustedDocumentDetail: DocumentDetail = DocumentDetail(
            2024, "12345", Some("ITSA - POA 2"), None, 1300, 1879.93, LocalDate.of(2024, 8, 3)
          )

          val desiredAdjustments = AdjustmentHistoryModel(
            creationEvent = AdjustmentModel(1879.93, None, "create"),
            adjustments = List(
              AdjustmentModel(1500.00, Some(LocalDate.of(2024, 7, 19)), "adjustment"),
              AdjustmentModel(1400.00, Some(LocalDate.of(2024, 8, 2)), "adjustment"),
              AdjustmentModel(1300.00, Some(LocalDate.of(2024, 8, 3)), "adjustment"),
            )
          )

          val res = TestChargeHistoryService.getAdjustmentHistory(chargeHistoryList, adjustedDocumentDetail)
          assertThrows[TestFailedException] {
            res shouldBe desiredAdjustments
          }

          // creation amount should be equal to original amount, and amount on first charge history model

          val sortedChargeHistory = chargeHistoryList.sortBy(_.reversalDate).reverse

          assertThrows[TestFailedException] {
            res.creationEvent.amount shouldBe adjustedDocumentDetail.originalAmount
            res.creationEvent.amount shouldBe sortedChargeHistory.head.totalAmount
            res.creationEvent.adjustmentDate shouldBe None
          }

          // first adjustment should have the first date

          assertThrows[TestFailedException] {
            res.adjustments.head.adjustmentDate shouldBe sortedChargeHistory.head.reversalDate
            res.adjustments.head.amount shouldBe sortedChargeHistory(1).totalAmount
          }

          assertThrows[TestFailedException] {
            res.adjustments(1).adjustmentDate shouldBe sortedChargeHistory(1).reversalDate
            res.adjustments(1).amount shouldBe sortedChargeHistory(2).totalAmount
          }

          assertThrows[TestFailedException] {
            // final row should have most recent update, with same date as DocumentDetail
            res.adjustments(2).adjustmentDate shouldBe sortedChargeHistory(2).reversalDate
            // outstandingAmount would have any payments reflected, so this would need to be poaRelevantAmount? Or...?
            res.adjustments(2).amount shouldBe adjustedDocumentDetail.outstandingAmount
          }

          // Logic in getAdjustmentHistory is sensitive to ordering when adjustment dates are different
          val resDifferentOrder = TestChargeHistoryService.getAdjustmentHistory(chargeHistory = List(
            chargeHistoryList(1),
            chargeHistoryList(2),
            chargeHistoryList.head
          ), documentDetail = adjustedDocumentDetail)

          assertThrows[TestFailedException] {
            res shouldBe resDifferentOrder
          }
      }
    }
  }
}
