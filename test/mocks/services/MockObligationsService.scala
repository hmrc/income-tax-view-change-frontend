/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants.BusinessDetails.businessIncomeModel
import assets.TestConstants.PropertyIncome.propertyIncomeModel
import assets.TestConstants.testNino
import models._
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

  def setupMockBusinessReportDeadlinesResult(nino: String, businessIncome: Option[BusinessIncomeModel])(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getBusinessReportDeadlines(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(businessIncome))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockPropertyReportDeadlinesResult(nino: String, propertyIncome: Option[PropertyIncomeModel])(response: ReportDeadlinesResponseModel): Unit = {
    when(mockReportDeadlinesService.getPropertyReportDeadlines(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(propertyIncome))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessSuccess(): Unit = setupMockBusinessReportDeadlinesResult(testNino, Some(businessIncomeModel))(
    ReportDeadlinesModel(
      List(
        ObligationModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ObligationModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ObligationModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ObligationModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )
  def mockBusinessError(): Unit = setupMockBusinessReportDeadlinesResult(testNino, Some(businessIncomeModel))(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockNoBusinessIncome(): Unit = setupMockBusinessReportDeadlinesResult(testNino, None)(NoReportDeadlines)

  def mockPropertySuccess(): Unit = setupMockPropertyReportDeadlinesResult(testNino, Some(propertyIncomeModel))(
    ReportDeadlinesModel(
      List(
        ObligationModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ObligationModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ObligationModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ObligationModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )
  def mockPropertyError(): Unit = setupMockPropertyReportDeadlinesResult(testNino, Some(propertyIncomeModel))(
    ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockNoPropertyIncome(): Unit = setupMockPropertyReportDeadlinesResult(testNino, None)(NoReportDeadlines)
}
