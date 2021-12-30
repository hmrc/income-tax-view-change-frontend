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

import testConstants.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import testConstants.IncomeSourceDetailsTestConstants.{businessAndPropertyAligned, noIncomeDetails, propertyIncomeOnly}
import testConstants.NextUpdatesTestConstants._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, NextUpdatesModel, ObligationsModel}
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class NextUpdatesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestNextUpdatesService extends NextUpdatesService(mockIncomeTaxViewChangeConnector)

  class Setup extends NextUpdatesService(mockIncomeTaxViewChangeConnector)

  val previousObligation: NextUpdateModel = NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "Quarterly", Some(LocalDate.now), "#001")

  def currentObligation(date: LocalDate): NextUpdateModel = NextUpdateModel(date, date, date, "Quarterly", None, "#001")

  "getObligationDueDates" should {
    "return an internal server exception when an error model is returned from the connector" in new Setup {
      setupMockNextUpdates(obligationsDataErrorModel)

      getObligationDueDates().failed.futureValue shouldBe an[InternalServerException]
      getObligationDueDates().failed.futureValue.getMessage shouldBe "Unexpected Exception getting obligation due dates"
    }
    "return a single overdue date" when {
      "the connector returns obligations with a single overdue date" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(1), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        result.futureValue shouldBe Left(LocalDate.now.minusDays(1) -> true)
      }
    }
    "return a count of overdue dates" when {
      "the connector returns obligations with more than one overdue date" in new Setup {
        val obligationsWithMultipleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(2), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(1), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithMultipleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        result.futureValue shouldBe Right(2)
      }
    }
    "return a single non-overdue date" when {
      "the connector returns obligations without any overdue dates" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates()

        result.futureValue shouldBe Left(LocalDate.now -> false)
      }
    }
  }

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockNextUpdates(obligationsAllDeadlinesSuccessModel)
        getNextDeadlineDueDateAndOverDueObligations().futureValue._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockNextUpdates(obligationsPropertyOnlySuccessModel)
        getNextDeadlineDueDateAndOverDueObligations().futureValue._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockNextUpdates(obligationsCrystallisedOnlySuccessModel)
        getNextDeadlineDueDateAndOverDueObligations().futureValue._1 shouldBe LocalDate.of(2017, 10, 31)
      }

      "there are no deadlines available" in new Setup {
        setupMockNextUpdates(emptyObligationsSuccessModel)
        val result = getNextDeadlineDueDateAndOverDueObligations().failed.futureValue
        result shouldBe an[Exception]
        result.getMessage shouldBe "Unexpected Exception getting next deadline due and Overdue Obligations"
      }

      "the Next Updates returned back an error model" in new Setup {
        setupMockNextUpdates(obligationsDataErrorModel)
        val result = getNextDeadlineDueDateAndOverDueObligations().failed.futureValue
        result shouldBe an[Exception]
        result.getMessage shouldBe "Dummy Error Message"
      }
    }
  }

  "The NextUpdatesService.getNextUpdates method" when {

    "a valid list of Next Updates is returned from the connector" should {

      "return a valid list of Next Updates" in {
        setupMockNextUpdates(ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel)))
        TestNextUpdatesService.getNextUpdates().futureValue shouldBe ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))
      }

      "return a valid list of previous Next Updates" in {
        setupMockPreviousObligations(ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel)))
        TestNextUpdatesService.getNextUpdates(previous = true).futureValue shouldBe ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockNextUpdates(obligationsDataErrorModel)
        TestNextUpdatesService.getNextUpdates().futureValue shouldBe obligationsDataErrorModel
      }

      "return the error for previous deadlines" in {
        setupMockPreviousObligations(obligationsDataErrorModel)
        TestNextUpdatesService.getNextUpdates(previous = true).futureValue shouldBe obligationsDataErrorModel
      }
    }
  }


  "The NextUpdatesService.getNextUpdates method" when {
    "it receives a fromDate and toDate" should {
      "valid current and previous obligations are returned" should {
        "return all obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          )))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation)),
            NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          ))
        }
      }

          "valid current obligations are returned but there are no previous obligations" should {
        "return the current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(NextUpdatesErrorModel(NOT_FOUND, "not found"))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          ))
        }
      }

          "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockPreviousObligationsWithDates(
          from = LocalDate.now.minusDays(1),
          to = LocalDate.now.plusDays(1)
        )(ObligationsModel(Seq(
          NextUpdatesModel("idOne", List(previousObligation))
        )))
        setupMockNextUpdates(ObligationsModel(Seq(
          NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(3))))
        )))

        val result = TestNextUpdatesService.getNextUpdates(
          fromDate = LocalDate.now.minusDays(1),
          toDate = LocalDate.now.plusDays(1)
        ).futureValue

        result shouldBe ObligationsModel(Seq(
          NextUpdatesModel("idOne", List(previousObligation)),
        ))
      }

          "return an error" when {
        "an error is returned from current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(1)
          )(ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          )))
          setupMockNextUpdates(NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "error"))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(1)
          ).futureValue

          result shouldBe NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "error")
        }

        "an error is returned from the previous obligations" in {
          setupMockPreviousObligationsWithDates(
            from = LocalDate.now.minusDays(1),
            to = LocalDate.now.plusDays(2)
          )(NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "not found"))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(LocalDate.now.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = LocalDate.now.minusDays(1),
            toDate = LocalDate.now.plusDays(2)
          ).futureValue

          result shouldBe NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "not found")
        }
      }
    }
  }
}
