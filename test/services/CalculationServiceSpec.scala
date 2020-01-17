/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import mocks.connectors.MockIndividualCalculationsConnector
import models.calculation._
import play.api.http.Status
import testUtils.TestSupport

class CalculationServiceSpec extends TestSupport with MockIndividualCalculationsConnector {

  object TestCalculationService extends CalculationService(
    mockIndividualCalculationsConnector,
    frontendAppConfig
  )

  private trait CalculationDataApiEnabled {
    frontendAppConfig.features.calcDataApiEnabled(true)
  }

  private trait CalculationDataApiDisabled {
    frontendAppConfig.features.calcDataApiEnabled(false)
  }

  "The CalculationService.getAllLatestCalculations method" when {

    "when the  Calculation Data api feature switch is disabled" should {

      "passed an ordered list of years" should {

        "for a list of Estimates" should {

          "return a list of CalculationResponseModelWithYear models" in new CalculationDataApiDisabled {
            mockGetLatestCalculationId(testNino, "2017-18")(Right("testIdOne"))
            mockGetCalculation(testNino, "testIdOne")(lastTaxCalcSuccess)
            mockGetLatestCalculationId(testNino, "2018-19")(Right("testIdTwo"))
            mockGetCalculation(testNino, "testIdTwo")(lastTaxCalcSuccess)

            await(TestCalculationService.getAllLatestCalculations(testNino, List(testYear, testYearPlusOne))) shouldBe lastTaxCalcWithYearList
          }
        }

        "for a list of Bills" should {

          "return a list of CalculationResponseModelWithYear bills models" in new CalculationDataApiDisabled {
            mockGetLatestCalculationId(testNino, "2017-18")(Right("testIdOne"))
            mockGetCalculation(testNino, "testIdOne")(lastTaxCalcCrystallisedSuccess)
            mockGetLatestCalculationId(testNino, "2018-19")(Right("testIdTwo"))
            mockGetCalculation(testNino, "testIdTwo")(lastTaxCalcCrystallisedSuccess)

            await(TestCalculationService.getAllLatestCalculations(testNino, List(testYear, testYearPlusOne))) shouldBe lastTaxCalcWithYearCrystallisedList
          }
        }
      }

      "passed an empty list of Ints" in new CalculationDataApiDisabled {
        await(TestCalculationService.getAllLatestCalculations(testNino, List())) shouldBe List()
      }
    }
  }

  "The CalculationService.getCalculationDetail method" when {

    "when the  Calculation Data api feature switch is enabled" should {

      "successful response is returned from the IndividualCalculationConnector" should {

        "return a CalculationModel" in new CalculationDataApiEnabled{
          mockGetLatestCalculationId(testNino, "2017-18")(Right("testIdOne"))
          mockGetCalculation(testNino, "testIdOne")(calculationDataSuccessModel)

          await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe calculationDisplaySuccessCrystalisationModel(calculationDataSuccessModel)

        }

        "error response is returned from the IndividualCalculationConnector" should {
          "return a CalculationErrorModel" in new CalculationDataApiEnabled {
            mockGetLatestCalculationId(testNino, "2017-18")(
              Left(CalculationErrorModel(Status.INTERNAL_SERVER_ERROR, "Internal server error"))
            )

            await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe CalcDisplayError
          }
        }
      }
    }
  }
}
