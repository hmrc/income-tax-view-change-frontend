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

import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutOneYearCheckpointViewModel, OptOutOneYearViewModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.optout.OptOutService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptOutService extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutService: OptOutService = mock(classOf[OptOutService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutService)
  }

  def mockGetNextUpdatesQuarterlyReportingContentChecks(out: Future[NextUpdatesQuarterlyReportingContentChecks]): Unit = {
    when(mockOptOutService.getNextUpdatesQuarterlyReportingContentChecks(any(), any(), any()))
      .thenReturn(out)
  }

  def mockNextUpdatesPageOneYearOptOutViewModel(out: Future[Option[OptOutOneYearViewModel]]): Unit = {
    when(mockOptOutService.nextUpdatesPageOneYearOptOutViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockOptOutCheckPointPageViewModel(out: Future[Option[OptOutOneYearCheckpointViewModel]]): Unit = {
    when(mockOptOutService.optOutCheckPointPageViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

}
