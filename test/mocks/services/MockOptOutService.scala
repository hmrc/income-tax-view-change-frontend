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

import models.itsaStatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponse
import models.optout._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.reportingfreq.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import services.optout.{OptOutProposition, OptOutService}
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptOutService extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutService: OptOutService = mock(classOf[OptOutService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutService)
  }

  def mockGetNextUpdatesPageOptOutViewModels(out: Future[(NextUpdatesQuarterlyReportingContentChecks, Option[OptOutViewModel])]): Unit = {
    when(mockOptOutService.nextUpdatesPageOptOutViewModels()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockRecallNextUpdatesPageOneYearOptOutViewModel(out: Future[Option[OptOutOneYearViewModel]]): Unit = {
    when(mockOptOutService.recallNextUpdatesPageOptOutViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockGetSubmissionCountForTaxYear(out: Future[QuarterlyUpdatesCountForTaxYearModel]): Unit = {
    when(mockOptOutService.getQuarterlyUpdatesCountForOfferedYears(any[OptOutProposition])(any(), any(), any())).thenReturn(out)
  }

  def mockOptOutCheckPointPageViewModel(out: Future[Option[OptOutCheckpointViewModel]]): Unit = {
    when(mockOptOutService.optOutCheckPointPageViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockOptOutConfirmedPageViewModel(out: Future[Option[ConfirmedOptOutViewModel]]): Unit = {
    when(mockOptOutService.optOutConfirmedPageViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockMakeOptOutUpdateRequest(out: Future[ITSAStatusUpdateResponse]): Unit = {
    when(mockOptOutService.makeOptOutUpdateRequest()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockRecallOptOutProposition(out: Future[OptOutProposition]): Unit = {
    when(mockOptOutService.recallOptOutProposition()(any(), any())).thenReturn(out)
  }
}
