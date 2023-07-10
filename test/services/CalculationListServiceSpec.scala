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

package services

import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.calculationList.CalculationListErrorModel
import models.core.Nino
import play.api.http.Status.{IM_A_TEAPOT, NOT_FOUND}
import testConstants.BaseTestConstants.{testNino, testTaxYear, testTaxYearRange}
import testConstants.CalculationListTestConstants
import testUtils.TestSupport


class CalculationListServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {
  object TestCalculationListService extends CalculationListService(mockIncomeTaxViewChangeConnector)

  val notFoundText = "The remote endpoint has indicated that the requested resource could not be found."

  "getLegacyCalculationList (API 1404)" should {
    "return a CalculationListModel" when {
      "a success response is received from the connector" in {
        setupGetLegacyCalculationList(testNino, testTaxYear.toString)(CalculationListTestConstants.calculationListFull)
        TestCalculationListService.getLegacyCalculationList(Nino(testNino), testTaxYear.toString).futureValue shouldBe
          CalculationListTestConstants.calculationListFull
      }
      "return a CalculationListErrorModel" when {
        "an error response is received from the connector" in {
          setupGetLegacyCalculationList(testNino, testTaxYear.toString)(CalculationListErrorModel(NOT_FOUND, notFoundText))
          TestCalculationListService.getLegacyCalculationList(Nino(testNino), testTaxYear.toString).futureValue shouldBe
            CalculationListErrorModel(NOT_FOUND, notFoundText)
        }
      }
    }
  }
  "getCalculationList (API 1896)" should {
    "a success response is received from the connector (including optional field `crystallised`)" in {
      setupGetCalculationList(testNino, testTaxYearRange)(CalculationListTestConstants.calculationListFull)
      TestCalculationListService.getCalculationList(Nino(testNino), testTaxYearRange).futureValue shouldBe
        CalculationListTestConstants.calculationListFull
    }
    "a success response is received from the connector (excluding optional field `crystallised`)" in {
      setupGetCalculationList(testNino, testTaxYearRange)(CalculationListTestConstants.calculationListMin)
      TestCalculationListService.getCalculationList(Nino(testNino), testTaxYearRange).futureValue shouldBe
        CalculationListTestConstants.calculationListMin
    }
    "return a CalculationListErrorModel" when {
      "an error response is received from the connector" in {
        setupGetCalculationList(testNino, testTaxYearRange)(CalculationListErrorModel(NOT_FOUND, notFoundText))
        TestCalculationListService.getCalculationList(Nino(testNino), testTaxYearRange).futureValue shouldBe
          CalculationListErrorModel(NOT_FOUND, notFoundText)
      }
    }
  }

  "isTaxYearCrystallised" when {
    "calculation list is returned for year 2022-23" should {
      val taxYearEnd = "2023"
      "returns Some(true)" in {
        setupGetLegacyCalculationList(testNino, taxYearEnd)(CalculationListTestConstants.calculationListFull)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(true)
      }
      "returns Some(false)" in {
        setupGetLegacyCalculationList(testNino, taxYearEnd)(CalculationListTestConstants.calculationListFalseFull)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(false)
      }
      "returns None" in {
        setupGetLegacyCalculationList(testNino, taxYearEnd)(CalculationListTestConstants.calculationListMin)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe None
      }
      "returns InternalServerException" in {
        val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
        setupGetLegacyCalculationList(testNino, taxYearEnd)(error)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).failed.futureValue.getMessage shouldBe error.message
      }
    }

    "calculation list is returned for year 2023-24" should {
      val taxYearEnd = "2024"
      val testTaxYearRange = "23-24"
      "returns Some(true)" in {
        setupGetCalculationList(testNino, testTaxYearRange)(CalculationListTestConstants.calculationListFull)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(true)
      }
      "returns Some(false)" in {
        setupGetCalculationList(testNino, testTaxYearRange)(CalculationListTestConstants.calculationListFalseFull)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe Some(false)
      }
      "returns None" in {
        setupGetCalculationList(testNino, testTaxYearRange)(CalculationListTestConstants.calculationListMin)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe None
      }
      "returns InternalServerException" in {
        val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
        setupGetCalculationList(testNino, testTaxYearRange)(error)
        TestCalculationListService.isTaxYearCrystallised(taxYearEnd.toInt).failed.futureValue.getMessage shouldBe error.message
      }
    }
  }

}
