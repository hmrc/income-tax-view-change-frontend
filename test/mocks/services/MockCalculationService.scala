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

import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.CalculationService
import testConstants.BaseTestConstants._
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockCalculationService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockCalculationService: CalculationService = mock[CalculationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationService)
  }

  def setupMockGetCalculationNew(mtditid: String, nino: String, taxYear: Int)(response: LiabilityCalculationResponseModel): Unit = {
    when(mockCalculationService
      .getLiabilityCalculationDetail(
        ArgumentMatchers.eq(mtditid),
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxYear)
      )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetLatestCalculation(mtditid: String, nino: String, calcId: String)(response: LiabilityCalculationResponseModel): Unit = {
    when(mockCalculationService.getLatestCalculation(
      ArgumentMatchers.eq(mtditid),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(calcId)
    )(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  def mockCalculationSuccessFullNew(mtditid: String = "XAIT00000000015", nino: String = testNino, taxYear: Int = testTaxYear): Unit =
    setupMockGetCalculationNew(mtditid, nino, taxYear)(liabilityCalculationModelSuccessFull)

  def mockCalculationSuccessMinimalNew(mtditid: String = "XAIT00000000015", taxYear: Int = testTaxYear): Unit =
    setupMockGetCalculationNew(mtditid, testNino, taxYear)(liabilityCalculationModelDeductionsMinimal())

  def mockCalculationErrorNew(mtditid: String = "XAIT00000000015", nino: String = testNino, year: Int = testTaxYear): Unit =
    setupMockGetCalculationNew(mtditid, nino, year)(LiabilityCalculationError(500, "Internal server error"))

  def mockCalculationNotFoundNew(mtditid: String = "XAIT00000000015", nino: String = testNino, year: Int = testTaxYear): Unit =
    setupMockGetCalculationNew(mtditid, nino, year)(LiabilityCalculationError(404, "not found"))
}
