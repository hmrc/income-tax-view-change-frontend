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
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.obligations._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.INTERNAL_SERVER_ERROR
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutTestSupport
import testConstants.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import testConstants.NextUpdatesTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future

class NextUpdatesServiceSpec extends TestSupport with MockObligationsConnector with FeatureSwitching {

  object TestNextUpdatesService extends NextUpdatesService(mockObligationsConnector)

  class Setup extends NextUpdatesService(mockObligationsConnector)

  val previousObligation: SingleObligationModel = SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled)

  def currentObligation(date: LocalDate): SingleObligationModel = SingleObligationModel(date, date, date, "Quarterly", None, "#001", StatusFulfilled)

  val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
  val crystallisationDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

  "getQuarterlyUpdatesCountForTaxYear" when {
    "offer three years opt-out, queried is previous-year" when {
      "previous-year count is 3 and current-year count is 3" should {
        "return a count of 6" in new Setup {

          val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(queriedTaxYear.toFinancialYearStart, queriedTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer three years opt-out, queried is current-year" should {
      "previous-year count is 3 and current-year count is 3" should {
        "return a count of 3" in new Setup {

          val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut(1)
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(queriedTaxYear.toFinancialYearStart, queriedTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer three years opt-out, queried is next-year" should {
      "previous-year count is 3 and current-year count is 3" should {
        "return a count of 2" in new Setup {

          val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut.last
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)
          val nextYear = optOutProposition.availableTaxYearsForOptOut.last

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 2).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(nextYear.toFinancialYearStart, nextYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 2)
        }
      }
    }

    "offer two years opt-out (PY, NY), queried is previous-year" should {
      "previous-year count is 3 and next-year count is 3" should {
        "return a count of 3" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndNY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val nextYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(queriedTaxYear.toFinancialYearStart, queriedTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(nextYear.toFinancialYearStart, nextYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer two years opt-out (PY, NY), queried is next-year" should {
      "previous-year count is 3 and next-year count is 3" should {
        "return a count of 3" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndNY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut(1)
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val nextYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(nextYear.toFinancialYearStart, nextYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer two years opt-out (PY, CY), queried is previous-year" should {
      "previous-year count is 3 and current-year count is 3" should {
        "return a count of 6" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndCY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer two years opt-out (PY, CY), queried is current-year" should {
      "previous-year count is 3 and current-year count is 3" should {
        "return a count of 3" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndCY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut(1)
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          def buildNextUpdatesModel(): SingleObligationModel = {
            SingleObligationModel(
              fixedDate, fixedDate, fixedDate,
              "Quarterly", Some(fixedDate),
              "#001", StatusFulfilled)
          }

          val updates = GroupedObligationsModel("XA00001234", (1 to 3).map(_ => buildNextUpdatesModel()).toList)
          val response = ObligationsModel(Seq(updates))
          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 3)
        }
      }
    }

    "offer two years opt-out (PY, CY), queried is current-year" should {
      "previous-year count is 0 and current-year count is 0" should {
        "return a count of 0" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndCY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut(1)
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          val response = ObligationsModel(Seq())
          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(response)
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(response)

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 0)
        }
      }
    }

    "offer two years opt-out (PY, CY), queried is current-year" should {
      "calls fail" should {
        "return a count of 0" in new Setup {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndCY()
          val queriedTaxYear = optOutProposition.availableTaxYearsForOptOut(1)
          val previousTaxYear = optOutProposition.availableTaxYearsForOptOut.head
          val currentYear = optOutProposition.availableTaxYearsForOptOut(1)

          setupMockAllObligationsWithDates(previousTaxYear.toFinancialYearStart, previousTaxYear.toFinancialYearEnd)(ObligationsErrorModel(400, "some error"))
          setupMockAllObligationsWithDates(currentYear.toFinancialYearStart, currentYear.toFinancialYearEnd)(ObligationsErrorModel(400, "some error"))

          val result = TestNextUpdatesService.getQuarterlyUpdatesCounts(queriedTaxYear)

          result.futureValue shouldBe QuarterlyUpdatesCountForTaxYear(queriedTaxYear, 0)
        }
      }
    }

  }

  "getObligationDueDates" should {
    "return an internal server exception when an error model is returned from the connector" in new Setup {
      setupMockNextUpdates(obligationsDataErrorModel)

      getObligationDueDates.failed.futureValue shouldBe an[InternalServerException]
      getObligationDueDates.failed.futureValue.getMessage shouldBe "Unexpected Exception getting obligation due dates"
    }
    "return a single overdue date" when {
      "the connector returns obligations with a single overdue date" in new Setup {
        val obligationsWithSingleOverdue: ObligationsModel = ObligationsModel(Seq(
          GroupedObligationsModel(
            identification = "testId1",
            obligations = List(
              SingleObligationModel(fixedDate, fixedDate, fixedDate.minusDays(1), "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey", StatusFulfilled)
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
          GroupedObligationsModel(
            identification = "testId1",
            obligations = List(
              SingleObligationModel(fixedDate, fixedDate, fixedDate.minusDays(2), "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate.minusDays(1), "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey", StatusFulfilled)
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
          GroupedObligationsModel(
            identification = "testId1",
            obligations = List(
              SingleObligationModel(fixedDate, fixedDate, fixedDate, "obligationsType", None, "testPeriodKey", StatusFulfilled),
              SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "obligationsType", None, "testPeriodKey", StatusFulfilled)
            )
          )
        ))
        setupMockNextUpdates(obligationsWithSingleOverdue)

        val result: Future[Either[(LocalDate, Boolean), Int]] = getObligationDueDates

        result.futureValue shouldBe Left(fixedDate -> false)
      }
    }
  }

  "getDueDates" should {
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

      "the ObligationsModel contains only empty obligations" in new Setup {
        setupMockNextUpdates(obligationsSuccessModelFiltered)
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

  "The NextUpdatesService.getOpenObligations method" when {

    "a valid list of Next Updates is returned from the connector" should {

      "return a valid list of Next Updates" in {
        setupMockNextUpdates(ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel)))
        TestNextUpdatesService.getOpenObligations().futureValue shouldBe ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockNextUpdates(obligationsDataErrorModel)
        TestNextUpdatesService.getOpenObligations().futureValue shouldBe obligationsDataErrorModel
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
            GroupedObligationsModel("idOne", List(previousObligation))
          )))

          val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsModel(Seq(
            GroupedObligationsModel("idOne", List(previousObligation))
          ))
        }
      }

      "valid obligations are returned but current obligations are not in the correct time period" in {
        setupMockAllObligationsWithDates(
          from = fixedDate.minusDays(1),
          to = fixedDate.plusDays(1)
        )(ObligationsModel(Seq(
          GroupedObligationsModel("idOne", List(previousObligation))
        )))
        setupMockNextUpdates(ObligationsModel(Seq(
          GroupedObligationsModel("idTwo", List(currentObligation(fixedDate.plusDays(3))))
        )))

        val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
          fromDate = fixedDate.minusDays(1),
          toDate = fixedDate.plusDays(1)
        ).futureValue

        result shouldBe ObligationsModel(Seq(
          GroupedObligationsModel("idOne", List(previousObligation))
        ))
      }

      "return an error" when {
        "an error is returned from  getAllObligations" in {
          setupMockAllObligationsWithDates(
            from = fixedDate.minusDays(1),
            to = fixedDate.plusDays(2)
          )(ObligationsErrorModel(INTERNAL_SERVER_ERROR, "not found"))

          val result = TestNextUpdatesService.getAllObligationsWithinDateRange(
            fromDate = fixedDate.minusDays(1),
            toDate = fixedDate.plusDays(2)
          ).futureValue

          result shouldBe ObligationsErrorModel(INTERNAL_SERVER_ERROR, "not found")
        }
      }
    }
  }

  "getObligationDates" should {
    "return the correct set of dates given an ObligationsModel" in {
      disableAllSwitches()

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Quarterly", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false, obligationType = "Quarterly"))
      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe expectedResult
    }
    "show correct error when given a NextUpdatesErrorModel" in {
      disableAllSwitches()

      val nextModel: ObligationsErrorModel = ObligationsErrorModel(1, "fail")
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationDates("123")
      result.futureValue shouldBe Seq.empty
    }
  }

  "getObligationsViewModel" should {

    "return a valid view model with quarterly obligations and final declaration(s)" in {
      disableAllSwitches()

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Quarterly", None, "#001", StatusFulfilled)
        )),
        GroupedObligationsModel("123", List(
          SingleObligationModel(day.minusYears(1), day.minusYears(1).plusDays(1), day.minusYears(1).plusDays(2), "Quarterly", None, "#001", StatusFulfilled)
        )
        ),
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq(
          Seq(
            DatesModel(day.minusYears(1), day.minusYears(1).plusDays(1),
              day.minusYears(1).plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly"),
            DatesModel(day, day.plusDays(1), day.plusDays(2), "#001", isFinalDec = false, obligationType = "Quarterly")
          )
        ),
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no final declaration" in {
      disableAllSwitches()

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val expectedResult = ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)
      result.futureValue shouldBe expectedResult
    }

    "return a valid view model if no quarterly obligations" in {
      disableAllSwitches()

      val day = LocalDate.of(2023, 1, 1)
      val nextModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("123", List(
          SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "Crystallisation", None, "C", StatusFulfilled)
        ))
      ))
      when(mockObligationsConnector.getOpenObligations()(any(), any())).
        thenReturn(Future(nextModel))

      val result = TestNextUpdatesService.getObligationsViewModel("123", showPreviousTaxYears = true)

      result.futureValue shouldBe ObligationsViewModel(
        Seq.empty,
        Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")),
        dateService.getCurrentTaxYearEnd,
        showPrevTaxYears = true
      )
    }

  }

  ".getNextUpdatesViewModel" should {
    "return a valid model with no obligations of unsupported types" in {
      val obligations = ObligationsModel(
        Seq(
          GroupedObligationsModel("XA00001234", List(
            SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", None, "#001", StatusFulfilled),
            SingleObligationModel(fixedDate, fixedDate, fixedDate, "Other", None, "#002", StatusFulfilled)
          )),
          GroupedObligationsModel("XA00001235", List(
            SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", None, "#003", StatusFulfilled)
          )),
          GroupedObligationsModel("XA00001236", List(
            SingleObligationModel(fixedDate, fixedDate, fixedDate, "Other", None, "#003", StatusFulfilled)
          ))
        )
      )
      TestNextUpdatesService.getNextUpdatesViewModel(obligations, true) shouldBe {
        NextUpdatesViewModel(
          List(DeadlineViewModel(QuarterlyObligation, true, LocalDate.parse("2023-12-15"),
            List(ObligationWithIncomeType("nextUpdates.business", SingleObligationModel(LocalDate.parse("2023-12-15"), LocalDate.parse("2023-12-15"), LocalDate.parse("2023-12-15"), "Quarterly", None, "#001", StatusFulfilled))),
            List()))
        )
      }
    }
  }

  "getNextDueDates" should {
    "return the earliest quarterly due date and next tax return due date" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(3), "Quarterly", None, "#001", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "Quarterly", None, "#002", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, crystallisationDueDate, "Crystallisation", None, "#003", StatusOpen)
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe Some(fixedDate.plusDays(1))

      nextTaxReturnDueDate shouldBe Some(crystallisationDueDate)
    }

    "return the earliest quarterly due date and fallback static next tax return due date when Crystallisation is not present" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(3), "Quarterly", None, "#001", StatusOpen),
          SingleObligationModel(fixedDate, fixedDate, fixedDate.plusDays(1), "Quarterly", None, "#002", StatusOpen),
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe Some(fixedDate.plusDays(1))

      val staticNextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

      nextTaxReturnDueDate shouldBe Some(staticNextTaxReturnDueDate)
    }

    "return None when no quarterly obligations exist" in new Setup {
      val obligationsModel: ObligationsModel = ObligationsModel(Seq(
        GroupedObligationsModel("id1", List(
          SingleObligationModel(fixedDate, fixedDate, crystallisationDueDate, "Crystallisation", None, "#003", StatusOpen)
        ))
      ))

      setupMockNextUpdates(obligationsModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe None

      nextTaxReturnDueDate shouldBe Some(crystallisationDueDate)
    }
    "return an error when the obligations service returns an error model" in new Setup {
      setupMockNextUpdates(obligationsDataErrorModel)

      val (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) = TestNextUpdatesService.getNextDueDates().futureValue

      nextQuarterlyUpdateDueDate shouldBe None

      val currentTaxYearEnd: Int = TestNextUpdatesService.dateService.getCurrentTaxYear.endYear
      val expectedNextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYearEnd + 1, 1, 31)

      nextTaxReturnDueDate shouldBe Some(expectedNextTaxReturnDueDate)
    }
  }
}
