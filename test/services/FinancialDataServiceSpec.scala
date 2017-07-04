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
import assets.TestConstants.CalcBreakdown._
import assets.TestConstants._
import mocks.connectors.{MockLastTaxCalculationConnector, MockCalculationDataConnector}
import utils.TestSupport

class FinancialDataServiceSpec extends TestSupport with MockLastTaxCalculationConnector with MockCalculationDataConnector {

  object TestFinancialDataService extends FinancialDataService(mockLastTaxCalculationConnector, mockCalculationDataConnector)

  "The FinancialDataService.getLastEstimatedTaxCalculation method" when {

    "a successful response is returned from the EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcSuccess)
        await(TestFinancialDataService.getLastEstimatedTaxCalculation(testNino, testYear)) shouldBe Estimates.lastTaxCalcSuccess
      }
    }

    "an Error Response is returned from the EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted EstimateTaxLiability model" in {
        setupLastTaxCalculationResponse(testNino, testYear)(Estimates.lastTaxCalcError)
        await(TestFinancialDataService.getLastEstimatedTaxCalculation(testNino, testYear)) shouldBe Estimates.lastTaxCalcError
      }
    }
  }

  "The FinancialDataService.getCalculationData method" when {

    "a successful response is returned from the CalculationDataConnector" should {

      "return a correctly formatted CalculationData model" in {
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataSuccessModel)
        await(TestFinancialDataService.getCalculationData(testNino, testTaxCalculationId)) shouldBe calculationDataSuccessModel
      }
    }

    "an Error Response is returned from the CalculationDataConnector" should {

      "return a correctly formatted CalculationDataError model" in {
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataErrorModel)
        await(TestFinancialDataService.getCalculationData(testNino, testTaxCalculationId)) shouldBe calculationDataErrorModel
      }
    }
  }
}
