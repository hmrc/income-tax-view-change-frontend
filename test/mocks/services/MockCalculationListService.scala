/*
 * Copyright 2024 HM Revenue & Customs
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

import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.CalculationListService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockCalculationListService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationListService)
  }
  def setupMockTaxYearNotCrystallised(): Unit =
    when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.anyInt())(any(), any()))
      .thenReturn(Future.successful(false))

  def setupMockIsTaxYearCrystallisedCall(taxYear: TaxYear)(out: Future[Boolean]): Unit = {
    when(mockCalculationListService
      .isTaxYearCrystallised(ArgumentMatchers.eq(taxYear))(any, any))
      .thenReturn(out)
  }
  def setupMockTaxYearCrystallised(): Unit =
    when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.anyInt())(any(), any()))
      .thenReturn(Future.successful(true))

  def setupMockTaxYearCrystallised(year: Int): Unit =
    when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(year))(any(), any()))
      .thenReturn(Future.successful(true))

  def setupMockTaxYearNotCrystallised(year: Int): Unit =
    when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(year))(any(), any()))
      .thenReturn(Future.successful(false))
}
