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
  val taxYear2022_2023FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2022,
    inboundCorrespondenceTo = endDateTaxYear2022,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2022_2023,
    periodKey = "C",
    isFinalDec = true,
    obligationType = "Crystallisation"
  )

  val taxYear2023_2024FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2023,
    inboundCorrespondenceTo = endDateTaxYear2023,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2023_2024,
    periodKey = "C",
    isFinalDec = true,
    obligationType = "Crystallisation"
  )

  val taxYear2024_2025FinalDeclaration: DatesModel = DatesModel(
    inboundCorrespondenceFrom = startDateTaxYear2024,
    inboundCorrespondenceTo = endDateTaxYear2024,
    inboundCorrespondenceDue = crystallisedDeadlineTaxYear2024_2025,
    periodKey = "C",
    isFinalDec = true,
    obligationType = ""
  )

  val taxYear2022_2023QuarterlyObligationDates: Seq[DatesModel] = taxYear2022_2023quarterlyDates
  val taxYear2023_2024QuarterlyObligationDates: Seq[DatesModel] = taxYear2023_2024quarterlyDates
  val taxYear2024_2025QuarterlyObligationDates: Seq[DatesModel] = taxYear2024_2025quarterlyDates

  val viewModelDefaultCurrentTaxYear: TaxYear = TaxYear(2024, 2025)
  val withinFirstQuarter: LocalDate = LocalDate.of(2024, 5, 10)
  val afterFirstQuarterDeadline = LocalDate.of(2024, 8, 10)
  val afterSecondQuarterDeadline = LocalDate.of(2024, 11, 10)
  val after2023_2024FinalDeclarationDeadline = LocalDate.of(2025, 2, 10)

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

  "getOverdueObligationsMessageComponents" should {
    "return empty message components" when {
      "there are no overdue obligations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2024_2025QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("", Nil)

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }
    }

    "return the correct message components" when {
      "there is only one overdue quarterly obligation" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2024_2025QuarterlyObligationDates))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.single-quarterly-overdue.text", List("2024", "2025"))

        viewModel.getOverdueObligationsMessageComponents(afterFirstQuarterDeadline, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there are multiple overdue quarterly obligations in the current tax year" in {
        val viewModel = constructObligationsViewModel(quarterlyObligationsDates = Seq(taxYear2024_2025QuarterlyObligationDates))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-quarterly-overdue.text", List("2", "6", "2024", "2025"))

        viewModel.getOverdueObligationsMessageComponents(afterSecondQuarterDeadline, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there are overdue quarterly obligations from the previous tax year that also have an associated overdue final declaration" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("hybrid", Nil)

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there is only one overdue quarterly obligation from the previous tax year" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(Seq(DatesModel(taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")), taxYear2024_2025QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-tax-years-single-quarterly-overdue.text", Nil)

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there are multiple overdue quarterly obligations from the previous tax year and current tax year without an overdue final declaration" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-tax-years-multiple-quarterly-overdue.text", List("6"))

        viewModel.getOverdueObligationsMessageComponents(afterSecondQuarterDeadline, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there is only one overdue final declaration" in {
        val viewModel = constructObligationsViewModel(finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration))

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.single-annual-overdue.text", Nil)

        viewModel.getOverdueObligationsMessageComponents(withinFirstQuarter, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }

      "there are multiple overdue final declarations because a business is historic, one of which is in the current tax year" in {
        val viewModel = constructObligationsViewModel(
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.single-historic-overdue.text", List("2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline, isBusinessHistoric = true) shouldBe expectedMessageComponents
      }

      "there are multiple overdue obligations with a historic business, multiple of which are for the past 2 tax years" in {
        val viewModel = constructObligationsViewModel(
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration),
          quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("obligation.inset.multiple-historic-overdue.text", List("8", "2023", "2024"))

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline, isBusinessHistoric = true) shouldBe expectedMessageComponents
      }

      "there is hybrid reporting including overdue quarterly obligations and final declarations" in {
        val viewModel = constructObligationsViewModel(
          quarterlyObligationsDates = Seq(taxYear2024_2025QuarterlyObligationDates),
          finalDeclarationDates = Seq(taxYear2022_2023FinalDeclaration, taxYear2023_2024FinalDeclaration)
        )

        val expectedMessageComponents = OverdueObligationsMessageComponents("hybrid", Nil)

        viewModel.getOverdueObligationsMessageComponents(after2023_2024FinalDeclarationDeadline, isBusinessHistoric = false) shouldBe expectedMessageComponents
      }
    }
  }

  "getFirstUpcomingQuarterlyDate" should {
    "return None when there are no upcoming quarterly obligation due dates" in {
      val viewModel = constructObligationsViewModel(
        quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates),
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
      )

      val expectedUpcomingQuarterlyObligation = None

      viewModel.getFirstUpcomingQuarterlyDate(withinFirstQuarter) shouldBe expectedUpcomingQuarterlyObligation
    }

    "return the quarterly obligation with the next upcoming due date" in {
      val viewModel = constructObligationsViewModel(
        quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates),
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
      )

      val expectedUpcomingQuarterlyObligation = taxYear2024_2025QuarterlyObligationDates.headOption

      viewModel.getFirstUpcomingQuarterlyDate(withinFirstQuarter) shouldBe expectedUpcomingQuarterlyObligation
    }
  }

  "getQuarterlyObligationTaxYear" should {
    "return the previous tax year if the obligation is for the previous tax year" in {
      val viewModel = constructObligationsViewModel(
        quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates),
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration),
        currentTaxYear = TaxYear.forYearEnd(2025)
      )

      viewModel.quarterlyObligationsDates.flatten.headOption match {
        case None => fail("Test data does not have any quarterly obligations")
        case Some(quarterlyObligation) =>
          val expectedTaxYear = 2024

          viewModel.getQuarterlyObligationTaxYear(quarterlyObligation) shouldBe expectedTaxYear
      }
    }

    "return the next tax year if the obligation is for the next tax year" in {
      val viewModel = constructObligationsViewModel(
        quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates),
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration),
        currentTaxYear = TaxYear.forYearEnd(2024)
      )

      viewModel.quarterlyObligationsDates.flatten.lastOption match {
        case None => fail("Test data does not have any quarterly obligations")
        case Some(quarterlyObligation) =>
          val expectedTaxYear = 2025

          viewModel.getQuarterlyObligationTaxYear(quarterlyObligation) shouldBe expectedTaxYear
      }
    }

    "return the current tax year if the obligation is not in current tax year" in {
      val viewModel = constructObligationsViewModel(
        quarterlyObligationsDates = Seq(taxYear2023_2024QuarterlyObligationDates, taxYear2024_2025QuarterlyObligationDates),
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration),
        currentTaxYear = TaxYear.forYearEnd(2024)
      )

      viewModel.quarterlyObligationsDates.flatten.headOption match {
        case None => fail("Test data does not have any quarterly obligations")
        case Some(quarterlyObligation) =>
          val expectedTaxYear = 2024

          viewModel.getQuarterlyObligationTaxYear(quarterlyObligation) shouldBe expectedTaxYear
      }
    }
  }

  "getFinalDeclarationDate" should {
    "return None when there are no upcoming final declaration due dates" in {
      val viewModel = constructObligationsViewModel(
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration)
      )

      val expectedUpcomingFinalDeclaration = None

      viewModel.getFinalDeclarationDate(after2023_2024FinalDeclarationDeadline) shouldBe expectedUpcomingFinalDeclaration
    }

    "return the final declaration with the next upcoming due date" in {
      val viewModel = constructObligationsViewModel(
        finalDeclarationDates = Seq(taxYear2023_2024FinalDeclaration, taxYear2024_2025FinalDeclaration)
      )

      val expectedUpcomingFinalDeclaration = Some(taxYear2024_2025FinalDeclaration)

      viewModel.getFinalDeclarationDate(after2023_2024FinalDeclarationDeadline) shouldBe expectedUpcomingFinalDeclaration
    }
  }
}
