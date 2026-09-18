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

package returns.services

import common.testConstants.BaseTestConstants.{docNumber, taxYear, testNino}
import common.testUtils.TestSupport
import returns.mocks.connectors.MockChargeHistoryConnector
import returns.models.chargeHistory.*
import play.api.http.Status.INTERNAL_SERVER_ERROR

import java.time.{LocalDate, LocalDateTime, LocalTime}

class ChargeHistoryServiceSpec extends TestSupport with MockChargeHistoryConnector {

  object TestChargeHistoryService extends ChargeHistoryService(
    mockChargeHistoryConnector
  )

  val testChargesHistoryModel: ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", None)

  val testChargeHistoryErrorModel: ChargesHistoryErrorModel = ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure")

  val testChargeHistory: List[ChargeHistoryDetailsModel] = List(ChargeHistoryDetailsModel(
    taxYear = taxYear.toString, documentId = docNumber, documentDate = LocalDate.of(2021, 1, 1), documentDescription = "desc", totalAmount = 1000,
    reversalDate = LocalDateTime.of(LocalDate.of(taxYear + 1, 2, 14), LocalTime.of(9, 30, 45)), reversalReason = "", poaAdjustmentReason = None, chargeClassificationType = None
  ))

  val chargesHistoryWithHistory: ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", Some(testChargeHistory))

  "ChargeHistoryService.chargeHistoryResponse" should {
    "return a Right(Nil)" when {
      "the chargeHistory has no details" in {
        setupGetChargeHistory(testNino, None)(testChargesHistoryModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(None)

        res.futureValue shouldBe Right(Nil)
      }
    }
    "return an error response" when {
      "the controller returns an error" in {
        setupGetChargeHistory(testNino, None)(testChargeHistoryErrorModel)

        val res = TestChargeHistoryService.chargeHistoryResponse(None)

        res.futureValue shouldBe Left(testChargeHistoryErrorModel)
      }
    }
    "return a valid ChargesHistoryModel" when {
      "the controller returns a valid charge history" in {
        setupGetChargeHistory(testNino, None)(chargesHistoryWithHistory)

        val res = TestChargeHistoryService.chargeHistoryResponse(None)

        res.futureValue shouldBe Right(testChargeHistory)
      }
    }
  }

}
