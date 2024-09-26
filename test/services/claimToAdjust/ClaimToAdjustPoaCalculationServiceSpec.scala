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

package services.claimToAdjust

import mocks.connectors.MockClaimToAdjustPoaConnector
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.{ClaimToAdjustPoaError, ClaimToAdjustPoaInvalidJson, ClaimToAdjustPoaSuccess, UnexpectedError}
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, MainIncomeLower}
import models.incomeSourceDetails.TaxYear
import services.claimToAdjustPoa.ClaimToAdjustPoaCalculationService
import testConstants.BaseTestConstants.{testNino, testNinoNino, testTaxYear}
import testUtils.TestSupport

class ClaimToAdjustPoaCalculationServiceSpec extends TestSupport with MockClaimToAdjustPoaConnector{

  val testClaimToAdjustPoaCalculationService = new ClaimToAdjustPoaCalculationService(mockClaimToAdjustPoaConnector)

  val processingDate = "2024-01-31T09:27:17Z"
  val errorMessage = "Unexpected error"

  val request: ClaimToAdjustPoaRequest = ClaimToAdjustPoaRequest(
    nino = testNino,
    taxYear = TaxYear(startYear = testTaxYear, endYear = testTaxYear + 1).endYear.toString,
    amount = 2000.00,
    poaAdjustmentReason = MainIncomeLower
  )

  "recalculate method" should {
    "return a success response" when {
      "post POA was successful" in {
        setupMockPostClaimToAdjustPoa(request)(response = Right(ClaimToAdjustPoaSuccess(processingDate)))
        val result = testClaimToAdjustPoaCalculationService.
          recalculate(testNinoNino, TaxYear(startYear = testTaxYear, endYear = testTaxYear + 1), 2000.00, MainIncomeLower)

        result.futureValue shouldBe Right(())
      }
    }
    "return an error" when {
      "POA recalculation failed" in {
        setupMockPostClaimToAdjustPoa(request)(response = Left(ClaimToAdjustPoaError(errorMessage)))
        val result = testClaimToAdjustPoaCalculationService.
          recalculate(testNinoNino, TaxYear(startYear = testTaxYear, endYear = testTaxYear + 1), 2000.00, MainIncomeLower)

        result.futureValue.toString shouldBe Left(new Exception("Unexpected error")).toString
      }
    }
    "return an error" when {
      "POA recalculation failed due to json error" in {
        setupMockPostClaimToAdjustPoa(request)(response = Left(ClaimToAdjustPoaInvalidJson))
        val result = testClaimToAdjustPoaCalculationService.
          recalculate(testNinoNino, TaxYear(startYear = testTaxYear, endYear = testTaxYear + 1), 2000.00, MainIncomeLower)

        result.futureValue.toString shouldBe Left(new Exception("Invalid JSON")).toString
      }
    }
    "return an error" when {
      "POA recalculation failed due to unexpected error" in {
        setupMockPostClaimToAdjustPoa(request)(response = Left(UnexpectedError))
        val result = testClaimToAdjustPoaCalculationService.
          recalculate(testNinoNino, TaxYear(startYear = testTaxYear, endYear = testTaxYear + 1), 2000.00, MainIncomeLower)

        result.futureValue.toString shouldBe Left(new Exception("Unexpected error")).toString
      }
    }
  }

}
