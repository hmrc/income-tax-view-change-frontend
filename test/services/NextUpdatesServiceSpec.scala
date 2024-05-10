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

import config.featureswitch.FeatureSwitching
import mocks.connectors.MockObligationsConnector
import models.admin.{IncomeSources, TimeMachineAddYear}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.INTERNAL_SERVER_ERROR
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

  "getNextDeadlineDueDateAndOverdueObligations" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockNextUpdates(obligationsAllDeadlinesSuccessModel)
        getDueDates().futureValue shouldBe
          Right(Seq(LocalDate.of(2017, 10, 30),
            LocalDate.of(2017, 10, 31), LocalDate.of(2017, 10, 1),
            LocalDate.of(2017, 10, 31), LocalDate.of(2017, 10, 31)))
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockNextUpdates(obligationsPropertyOnlySuccessModel)
        getDueDates().futureValue shouldBe
          Right(Seq(LocalDate.of(2017, 10, 1), LocalDate.of(2017, 10, 31)))
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockNextUpdates(obligationsCrystallisedOnlySuccessModel)
        getDueDates().futureValue shouldBe Right(Seq(LocalDate.of(2017, 10, 31)))
      }

      "there are no deadlines available" in new Setup {
        setupMockNextUpdates(emptyObligationsSuccessModel)
        val result: Either[Exception, Seq[LocalDate]] = getDueDates().futureValue
        result.isRight shouldBe true
        result shouldBe Right(Seq.empty)
      }

      "the Next Updates returned back an error model" in new Setup {
        setupMockNextUpdates(obligationsDataErrorModel)
        val result: Either[Exception, Seq[LocalDate]] = getDueDates().futureValue
        result.isLeft shouldBe true
        result.left.map(_.getMessage) shouldBe Left("Dummy Error Message")
      }
    }
    "return None" when {
      "404 response from getNextUpdates" in new Setup {
        setupMockNextUpdates(ObligationsModel(Seq.empty))
        getDueDates().futureValue shouldBe Right(Seq())
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
        setupMockFulfilledObligations(ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel)))
        TestNextUpdatesService.getNextUpdates(previous = true).futureValue shouldBe ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockNextUpdates(obligationsDataErrorModel)
        TestNextUpdatesService.getNextUpdates().futureValue shouldBe obligationsDataErrorModel
      }

      "return the error for previous deadlines" in {
        setupMockFulfilledObligations(obligationsDataErrorModel)
        TestNextUpdatesService.getNextUpdates(previous = true).futureValue shouldBe obligationsDataErrorModel
      }
    }
  }


  "The NextUpdatesService.getNextUpdates method" when {
    "it receives a fromDate and toDate" should {
      "valid current and previous obligations are returned" should {
        "return all obligations" in {

          setupMockAllObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          )))

          val result = TestNextUpdatesService.getNextUpdates(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            NextUpdatesModel("idOne", List(previousObligation))
          ))
        }
      }

      "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockAllObligationsWithDates(
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
        "an error is returned from  getAllObligations" in {
          setupMockAllObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(NextUpdatesErrorModel(INTERNAL_SERVER_ERROR, "not found"))

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

    "return a valid view model with quarterly obligations and final declaration(s)" in {
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
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no final declaration" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "Crystallised", None, "C")
        ))
      ))
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no quarterly obligations" in {
      disableAllSwitches()
      enable(IncomeSources)

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel("123", List(
          NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "Crystallised", None, "C")
        ))
      ))
      when(mockObligationsConnector.getNextUpdates()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)

      result.futureValue shouldBe ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallised")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
    }

  }
}
