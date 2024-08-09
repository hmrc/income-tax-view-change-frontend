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
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargeHistoryModel, ChargesHistoryErrorModel, ChargesHistoryModel}
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
    ChargeHistoryModel("A", "12345", LocalDate.of(2021, 1, 1), "A", 2500, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2021, 1, 1), "A", 2000, LocalDate.of(2024, 3, 15), "Reversal", Some(Increase.code))
  )
  val jumbledChargeHistoryList: List[ChargeHistoryModel] = List(
    ChargeHistoryModel("A", "12345", LocalDate.of(2021, 1, 1), "A", 2500, LocalDate.of(2024, 2, 10), "Reversal", Some(MainIncomeLower.code)),
    ChargeHistoryModel("A", "34556", LocalDate.of(2021, 1, 1), "A", 2300, LocalDate.of(2024, 10, 20), "Reversal", Some(MainIncomeLower.code)),
      ChargeHistoryModel("A", "77777", LocalDate.of(2021, 1, 1), "A", 2000, LocalDate.of(2024, 7, 15), "Reversal", Some(Increase.code)))
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
            AdjustmentModel(2300, Some(LocalDate.of(2024, 7, 15)), "adjustment"),
            AdjustmentModel(2200, Some(LocalDate.of(2024, 10, 20)), "adjustment"),
          )
        )

        val res = TestChargeHistoryService.getAdjustmentHistory(jumbledChargeHistoryList, adjustedDocumentDetail)
        res shouldBe desiredAdjustments
      }
      "charge history entries are not in chronological order" in {
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
    }
  }
}
