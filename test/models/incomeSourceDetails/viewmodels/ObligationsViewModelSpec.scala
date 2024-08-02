/*
 * Copyright 2024 HM Revenue & Customs
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

package models.incomeSourceDetails.viewmodels

import models.incomeSourceDetails.TaxYear
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate

class ObligationsViewModelSpec extends UnitSpec {
  val withinFirstQuarter: LocalDate = LocalDate.of(2024, 3, 10)
  val afterFirstQuarterDeadline = LocalDate.of(2024, 5, 10)
  val afterSecondQuarterDeadline = LocalDate.of(2024, 8, 10)
  val after2023_2024FinalDeclarationDeadline = LocalDate.of(2025, 2, 10)

  val taxYear2022_2023FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2022,
    inboundCorrespondenceTo = endDateTaxYear2022,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2022,
    periodKey = "C",
    isFinalDec = true,
    obligationType = "Crystallised"
  )

  val taxYear2023_2024FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2023,
    inboundCorrespondenceTo = endDateTaxYear2023,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2023,
    periodKey = "C",
    isFinalDec = true,
    obligationType = "Crystallised"
  )

  val taxYear2024_2025FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2024,
    inboundCorrespondenceTo = endDateTaxYear2024,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2024,
    periodKey = "C",
    isFinalDec = true,
    obligationType = "Crystallised"
  )

  val taxYear2022QuarterlyObligationDates: Seq[DatesModel] = quarterlyDatesYearOne
  val taxYear2023QuarterlyObligationDates: Seq[DatesModel] = quarterlyDatesYearTwo
  val taxYear2024QuarterlyObligationDates: Seq[DatesModel] = quarterlyDatesYearThree

  val viewModelDefaultCurrentTaxYear: TaxYear = TaxYear(2023, 2024)

  private def constructObligationsViewModel(
    quarterlyObligationsDates: Seq[Seq[DatesModel]] = Seq(Seq()),
    finalDeclarationDates: Seq[DatesModel] = Seq(),
    currentTaxYear: TaxYear = viewModelDefaultCurrentTaxYear,
    showPrevTaxYears: Boolean = true
  ): ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = quarterlyObligationsDates,
      finalDeclarationDates = finalDeclarationDates,
      currentTaxYear = currentTaxYear.endYear,
      showPrevTaxYears = showPrevTaxYears
    )

  "getNumberOfOverdueAnnualObligations" should {
    "correctly return the number of overdue final declarations" when {
      "there are no final declarations" in {
        val viewModel = constructObligationsViewModel()

        viewModel.getNumberOfOverdueAnnualObligations(withinFirstQuarter) shouldBe 0
      }

      "there are no overdue final declarations" in {
        val viewModel = constructObligationsViewModel(finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration))

        viewModel.getNumberOfOverdueAnnualObligations(withinFirstQuarter) shouldBe 0
      }

      "there are both overdue and non-overdue final declarations" in {
        val viewModel = constructObligationsViewModel(finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration))

        viewModel.getNumberOfOverdueAnnualObligations(withinFirstQuarter) shouldBe 1
      }

      "there are overdue final declarations as well as overdue quarterly obligations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2023QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration)
        )

        viewModel.getNumberOfOverdueAnnualObligations(withinFirstQuarter) shouldBe 1
      }
    }
  }

  "getNumberOfOverdueQuarterlyObligations" should {
    "correctly return the number of overdue quarterly obligations" when {
      "there are no quarterly obligations" in {
        val viewModel = constructObligationsViewModel()

        viewModel.getNumberOfOverdueQuarterlyObligations(withinFirstQuarter) shouldBe 0
      }

      "there are no overdue quarterly obligations" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates))

        viewModel.getNumberOfOverdueQuarterlyObligations(withinFirstQuarter) shouldBe 0
      }

      "there are both overdue and non-overdue quarterly obligations" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2023QuarterlyObligationDates, taxYear2024QuarterlyObligationDates))

        viewModel.getNumberOfOverdueQuarterlyObligations(afterSecondQuarterDeadline) shouldBe 6
      }

      "there are overdue quarterly obligations as well as overdue final declarations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration)
        )

        viewModel.getNumberOfOverdueQuarterlyObligations(afterSecondQuarterDeadline) shouldBe 2
      }
    }
  }

  "getOverdueObligationsMessageComponents" should {
    "return empty message components" when {
      "there are no overdue obligations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("", Seq())

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter) shouldBe expectedMessageComponents
      }
    }

    "return the correct message components" when {
      "there is only one overdue quarterly obligation" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.single-quarterly-overdue.text", Seq("2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(afterFirstQuarterDeadline) shouldBe expectedMessageComponents
      }

      "there are multiple overdue quarterly obligations in the current tax year" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-quarterly-overdue.text", Seq("2", "6", "2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(afterSecondQuarterDeadline) shouldBe expectedMessageComponents
      }

      "there are quarterly obligations from the previous tax year that also have an associated final declaration" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2023QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration) // Due date of this is the due date for the quarterly reporting 2023 final declaration
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-hybrid-overdue.text", Seq("5", "2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter) shouldBe expectedMessageComponents
      }

      "there is only one overdue final declaration" in {
        val viewModel = constructObligationsViewModel(finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.single-annual-overdue.text", Seq())

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter) shouldBe expectedMessageComponents
      }

      "there are multiple overdue final declarations" in {
        val viewModel = constructObligationsViewModel(
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-annual-overdue.text", Seq("2"))

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline) shouldBe expectedMessageComponents
      }

      "there is hybrid reporting including overdue quarterly obligations and final declarations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2024QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-hybrid-overdue.text", Seq("6", "2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline) shouldBe expectedMessageComponents
      }
    }
  }
}
