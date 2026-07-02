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

package returns.services

import common.services.DateService
import common.testConstants.BaseTestConstants.{testMtditid, testNino}
import common.testUtils.TestSupport
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{IM_A_TEAPOT, NOT_FOUND}
import returns.mocks.connectors.MockCalculationListConnector
import returns.testConstants.CalculationListTestConstants
import shared.models.calculationList.CalculationListErrorModel
import shared.services.CalculationListService


class CalculationListServiceSpec extends TestSupport with MockCalculationListConnector {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestCalculationListService extends CalculationListService(mockCalculationListConnector, mockDateService)

  val taxYearEnd = 2024
  when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
  when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd + 1)
  val notFoundText = "The remote endpoint has indicated that the requested resource could not be found."

  "isTaxYearCrystallised" when {
    "for year 2022-23" should {
      val taxYearEnd = "2023"
      "returns Some(true)" in {
        setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListFull)
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe true
      }
      "returns Some(false)" in {
        setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListFalseFull)
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
      }
      "returns None" in {
        setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListMin)
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
      }
      "returns Some(false) given 404 response from 1404" in {
        val error = CalculationListErrorModel(NOT_FOUND, "not found")
        setupGetCalculationList(testNino, taxYearEnd, testMtditid)(error)
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
      }
      "returns InternalServerException" in {
        val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
        setupGetCalculationList(testNino, taxYearEnd, testMtditid)(error)
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).failed.futureValue.getMessage shouldBe error.message
      }
    }

     "for year 2023-24" should {
       val taxYearEnd = "2024"
       "returns Some(true)" in {
         setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListFull)
         TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe true
       }
       "returns Some(false)" in {
         setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListFalseFull)
         TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
       }
       "returns None" in {
         setupGetCalculationList(testNino, taxYearEnd, testMtditid)(CalculationListTestConstants.calculationListMin)
         TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
       }
       "returns Some(false) given 404 response from 1896" in {
         val error = CalculationListErrorModel(NOT_FOUND, "not found")
         setupGetCalculationList(testNino, taxYearEnd, testMtditid)(error)
         TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
       }
       "returns InternalServerException" in {
         val error = CalculationListErrorModel(IM_A_TEAPOT, "I'm a teapot")
         setupGetCalculationList(testNino, taxYearEnd, testMtditid)(error)
         TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
       }
     }
    "for year 2024-25" should {
      "returns Some(false)" in {
        val taxYearEnd = "2025"
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
      }
    }
    "for year 2025-26" should {
      "returns Some(false)" in {
        val taxYearEnd = "2026"
        TestCalculationListService.determineTaxYearCrystallised(taxYearEnd.toInt).futureValue shouldBe false
      }
    }
  }

}
