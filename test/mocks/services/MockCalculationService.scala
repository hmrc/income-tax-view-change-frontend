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

import assets.TestConstants.CalcBreakdown.{calculationDataSuccessModel, calculationDisplayNoBreakdownModel, calculationDisplaySuccessCrystalisationModel, calculationDisplaySuccessModel}
import assets.TestConstants.Estimates._
import assets.TestConstants.testNino
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import services.CalculationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


trait MockCalculationService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockCalculationService: CalculationService = mock[CalculationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationService)
  }

  def setupMockGetFinancialData(nino: String, taxYear: Int)(response: CalcDisplayResponseModel): Unit =
    when(mockCalculationService
      .getFinancialData(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetLastEstimatedTaxCalculation(nino: String, year: Int)(response: LastTaxCalculationResponseModel): Unit =
    when(mockCalculationService
      .getLastEstimatedTaxCalculation(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(year)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetAllLatestCalculations(nino: String, orderedYears: List[Int])(response: List[LastTaxCalculationWithYear]): Unit =
    when(mockCalculationService
      .getAllLatestCalculations(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(orderedYears)
      )(ArgumentMatchers.any()))
    .thenReturn(Future.successful(response))

  def mockFinancialDataSuccess(): Unit = setupMockGetFinancialData(testNino, testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
  def mockFinancialDataCrystalisationSuccess(): Unit = setupMockGetFinancialData(testNino, testYear)(calculationDisplaySuccessCrystalisationModel(calculationDataSuccessModel))
  def mockFinancialDataNoBreakdown(): Unit = setupMockGetFinancialData(testNino, testYear)(calculationDisplayNoBreakdownModel)
  def mockFinancialDataError(): Unit = setupMockGetFinancialData(testNino, testYear)(CalcDisplayError)
  def mockFinancialDataNotFound(): Unit = setupMockGetFinancialData(testNino, testYear)(CalcDisplayNoDataFound)
  def mockGetAllLatestCalcSuccess():Unit = setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearList)
  def mockGetAllLatestCrystallisedCalcSuccess():Unit = setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearCrystallisedList)
  def mockGetAllLatestCrystallisedCalcWithError():Unit = setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearListWithError)
  def mockGetAllLatestCrystallisedCalcWithCalcNotFound():Unit = setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearListWithCalcNotFound)
  def mockGetAllLatestCalcSuccessEmpty():Unit = setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(List())

}
