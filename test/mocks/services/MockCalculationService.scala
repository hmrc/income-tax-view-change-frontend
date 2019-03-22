/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import models.calculation._
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

  def setupMockGetCalculation(nino: String, taxYear: Int)(response: CalcDisplayResponseModel): Unit =
    when(mockCalculationService
      .getCalculationDetail(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetLatestCalculation(nino: String, taxYear: Int)(response: CalculationResponseModel): Unit =
    when(mockCalculationService
      .getLatestCalculation(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def setupMockGetAllLatestCalculations(nino: String, orderedYears: List[Int])(response: List[CalculationResponseModelWithYear]): Unit =
    when(mockCalculationService
      .getAllLatestCalculations(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(orderedYears)
      )(ArgumentMatchers.any()))
    .thenReturn(Future.successful(response))

  def mockCalculationSuccess(): Unit =
    setupMockGetCalculation(testNino, testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
  def mockCalculationCrystalisationSuccess(): Unit =
    setupMockGetCalculation(testNino, testYear)(calculationDisplaySuccessCrystalisationModel(calculationDataSuccessModel))
  def mockCalculationNoBreakdown(): Unit =
    setupMockGetCalculation(testNino, testYear)(calculationDisplayNoBreakdownModel)
  def mockCalculationError(): Unit =
    setupMockGetCalculation(testNino, testYear)(CalcDisplayError)
  def mockCalculationNotFound(): Unit =
    setupMockGetCalculation(testNino, testYear)(CalcDisplayNoDataFound)
  def mockGetAllLatestCalcSuccess():Unit =
    setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearList)
  def mockGetAllLatestCrystallisedCalcSuccess():Unit =
    setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearCrystallisedList)
  def mockGetAllLatestCrystallisedCalcWithError():Unit =
    setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(lastTaxCalcWithYearListWithError)
  def mockGetAllLatestCalcSuccessEmpty():Unit =
    setupMockGetAllLatestCalculations(testNino, List(testYear, testYearPlusOne))(List())
  def mockLatestCalculationSuccess(): Unit =
    setupMockGetLatestCalculation(testNino, testYear)(testCalcModelEstimate)
  def mockLatestCalculationCrystallisationSuccess(): Unit =
    setupMockGetLatestCalculation(testNino, testYear)(testCalcModelCrystallised)
  def mockLatestCalculationError(): Unit =
    setupMockGetLatestCalculation(testNino, testYear)(errorCalculationModel)
  def mockLatestCalculationCrystallisationInvalidData(): Unit =
    setupMockGetLatestCalculation(testNino, testYear)(testCalcModelNoDisplayAmount)
  def mockLatestCalculationInvalidData(): Unit =
    setupMockGetLatestCalculation(testNino, testYear)(testCalcModelEmpty)
}
