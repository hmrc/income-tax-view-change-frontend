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
import controllers.Assets.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel}
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class ReportDeadlinesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  class Setup extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  val previousObligation: ReportDeadlineModel = ReportDeadlineModel(LocalDate.now, LocalDate.now, LocalDate.now, "Quarterly", Some(LocalDate.now), "#001")

  def currentObligation(date: LocalDate): ReportDeadlineModel = ReportDeadlineModel(date, date, date, "Quarterly", None, "#001")

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
        await(getNextDeadlineDueDateAndOverDueObligations(propertyIncomeOnly))._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockReportDeadlines(obligationsCrystallisedOnlySuccessModel)
        await(getNextDeadlineDueDateAndOverDueObligations(noIncomeDetails))._1 shouldBe LocalDate.of(2017, 10, 31)
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


  "The ReportDeadlinesService.getReportDeadlines method" when {
    "it receives a fromDate and toDate" should {
      "valid current and previous obligations are returned" should {
        "return all obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(ObligationsModel(Seq(
            ReportDeadlinesModel("idOne", List(previousObligation))
          )))
          setupMockReportDeadlines(ObligationsModel(Seq(
            ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = await(TestReportDeadlinesService.getReportDeadlines(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ))

          result shouldBe ObligationsModel(Seq(
            ReportDeadlinesModel("idOne", List(previousObligation)),
            ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          ))
        }
      }

          "valid current obligations are returned but there are no previous obligations" should {
        "return the current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(ReportDeadlinesErrorModel(NOT_FOUND, "not found"))
          setupMockReportDeadlines(ObligationsModel(Seq(
            ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = await(TestReportDeadlinesService.getReportDeadlines(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ))

          result shouldBe ObligationsModel(Seq(
            ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          ))
        }
      }

          "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockPreviousObligationsWithDates(
          from = LocalDate.now.minusDays(1),
          to = LocalDate.now.plusDays(1)
        )(ObligationsModel(Seq(
          ReportDeadlinesModel("idOne", List(previousObligation))
        )))
        setupMockReportDeadlines(ObligationsModel(Seq(
          ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(3))))
        )))

        val result = await(TestReportDeadlinesService.getReportDeadlines(
          fromDate = LocalDate.now.minusDays(1),
          toDate = LocalDate.now.plusDays(1)
        ))

        result shouldBe ObligationsModel(Seq(
          ReportDeadlinesModel("idOne", List(previousObligation)),
        ))
      }

          "return an error" when {
        "an error is returned from current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(1)
          )(ObligationsModel(Seq(
            ReportDeadlinesModel("idOne", List(previousObligation))
          )))
          setupMockReportDeadlines(ReportDeadlinesErrorModel(INTERNAL_SERVER_ERROR, "error"))

          val result = await(TestReportDeadlinesService.getReportDeadlines(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(1)
          ))

          result shouldBe ReportDeadlinesErrorModel(INTERNAL_SERVER_ERROR, "error")
        }

        "an error is returned from the previous obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(ReportDeadlinesErrorModel(INTERNAL_SERVER_ERROR, "not found"))
          setupMockReportDeadlines(ObligationsModel(Seq(
            ReportDeadlinesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = await(TestReportDeadlinesService.getReportDeadlines(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ))

          result shouldBe ReportDeadlinesErrorModel(INTERNAL_SERVER_ERROR, "not found")
        }
      }
    }
  }
}
