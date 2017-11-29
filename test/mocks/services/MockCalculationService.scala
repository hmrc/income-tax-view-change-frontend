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

package mocks.services

import assets.TestConstants.CalcBreakdown.{calculationDataSuccessModel, calculationDisplayNoBreakdownModel, calculationDisplaySuccessModel}
import assets.TestConstants.Estimates.testYear
import assets.TestConstants.testNino
import models.{LastTaxCalculationResponseModel, CalcDisplayError, CalcDisplayNoDataFound, CalcDisplayResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import services.CalculationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


trait MockCalculationService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockFinancialDataService: CalculationService = mock[CalculationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDataService)
  }

  def setupMockGetFinancialData(nino: String, taxYear: Int)(response: CalcDisplayResponseModel): Unit =
    when(mockFinancialDataService
      .getFinancialData(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetLastEstimatedTaxCalculation(nino: String, year: Int)(response: LastTaxCalculationResponseModel): Unit =
    when(mockFinancialDataService
      .getLastEstimatedTaxCalculation(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(year)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def mockFinancialDataSuccess(): Unit = setupMockGetFinancialData(testNino, testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
  def mockFinancialDataNoBreakdown(): Unit = setupMockGetFinancialData(testNino, testYear)(calculationDisplayNoBreakdownModel)
  def mockFinancialDataError(): Unit = setupMockGetFinancialData(testNino, testYear)(CalcDisplayError)
  def mockFinancialDataNotFound(): Unit = setupMockGetFinancialData(testNino, testYear)(CalcDisplayNoDataFound)
}
