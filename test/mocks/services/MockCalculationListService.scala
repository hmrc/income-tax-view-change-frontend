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

package mocks.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.CalculationListService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockCalculationListService extends UnitSpec with BeforeAndAfterEach {

  val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])

  def setupMockTaxYearNotCrystallised(): Unit =
    when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.any())(any(), any()))
      .thenReturn(Future.successful(Some(false)))

  def setupMockTaxYearCrystallised(): Unit =
    when(mockCalculationListService.isTaxYearCrystallised(ArgumentMatchers.any())(any(), any()))
      .thenReturn(Future.successful(Some(true)))
}
