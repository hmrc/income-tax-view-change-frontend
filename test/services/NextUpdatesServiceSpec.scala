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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.{JsValue, Json}
import testConstants.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import testConstants.NextUpdatesTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future
import scala.io.{BufferedSource, Source}

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: NextUpdateModel = NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "Quarterly", Some(LocalDate.now), "#001")
  implicit val isTimeMachineEnabled = isEnabled(TimeMachineAddYear)

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
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.minusDays(1), "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

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
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now, "obligationsType", None, "testPeriodKey"),
              NextUpdateModel(LocalDate.now, LocalDate.now, LocalDate.now.plusDays(1), "obligationsType", None, "testPeriodKey")
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

        result.futureValue shouldBe Left(LocalDate.now -> false)
      }
    }
  }

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockNextUpdates(obligationsAllDeadlinesSuccessModel)
        getNextDeadlineDueDateAndOverDueObligations.futureValue._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockNextUpdates(obligationsPropertyOnlySuccessModel)
        getNextDeadlineDueDateAndOverDueObligations.futureValue._1 shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockNextUpdates(obligationsCrystallisedOnlySuccessModel)
        getNextDeadlineDueDateAndOverDueObligations.futureValue._1 shouldBe LocalDate.of(2017, 10, 31)
      }

      "there are no deadlines available" in new Setup {
        setupMockNextUpdates(emptyObligationsSuccessModel)
        val result = getNextDeadlineDueDateAndOverDueObligations.failed.futureValue
        result shouldBe an[Exception]
        result.getMessage shouldBe "Unexpected Exception getting next deadline due and Overdue Obligations"
      }

      "the Next Updates returned back an error model" in new Setup {
        setupMockNextUpdates(obligationsDataErrorModel)
        val result = getNextDeadlineDueDateAndOverDueObligations.failed.futureValue
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

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: NextUpdateModel = NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "C")
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe (Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false, obligationType = "EOPS")))
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
        )),
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
        Seq(Seq(DatesModel(day.minusYears(1), day.minusYears(1).plusDays(1),
          day.minusYears(1).plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly")),
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly"))),
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

    "Fix for MISUV-6494" in new Setup {
      // Read response from plain json file
      val source: BufferedSource = Source.fromURL(getClass.getResource("/data/1330_Transformed.json"))
      val jsonString: String = source.getLines().toList.mkString("")
      val json: JsValue = Json.parse(jsonString)

      val expectedResponse: ObligationsModel = json.validate[ObligationsModel].fold(
        _ => None, valid => Some(valid)).get

      when(mockObligationsConnector.getNextUpdates()(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(expectedResponse))

      val expectedResult: ObligationsViewModel = ObligationsViewModel(
        quarterlyObligationsDates = List.empty,
        eopsObligationsDates = List(),  // List(DatesModel(LocalDate.parse("2023-04-06"), LocalDate.parse("2024-04-05"), LocalDate.parse("2025-01-31"), "EOPS", false)),
        finalDeclarationDates = List(DatesModel(LocalDate.parse("2023-04-06"), LocalDate.parse("2024-04-05"), LocalDate.parse("2025-01-31"), "23P0", true, obligationType = "Crystallised")),
        currentTaxYear = 2024,
        showPrevTaxYears = false
      )
      val actualResult: Future[ObligationsViewModel] = getObligationsViewModel("XTIT00001015155", showPreviousTaxYears = false)
      actualResult.futureValue shouldBe expectedResult
    }
  }
}
