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
import mocks.connectors.{MockCalculationDataConnector, MockLastTaxCalculationConnector}
import models.{CalcDisplayError, CalcDisplayNoDataFound}
import utils.TestSupport

class CalculationServiceSpec extends TestSupport with MockLastTaxCalculationConnector with MockCalculationDataConnector {

  object TestCalculationService$ extends CalculationService(mockLastTaxCalculationConnector, mockCalculationDataConnector)

  "The FinancialDataService.getCalculationData method" when {

    "successful responses are returned from the CalculationDataConnector & EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted CalculationData model" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcSuccess)
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataSuccessModel)

        await(TestCalculationService$.getFinancialData(testNino, testYear)) shouldBe calculationDisplaySuccessModel(calculationDataSuccessModel)
      }
    }

    "an Error Response is returned from the EstimatedTaxLiabilityConnector" should {

      "return none" in {
        setupLastTaxCalculationResponse(testNino, testYear)(Estimates.lastTaxCalcError)
        await(TestCalculationService$.getFinancialData(testNino, testYear)) shouldBe CalcDisplayError
      }
    }

    "a Not Found Response is returned from the EstimatedTaxLiabilityConnector" should {

      "return none" in {
        setupLastTaxCalculationResponse(testNino, testYear)(Estimates.lastTaxCalcNotFound)
        await(TestCalculationService$.getFinancialData(testNino, testYear)) shouldBe CalcDisplayNoDataFound
      }
    }

    "an Error Response is returned from the CalculationDataConnector" should {

      "return a correctly formatted CalcDisplayModel model with calcDataModel = None" in {
        setupLastTaxCalculationResponse(testNino, testYear)(Estimates.lastTaxCalcSuccess)
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataErrorModel)

        await(TestCalculationService$.getFinancialData(testNino, testYear)) shouldBe calculationDisplayNoBreakdownModel
      }
    }
  }
}
