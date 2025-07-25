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

import models.incomeSourceDetails.{LatencyDetails, TaxYear}
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.itsaStatus.StatusDetail
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.{ITSAStatusService, PenaltyDetailsService}
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockPenaltyDetailsService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockPenaltyDetailsService: PenaltyDetailsService = mock(classOf[PenaltyDetailsService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPenaltyDetailsService)
  }

  def setupMockGetPenaltyDetailsCount(enabled: Boolean)(out: Future[Int]): Unit = {
    when(mockPenaltyDetailsService.getPenaltiesCount(ArgumentMatchers.eq(enabled))(any, any, any))
      .thenReturn(out)
  }

  def setupMockGetPenaltySubmissionFrequency(status: ITSAStatus)(out: String): Unit = {
    when(mockPenaltyDetailsService.getPenaltySubmissionFrequency(ArgumentMatchers.eq(status)))
      .thenReturn(out)
  }
}
