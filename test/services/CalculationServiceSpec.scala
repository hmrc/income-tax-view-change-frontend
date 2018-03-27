/*
 * Copyright 2018 HM Revenue & Customs
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
import mocks.connectors.{MockCalculationDataConnector, MockLastTaxCalculationConnector}
import mocks.services.MockCalculationService
import models.calculation.{CalcDisplayError, CalcDisplayNoDataFound, LastTaxCalculationResponseModel}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestSupport

import scala.concurrent.Future

class CalculationServiceSpec extends TestSupport with MockLastTaxCalculationConnector with MockCalculationDataConnector with MockCalculationService {

  object TestCalculationService extends CalculationService(mockLastTaxCalculationConnector, mockCalculationDataConnector)

  "The CalculationService.getCalculationData method" when {

    "successful responses are returned from the CalculationDataConnector & EstimatedTaxLiabilityConnector" should {

      "return a correctly formatted CalculationData model" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcSuccess)
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataSuccessModel)

        await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe calculationDisplaySuccessModel(calculationDataSuccessModel)
      }
    }

    "an Error Response is returned from the EstimatedTaxLiabilityConnector" should {

      "return none" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcError)
        await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe CalcDisplayError
      }
    }

    "a Not Found Response is returned from the EstimatedTaxLiabilityConnector" should {

      "return none" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcNotFound)
        await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe CalcDisplayNoDataFound
      }
    }

    "an Error Response is returned from the CalculationDataConnector" should {

      "return a correctly formatted CalcDisplayModel model with calcDataModel = None" in {
        setupLastTaxCalculationResponse(testNino, testYear)(lastTaxCalcSuccess)
        setupCalculationDataResponse(testNino, testTaxCalculationId)(calculationDataErrorModel)

        await(TestCalculationService.getCalculationDetail(testNino, testYear)) shouldBe calculationDisplayNoBreakdownModel
      }
    }
  }

  "The CalculationService.getAllLatestCalculations method" when {

    object TestCalculationService extends CalculationService(mockLastTaxCalculationConnector, mockCalculationDataConnector) {
      override def getLastEstimatedTaxCalculation(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {
        lastTaxCalcSuccess
      }
    }

    "passed an ordered list of years" should {

      "return a list of LastTaxCalculationWithYear models" in {
        await(TestCalculationService.getAllLatestCalculations(testNino, List(testYear, testYearPlusOne))) shouldBe lastTaxCalcWithYearList
      }

      "passed an empty list of Ints" in {
        await(TestCalculationService.getAllLatestCalculations(testNino, List())) shouldBe List()
      }

    }

  }
}
