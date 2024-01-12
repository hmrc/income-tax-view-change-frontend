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

package services

import config.featureswitch.{FeatureSwitching, IncomeSources, TimeMachineAddYear}
import mocks.connectors.MockObligationsConnector
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import testConstants.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import testConstants.NextUpdatesTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: NextUpdateModel = NextUpdateModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001")
  implicit val isTimeMachineEnabled: Boolean = isEnabled(TimeMachineAddYear)

  def currentObligation(date: LocalDate): NextUpdateModel = NextUpdateModel(date, date, date, "Quarterly", None, "#001")

  "getObligationDueDates" should {
    "return an internal server exception when an error model is returned from the connector" in new Setup {
      setupMockNextUpdates(obligationsDataErrorModel)

      getObligationDueDates.failed.futureValue shouldBe an[InternalServerException]
      getObligationDueDates.failed.futureValue.getMessage shouldBe "Unexpected Exception getting obligation due dates"
    }
    "return a single overdue date" when {
      "the connector returns obligations with a single overdue date" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(fixedDate, fixedDate, fixedDate.minusDays(1), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

        result.futureValue shouldBe Left(fixedDate.minusDays(1) -> true)
      }
    }
    "return a count of overdue dates" when {
      "the connector returns obligations with more than one overdue date" in new Setup {
        val obligationsWithMultipleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(fixedDate, fixedDate, fixedDate.minusDays(2), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate.minusDays(1), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithMultipleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

        result.futureValue shouldBe Right(2)
      }
    }
    "return a single non-overdue date" when {
      "the connector returns obligations without any overdue dates" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel(
            identification = "testId1",
            obligations = List(
              NextUpdateModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

        result.futureValue shouldBe Left(fixedDate -> false)
      }
    }
  }

  "getNextDeadlineDueDate" should {
    def assertDate(deadlineFuture: Future[Either[Throwable, Option[(LocalDate, Seq[LocalDate])]]], expectedDate: LocalDate): Unit = {
      deadlineFuture.futureValue match {
        case Right(Some((firstElementOfTuple, _))) => firstElementOfTuple shouldBe expectedDate
        case _ => fail("The future did not contain a Right-side value with the expected date")
      }
    }

    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockNextUpdates(obligationsAllDeadlinesSuccessModel)
        val deadlineFuture: Future[Either[Throwable, Option[(LocalDate, Seq[LocalDate])]]] = getNextDeadlineAndOverdueObligations(isTimeMachineEnabled)
        val expectedDate: LocalDate = LocalDate.of(2017, 10, 1)
        assertDate(deadlineFuture, expectedDate)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockNextUpdates(obligationsPropertyOnlySuccessModel)
        val deadlineFuture: Future[Either[Throwable, Option[(LocalDate, Seq[LocalDate])]]] = getNextDeadlineAndOverdueObligations(isTimeMachineEnabled)
        val expectedDate: LocalDate = LocalDate.of(2017, 10, 1)
        assertDate(deadlineFuture, expectedDate)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockNextUpdates(obligationsCrystallisedOnlySuccessModel)
        val deadlineFuture: Future[Either[Throwable, Option[(LocalDate, Seq[LocalDate])]]] = getNextDeadlineAndOverdueObligations(isTimeMachineEnabled)
        val expectedDate: LocalDate = LocalDate.of(2017, 10, 31)
        assertDate(deadlineFuture, expectedDate)
      }

      "there are no deadlines available" in new Setup {
        setupMockNextUpdates(emptyObligationsSuccessModel)
        val result: Throwable = getNextDeadlineAndOverdueObligations(isTimeMachineEnabled).failed.futureValue
        result shouldBe an[Exception]
        result.getMessage shouldBe "Unexpected Exception getting next deadline due and Overdue Obligations"
      }

      "the Next Updates returned back an error model" in new Setup {
        setupMockNextUpdates(obligationsDataErrorModel)
        val result: Throwable = getNextDeadlineAndOverdueObligations(isTimeMachineEnabled).failed.futureValue
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
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          )))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation)),
            NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(1))))
          ))
        }
      }

      "valid current obligations are returned but there are no previous obligations" should {
        "return the current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(NextUpdatesErrorModel(NOT_FOUND, "not found"))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(1))))
          ))
        }
      }

      "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockPreviousObligationsWithDates(
          from = fixedDate.minusDays(1),
          to = fixedDate.plusDays(1)
        )(ObligationsModel(Seq(
          NextUpdatesModel("idOne", List(previousObligation))
        )))
        setupMockNextUpdates(ObligationsModel(Seq(
          NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(3))))
        )))

        val result = TestNextUpdatesService.getNextUpdates(
          fromDate = fixedDate.minusDays(1),
          toDate = fixedDate.plusDays(1)
        ).futureValue

        result shouldBe ObligationsModel(Seq(
          NextUpdatesModel("idOne", List(previousObligation))
        ))
      }

      "return an error" when {
        "an error is returned from current obligations" in {
          setupMockPreviousObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(1)
          )(ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          )))
          setupMockNextUpdates(NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "error"))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(1)
          ).futureValue

          result shouldBe NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "error")
        }

        "an error is returned from the previous obligations" in {
          setupMockPreviousObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "not found"))
          setupMockNextUpdates(ObligationsModel(Seq(
            NextUpdatesModel("idTwo", List(currentObligation(fixedDate.plusDays(1))))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "not found")
        }
      }
    }
  }

  "getObligationDates" should {
    "return the correct set of dates given an ObligationsModel" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "C")
        ))
      ))
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false, obligationType = "EOPS"))
      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe expectedResult
    }
    "return the correct set of dates given a NextUpdateModel" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day: LocalDate = LocalDate.of(2023, 1, 1)
      val nextModel: NextUpdateModel = NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "C")
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false, obligationType = "EOPS"))
    }
    "show correct error when given a NextUpdatesErrorModel" in {
      disableAllSwitches()
      enable(IncomeSources)

      val nextModel: NextUpdatesErrorModel = NextUpdatesErrorModel(1, "fail")
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe Seq.empty
    }
  }

  "getObligationsViewModel" should {

    "return a valid view model with EOPS and quarterly obligations and final declaration(s)" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "Quarterly", None, "#001")
        )),
        NextUpdatesModel("123", List(
          NextUpdateModel(day.minusYears(1), day.minusYears(1).plusDays(1), day.minusYears(1).plusDays(2), "Quarterly", None, "#001")
        )
        ),
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")
        )),
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "Crystallised", None, "C")
        ))
      ))
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq(
          Seq(
            DatesModel(day.minusYears(1), day.minusYears(1).plusDays(1),
              day.minusYears(1).plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly"),
            DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly")
          )
        ),
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false, obligationType = "EOPS")),
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")),
        dateService.getCurrentTaxYearEnd(),
        showPrevTaxYears = true
      )

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no EOPS obligations" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: NextUpdateModel = NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "#001")
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq.empty,
        Seq.empty,
        Seq.empty,
        dateService.getCurrentTaxYearEnd(),
        showPrevTaxYears = true
      )
      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no quarterly obligations" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: NextUpdateModel = NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "EOPS", isFinalDec = false, obligationType = "EOPS")),
        Seq.empty,
        dateService.getCurrentTaxYearEnd(),
        showPrevTaxYears = true
      )
    }

  }
}
