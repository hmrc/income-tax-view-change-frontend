/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.TestConstants.{testNino, testSelfEmploymentId}
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import services.ReportDeadlinesService
import uk.gov.hmrc.play.test.UnitSpec
import utils.ImplicitDateFormatter

import scala.concurrent.Future


trait MockReportDeadlinesService extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ImplicitDateFormatter {

  val mockReportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportDeadlinesService)
  }

  def setupMockBusinessReportDeadlinesResult(nino: String, selfEmploymentId: String)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getBusinessReportDeadlines(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(selfEmploymentId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockPropertyReportDeadlinesResult(nino: String)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getPropertyReportDeadlines(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessSuccess(): Unit = setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(
    ReportDeadlinesModel(
      List(
        ReportDeadlineModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ReportDeadlineModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ReportDeadlineModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ReportDeadlineModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )

  def mockBusinessError(): Unit = setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockPropertySuccess(): Unit = setupMockPropertyReportDeadlinesResult(testNino)(
    ReportDeadlinesModel(
      List(
        ReportDeadlineModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ReportDeadlineModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ReportDeadlineModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ReportDeadlineModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )

  def mockPropertyError(): Unit = setupMockPropertyReportDeadlinesResult(testNino)(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )
}
