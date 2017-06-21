/*
 * Copyright 2017 HM Revenue & Customs
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


import assets.TestConstants.Estimates._
import assets.TestConstants.Estimates
import assets.TestConstants._
import mocks.connectors.MockLastTaxCalculationConnector
import utils.TestSupport

class EstimatedTaxLiabilityServiceSpec extends TestSupport with MockLastTaxCalculationConnector {

  object TestEstimatedTaxLiabilityService extends EstimatedTaxLiabilityService(mockLastTaxCalculationConnector)

  "The EstimatedTaxLiabilityService.getEstimatedTaxLiability method" when {

    "a successful response is returned from the EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupLastTaxCalculationResponse(testNino, testYear, testCalcType)(lastTaxCalcSuccess)
        await(TestEstimatedTaxLiabilityService.getLastEstimatedTaxCalculation(testNino, testYear, testCalcType)) shouldBe Estimates.lastTaxCalcSuccess
      }
    }

    "an Error Response is returned from the FinancialDataConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupLastTaxCalculationResponse(testNino, testYear, testCalcType)(Estimates.lastTaxCalcError)
        await(TestEstimatedTaxLiabilityService.getLastEstimatedTaxCalculation(testNino, testYear, testCalcType)) shouldBe Estimates.lastTaxCalcError
      }
    }
  }
}
