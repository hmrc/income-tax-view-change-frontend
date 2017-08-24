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

import assets.TestConstants.Obligations._
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.PropertyIncome._
import assets.TestConstants.Estimates._
import assets.TestConstants._
import assets.TestConstants.IncomeSourceDetails._
import mocks.services.{MockObligationsService, MockFinancialDataService}
import models.{ObligationsModel, ObligationsErrorModel, ObligationModel}
import utils.TestSupport

class BTAPartialServiceSpec extends TestSupport with MockFinancialDataService with MockObligationsService {

  object TestBTAPartialService extends BTAPartialService(mockObligationsService, mockFinancialDataService)

  "The BTAPartialService getObligations method" when {

    "both property and business obligations are returned - business due before property" should {
      val returnedObligation = ObligationModel(
        start = "2017-7-1",
        end = "2017-9-30",
        due = "2017-10-30",
        met = false
      )
      val otherObligation = ObligationModel(
        start = "2017-7-1",
        end = "2017-9-30",
        due = "2017-10-31",
        met = false
      )
      "return an ObligationModel" in {
        setupMockBusinessObligationsResult(testNino, Some(businessIncomeModelAlignedTaxYear))(ObligationsModel(List(otherObligation, returnedObligation)))
        mockPropertySuccess()
        await(TestBTAPartialService.getObligations(testNino, bothIncomeSourcesSuccessBusinessAligned)) shouldBe returnedObligation
      }
    }

    "both property and business obligations are returned - property due before business" should {
      val returnedObligation = ObligationModel(
        start = "2017-1-1",
        end = "2017-2-1",
        due = "2017-3-1",
        met = false
      )
      "return an ObligationModel" in {
        setupMockBusinessObligationsResult(testNino, Some(businessIncomeModelAlignedTaxYear))(obligationsDataSuccessModel)
        setupMockPropertyObligationsResult(testNino, Some(propertyIncomeModel))(ObligationsModel(List(returnedObligation)))
        await(TestBTAPartialService.getObligations(testNino, bothIncomeSourcesSuccessBusinessAligned)) shouldBe returnedObligation
      }
    }

    "both obligations are returned - Received Business obligation returned" should {
      val returnedObligation = ObligationModel(
        start = "2017-6-1",
        end = "2017-7-1",
        due = "2017-8-1",
        met = true
      )
      val otherObligation = ObligationModel(
        start = "2017-6-1",
        end = "2017-7-1",
        due = "2017-7-30",
        met = true
      )
      val obligations = ObligationsModel(List(receivedObligation, returnedObligation, otherObligation))
      "return an ObligationModel" in {
        setupMockBusinessObligationsResult(testNino, Some(businessIncomeModelAlignedTaxYear))(obligations)
        setupMockPropertyObligationsResult(testNino, Some(propertyIncomeModel))(ObligationsModel(List(receivedObligation)))
        await(TestBTAPartialService.getObligations(testNino, bothIncomeSourcesSuccessBusinessAligned)) shouldBe returnedObligation
      }
    }

    "both obligations are returned - Received Property obligation returned" should {
      val returnedObligation = ObligationModel(
        start = "2017-6-1",
        end = "2017-7-1",
        due = "2017-8-1",
        met = true
      )
      val otherObligation = ObligationModel(
        start = "2017-6-1",
        end = "2017-7-1",
        due = "2017-7-30",
        met = true
      )
      val obligations = ObligationsModel(List(receivedObligation, returnedObligation, otherObligation))
      "return an ObligationModel" in {
        setupMockBusinessObligationsResult(testNino, Some(businessIncomeModelAlignedTaxYear))(ObligationsModel(List(receivedObligation)))
        setupMockPropertyObligationsResult(testNino, Some(propertyIncomeModel))(obligations)
        await(TestBTAPartialService.getObligations(testNino, bothIncomeSourcesSuccessBusinessAligned)) shouldBe returnedObligation
      }
    }

    "only business obligations are returned" should {
      val returnedObligation = ObligationModel(
        start = "2017-10-06",
        end = "2018-01-05",
        due = "2018-02-05",
        met = false
      )
      "return an ObligationModel" in {
        mockBusinessSuccess()
        mockNoPropertyIncome()
        await(TestBTAPartialService.getObligations(testNino, businessIncomeSourceSuccess)) shouldBe returnedObligation
      }
    }

    "only property obligations are returned" should {
      val returnedObligation = ObligationModel(
        start = "2017-10-06",
        end = "2018-01-05",
        due = "2018-02-05",
        met = false
      )
      "return an ObligationModel" in {
        mockNoBusinessIncome()
        mockPropertySuccess()
        await(TestBTAPartialService.getObligations(testNino, propertyIncomeSourceSuccess)) shouldBe returnedObligation
      }
    }

    "no obligations are returned" in {
      mockNoPropertyIncome()
      mockNoBusinessIncome()
      await(TestBTAPartialService.getObligations(testNino, noIncomeSourceSuccess)) shouldBe ObligationsErrorModel(500,"Could not retrieve obligations")
    }
  }

  "The BTAPartialService getEstimate method" when {
    "a valid LastTaxCalculation is returned from the FinancialDataService" should {
      "return LastTaxCalc model" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcSuccess)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcSuccess
      }
    }
    "NoLastTaxCalculation is returned from the FinancialDataService" should {
      "return NoLastTaxCalc" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcNotFound)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcNotFound
      }
    }
    "LastTaxCalculationError is returned from the FinancialDataService" should {
      "return LastTaxCalcError" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcError)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcError
      }
    }
  }

}
