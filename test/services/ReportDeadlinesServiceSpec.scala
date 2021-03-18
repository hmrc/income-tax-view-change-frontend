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

package services

import java.time.LocalDate

import assets.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import assets.IncomeSourceDetailsTestConstants.{businessAndPropertyAligned, noIncomeDetails, propertyIncomeOnly}
import assets.ReportDeadlinesTestConstants._
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class ReportDeadlinesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  class Setup extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  "getObligationDueDates" should {
    "return an internal server exception when an error model is returned from the connector" in new Setup {
      setupMockReportDeadlines(obligationsDataErrorModel)

      intercept[InternalServerException](await(getObligationDueDates()))
        .message shouldBe "Unexpected Exception getting obligation due dates"
    }
    "return a single overdue date" when {
      "the connector returns obligations with a single overdue date" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          ReportDeadlinesModel(
            identification = "testId1",
            obligations = List(
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(1), "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockReportDeadlines(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        await(result) shouldBe Left(LocalDate.now.minusDays(1) -> true)
      }
    }
    "return a count of overdue dates" when {
      "the connector returns obligations with more than one overdue date" in new Setup {
        val obligationsWithMultipleOverdue: ObligationsModel = ObligationsModel(Seq(
          ReportDeadlinesModel(
            identification = "testId1",
            obligations = List(
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(2), "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(1), "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockReportDeadlines(obligationsWithMultipleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        await(result) shouldBe Right(2)
      }
    }
    "return a single non-overdue date" when {
      "the connector returns obligations without any overdue dates" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          ReportDeadlinesModel(
            identification = "testId1",
            obligations = List(
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockReportDeadlines(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        await(result) shouldBe Left(LocalDate.now -> false)
      }
    }
  }

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockReportDeadlines(obligationsAllDeadlinesSuccessModel)
        await(getNextDeadlineDueDateAndOverDueObligations(businessAndPropertyAligned))._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockReportDeadlines(obligationsPropertyOnlySuccessModel)
        await(getNextDeadlineDueDateAndOverDueObligations(propertyIncomeOnly))._1  shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockReportDeadlines(obligationsCrystallisedOnlySuccessModel)
        await(getNextDeadlineDueDateAndOverDueObligations(noIncomeDetails))._1  shouldBe LocalDate.of(2017, 10, 31)
      }

      "there are no deadlines available" in new Setup {
        setupMockReportDeadlines(emptyObligationsSuccessModel)
        the[Exception] thrownBy await(getNextDeadlineDueDateAndOverDueObligations(noIncomeDetails)) should have message "Unexpected Exception getting next deadline due and Overdue Obligations"
      }

      "the report deadlines returned back an error model" in new Setup {
        setupMockReportDeadlines(obligationsDataErrorModel)
        the[Exception] thrownBy await(getNextDeadlineDueDateAndOverDueObligations(noIncomeDetails)) should have message "Dummy Error Message"
      }
    }
  }

  "The ReportDeadlinesService.getReportDeadlines method" when {

    "a valid list of Report Deadlines is returned from the connector" should {

      "return a valid list of Report Deadlines" in {
        setupMockReportDeadlines(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }

      "return a valid list of previous Report Deadlines" in {
        setupMockPreviousObligations(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockReportDeadlines(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe obligationsDataErrorModel
      }

      "return the error for previous deadlines" in {
        setupMockPreviousObligations(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe obligationsDataErrorModel
      }
    }
  }
}
