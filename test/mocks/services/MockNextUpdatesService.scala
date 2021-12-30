/*
 * Copyright 2021 HM Revenue & Customs
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

import implicits.ImplicitDateFormatter
import models.nextUpdates.{NextUpdatesErrorModel, NextUpdatesResponseModel}
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import services.NextUpdatesService
import testConstants.IncomeSourcesWithDeadlinesTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future


trait MockNextUpdatesService extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ImplicitDateFormatter {

  val mockNextUpdatesService: NextUpdatesService = mock[NextUpdatesService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNextUpdatesService)
  }

  def setupMockNextUpdatesResult()(response: NextUpdatesResponseModel): Unit = {
    when(mockNextUpdatesService.getNextUpdates(any())(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessError(): Unit = setupMockNextUpdatesResult()(
    NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockPropertyError(): Unit = setupMockNextUpdatesResult()(
    NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockSingleBusinessIncomeSourceWithDeadlines(): Unit = setupMockNextUpdatesResult()(singleBusinessIncomeWithDeadlines)

  def mockPropertyIncomeSourceWithDeadlines(): Unit = setupMockNextUpdatesResult()(propertyIncomeOnlyWithDeadlines)

  def mockBothIncomeSourcesWithDeadlines(): Unit = setupMockNextUpdatesResult()(businessAndPropertyIncomeWithDeadlines)

  def mockNoIncomeSourcesWithDeadlines(): Unit = setupMockNextUpdatesResult()(noIncomeDetailsWithNoDeadlines)

  def mockBothIncomeSourcesBusinessAlignedWithDeadlines(): Unit = setupMockNextUpdatesResult()(businessAndPropertyAlignedWithDeadlines)

  def mockErrorIncomeSourceWithDeadlines(): Unit = setupMockNextUpdatesResult()(NextUpdatesErrorModel(500, "error"))

  def mockGetObligationDueDates(response: Future[Either[(LocalDate, Boolean), Int]]): Unit = {
    when(mockNextUpdatesService.getObligationDueDates()(any(), any(), any()))
      .thenReturn(response)
  }

  def mockgetNextUpdates(fromDate: LocalDate, toDate: LocalDate)(response: NextUpdatesResponseModel): Unit = {
    when(mockNextUpdatesService.getNextUpdates(matches(fromDate), matches(toDate))(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def mockNextDeadlineDueDateAndOverDueObligations()(response: (LocalDate, Seq[LocalDate])): Unit = {
    when(mockNextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(response)
  }
}
