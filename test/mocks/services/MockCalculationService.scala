/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.CalcBreakdownTestConstants._
import testConstants.NewCalcBreakdownTestConstants._
import testConstants.EstimatesTestConstants._
import models.calculation._
import models.liabilitycalculation.LiabilityCalculationResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.CalculationService
import testUtils.UnitSpec

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

  def setupMockGetCalculationNew(nino: String, taxYear: Int)(response: LiabilityCalculationResponseModel): Unit = {
    when(mockCalculationService
      .getLiabilityCalculationDetail(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetCalculationId(nino: String, taxYear: Int)(response: Either[CalculationResponseModel, String]): Unit = {
    when(mockCalculationService.getCalculationId(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(taxYear)
    )(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  def setupMockGetLatestCalculation(nino: String, idResult: Either[CalculationResponseModel, String])(response: CalculationResponseModel): Unit = {
    when(mockCalculationService.getLatestCalculation(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(idResult)
    )(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  def mockCalculationSuccess(): Unit =
    setupMockGetCalculation(testNino, testYear)(calculationDisplaySuccessModel(calculationDataSuccessModel))
  def mockCalculationSuccessFullNew(taxYear: Int = testYear): Unit =
    setupMockGetCalculationNew(testNino, taxYear)(liabilityCalculationModelSuccessFull)
  def mockCalculationSuccessMinimalNew(taxYear: Int = testYear): Unit =
    setupMockGetCalculationNew(testNino, taxYear)(liabilityCalculationModelDeductionsMinimal2)
  def mockCalculationCrystalisationSuccess(): Unit =
    setupMockGetCalculation(testNino, testYear)(calculationDisplaySuccessCrystalisationModel(calculationDataSuccessModel.copy(crystallised = true)))
  def mockCalculationError(): Unit =
    setupMockGetCalculation(testNino, testYear)(CalcDisplayError)
  def mockCalculationNotFound(): Unit =
    setupMockGetCalculation(testNino, testYear)(CalcDisplayNoDataFound)
  def mockCalculationNotFoundAgent(): Unit =
    setupMockGetCalculation(testNinoAgent, testYear)(CalcDisplayNoDataFound)
}
