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

import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponse
import models.incomeSourceDetails.TaxYear
import models.optout._
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.optout.OptOutService
import services.optout.OptOutService.QuarterlyUpdatesCountForTaxYearModel
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptOutService extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutService: OptOutService = mock(classOf[OptOutService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutService)
  }

  def mockNextUpdatesPageOneYearOptOutViewModel(out: Future[Option[OptOutOneYearViewModel]]): Unit = {
    when(mockOptOutService.nextUpdatesPageOptOutViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockNextUpdatesPageMultiYearOptOutViewModel(out: Future[Option[OptOutMultiYearViewModel]]): Unit = {
    when(mockOptOutService.nextUpdatesPageOptOutViewModel()(any(), any(), any())).thenReturn(out)
  }

  def mockGetTaxYearsAvailableForOptOut(out: Future[Seq[TaxYear]]): Unit = {
    when(mockOptOutService.getTaxYearsAvailableForOptOut()(any(), any(), any())).thenReturn(out)
  }

  def mockGetSubmissionCountForTaxYear(in: Seq[TaxYear], out: Future[QuarterlyUpdatesCountForTaxYearModel]): Unit = {
    when(mockOptOutService.getQuarterlyUpdatesCountForTaxYear(same(in))(any(), any(), any())).thenReturn(out)
  }

  def mockOptOutCheckPointPageViewModel(out: Future[Option[OptOutCheckpointViewModel]]): Unit = {
    when(mockOptOutService.optOutCheckPointPageViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockOptOutConfirmedPageViewModel(out: Future[Option[ConfirmedOptOutViewModel]]): Unit = {
    when(mockOptOutService.optOutConfirmedPageViewModel()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockMakeOptOutUpdateRequest(out: Future[OptOutUpdateResponse]): Unit = {
    when(mockOptOutService.makeOptOutUpdateRequest()(any(), any(), any()))
      .thenReturn(out)
  }

  def mockSaveIntent(in: TaxYear, out: Future[Boolean]): Unit = {
    when(mockOptOutService.saveIntent(any[TaxYear])(any())).thenReturn(out)
  }

  def mockFetchIntent(out: Future[Option[TaxYear]]): Unit = {
    when(mockOptOutService.fetchSavedIntent()(any(), any())).thenReturn(out)
  }

  def mockNextUpdatesPageOptOutWithChecks(out: Future[(Option[OptOutViewModel], NextUpdatesQuarterlyReportingContentChecks)]): Unit = {
    when(mockOptOutService.nextUpdatesPageOptOutWithChecks()(any(), any(), any())).thenReturn(out)
  }
}
