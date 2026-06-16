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

package obligations.services

import common.config.featureswitch.FeatureSwitching
import common.testUtils.TestSupport
import obligations.mocks.connectors.MockObligationsConnector
import obligations.models.*
import obligations.services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import obligations.services.reportingObligations.optOut.OptOutTestSupport
import obligations.testConstants.NextUpdatesTestConstants.*
import play.api.http.Status.INTERNAL_SERVER_ERROR
import obligations.testConstants.BusinessDetailsTestConstants.obligationsDataSuccessModel as _
import shared.models.{GroupedObligationsModel, ObligationWithIncomeType, ObligationsErrorModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate

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
      TestNextUpdatesService.getNextUpdatesViewModel(obligations) shouldBe {
        NextUpdatesViewModel(
          List(DeadlineViewModel(QuarterlyObligation, true, LocalDate.parse("2023-12-15"),
            List(ObligationWithIncomeType("nextUpdates.business", SingleObligationModel(LocalDate.parse("2023-12-15"), LocalDate.parse("2023-12-15"), LocalDate.parse("2023-12-15"), "Quarterly", None, "#001", StatusFulfilled))),
            List()))
        )
      }
    }
  }
}
