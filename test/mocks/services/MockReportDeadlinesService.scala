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

import java.time.LocalDate

import assets.IncomeSourcesWithDeadlinesTestConstants._
import implicits.ImplicitDateFormatter
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import services.ReportDeadlinesService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


trait MockReportDeadlinesService extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ImplicitDateFormatter {

  val mockReportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportDeadlinesService)
  }

  def setupMockReportDeadlinesResult()(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getReportDeadlines(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessError(): Unit = setupMockReportDeadlinesResult()(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockPropertyError(): Unit = setupMockReportDeadlinesResult()(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockSingleBusinessIncomeSourceWithDeadlines(): Unit = setupMockReportDeadlinesResult()(singleBusinessIncomeWithDeadlines)

  def mockPropertyIncomeSourceWithDeadlines(): Unit = setupMockReportDeadlinesResult()(propertyIncomeOnlyWithDeadlines)

  def mockBothIncomeSourcesWithDeadlines(): Unit = setupMockReportDeadlinesResult()(businessAndPropertyIncomeWithDeadlines)

  def mockNoIncomeSourcesWithDeadlines(): Unit = setupMockReportDeadlinesResult()(noIncomeDetailsWithNoDeadlines)

  def mockBothIncomeSourcesBusinessAlignedWithDeadlines(): Unit = setupMockReportDeadlinesResult()(businessAndPropertyAlignedWithDeadlines)

  def mockErrorIncomeSourceWithDeadlines(): Unit = setupMockReportDeadlinesResult()(ReportDeadlinesErrorModel(500, "error"))

  def mockGetObligationDueDates(response: Future[Either[(LocalDate, Boolean), Int]]): Unit = {
    when(mockReportDeadlinesService.getObligationDueDates()(any(), any(), any()))
      .thenReturn(Future.successful(response))
  }

  def mockGetReportDeadlines(fromDate: LocalDate, toDate: LocalDate)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getReportDeadlines(matches(fromDate), matches(toDate))(any(), any()))
      .thenReturn(Future.successful(response))
  }
}
