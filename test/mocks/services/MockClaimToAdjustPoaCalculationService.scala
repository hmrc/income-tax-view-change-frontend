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

import models.core.Nino
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.claimToAdjustPoa.ClaimToAdjustPoaCalculationService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockClaimToAdjustPoaCalculationService extends UnitSpec with BeforeAndAfterEach {

  val mockClaimToAdjustPoaCalculationService: ClaimToAdjustPoaCalculationService = mock(classOf[ClaimToAdjustPoaCalculationService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClaimToAdjustPoaCalculationService)
  }

  def setupMockRecalculateSuccess(): Unit =
    when(mockClaimToAdjustPoaCalculationService.recalculate(Nino(ArgumentMatchers.anyString()), any(), any(), any())(any()))
      .thenReturn(Future.successful(Right((): Unit)))

  def setupMockRecalculateFailure(): Unit =
    when(mockClaimToAdjustPoaCalculationService.recalculate(Nino(ArgumentMatchers.anyString()), any(), any(), any())(any()))
      .thenReturn(Future.successful(Left(new Exception())))

}
