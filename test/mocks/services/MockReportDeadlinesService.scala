/*
 * Copyright 2020 HM Revenue & Customs
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

import assets.BaseTestConstants.{testPropertyIncomeId, testSelfEmploymentId}
import assets.IncomeSourceDetailsTestConstants._
import assets.IncomeSourcesWithDeadlinesTestConstants._
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourcesWithDeadlines.{IncomeSourcesWithDeadlinesError, IncomeSourcesWithDeadlinesResponse}
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import org.mockito.ArgumentMatchers
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

  def setupMockReportDeadlinesResult(incomeSourceId: String)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getReportDeadlines()(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessError(): Unit = setupMockReportDeadlinesResult(testSelfEmploymentId)(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockPropertyError(): Unit = setupMockReportDeadlinesResult(testPropertyIncomeId)(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )


  def setupMockGetIncomeSourceWithDeadlines(incomeSourceDetails: IncomeSourceDetailsModel)(sources: IncomeSourcesWithDeadlinesResponse): Unit = {
    when(
      mockReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(
        ArgumentMatchers.eq(incomeSourceDetails), ArgumentMatchers.anyBoolean()
      )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(sources))
  }

  def mockSingleBusinessIncomeSourceWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(singleBusinessIncome)(singleBusinessIncomeWithDeadlines)
  def mockPropertyIncomeSourceWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(propertyIncomeOnly)(propertyIncomeOnlyWithDeadlines)
  def mockBothIncomeSourcesWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(businessesAndPropertyIncome)(businessAndPropertyIncomeWithDeadlines)
  def mockNoIncomeSourcesWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(singleBusinessIncome)(noIncomeDetailsWithNoDeadlines)
  def mockBothIncomeSourcesBusinessAlignedWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(businessAndPropertyAligned)(businessAndPropertyAlignedWithDeadlines)
  def mockErrorIncomeSourceWithDeadlines(): Unit = setupMockGetIncomeSourceWithDeadlines(singleBusinessIncome)(IncomeSourcesWithDeadlinesError)
}
