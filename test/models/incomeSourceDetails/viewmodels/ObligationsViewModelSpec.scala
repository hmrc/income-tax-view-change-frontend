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

import testUtils.UnitSpec

import java.time.LocalDate

class ObligationsViewModelSpec extends UnitSpec {
  val currentDate: LocalDate = LocalDate.of(2024, 1, 10)
  val currentTaxYear: Int = currentDate.getYear

  val baseObligationsViewModel: ObligationsViewModel = ObligationsViewModel(Seq(), Seq(), currentTaxYear, showPrevTaxYears = true)

  "getNumberOfOverdueAnnualObligations" should {
    "correctly return the number of overdue final declarations" when {
      "there are no final declarations" in {
        baseObligationsViewModel.getNumberOfOverdueAnnualObligations(currentDate) shouldBe 0
      }

      "there are no overdue final declarations" in {
        val currentYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(1), currentDate, currentDate.plusYears(1), "C", isFinalDec = true, "")
        baseObligationsViewModel.copy(finalDeclarationDates = Seq(currentYearFinalDeclaration)).getNumberOfOverdueAnnualObligations(currentDate) shouldBe 0
      }

      "there are both overdue and non-overdue final declarations" in {
        val previousYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(2).minusDays(1), currentDate.minusYears(1).minusDays(1), currentDate.minusDays(1), "C", isFinalDec = true, "Crystallised")
        val currentYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(1), currentDate, currentDate.plusYears(1), "C", isFinalDec = true, "")
        baseObligationsViewModel.copy(finalDeclarationDates = Seq(previousYearFinalDeclaration, currentYearFinalDeclaration)).getNumberOfOverdueAnnualObligations(currentDate) shouldBe 1
      }

      "there are overdue final declarations as well as overdue quarterly obligations" in {
        val previousYearFinalDeclaration1: DatesModel = DatesModel(currentDate.minusYears(3), currentDate.minusYears(2), currentDate.minusYears(1).minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousYearFinalDeclaration2: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(1), currentDate.minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousYearQ1Obligation: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(2).plusMonths(3), currentDate.minusYears(2).plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(Seq(previousYearQ1Obligation)), finalDeclarationDates = Seq(previousYearFinalDeclaration1, previousYearFinalDeclaration2)).getNumberOfOverdueAnnualObligations(currentDate) shouldBe 2
      }
    }
  }

  "getNumberOfOverdueQuarterlyObligations" should {
    "correctly return the number of overdue quarterly obligations" when {
      "there are no quarterly obligations" in {
        baseObligationsViewModel.getNumberOfOverdueQuarterlyObligations(currentDate) shouldBe 0
      }

      "there are no overdue quarterly obligations" in {
        val currentYearQ1Obligation: DatesModel = DatesModel(currentDate, currentDate.plusMonths(3), currentDate.plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val currentYearQ2Obligation: DatesModel = DatesModel(currentDate.plusMonths(3).plusDays(1), currentDate.plusMonths(6).plusDays(1), currentDate.plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(Seq(currentYearQ1Obligation, currentYearQ2Obligation))).getNumberOfOverdueQuarterlyObligations(currentDate) shouldBe 0
      }

      "there are both overdue and non-overdue quarterly obligations" in {
        val previousYearQ1Obligation: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(2).plusMonths(3), currentDate.minusYears(2).plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val previousYearQ2Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(3).plusDays(1), currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val previousYearQ3Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(2).plusMonths(10).plusDays(1), "#003", isFinalDec = false, "Quarterly")
        val previousYearQ4Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(1).plusDays(1), currentDate.minusYears(1).plusMonths(1).plusDays(1), "#004", isFinalDec = false, "Quarterly")
        val previousYearObligations: Seq[DatesModel] = Seq(previousYearQ1Obligation, previousYearQ2Obligation, previousYearQ3Obligation, previousYearQ4Obligation)

        val currentYearQ1Obligation: DatesModel = DatesModel(currentDate, currentDate.plusMonths(3), currentDate.plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val currentYearQ2Obligation: DatesModel = DatesModel(currentDate.plusMonths(3).plusDays(1), currentDate.plusMonths(6).plusDays(1), currentDate.plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val currentYearObligations: Seq[DatesModel] = Seq(currentYearQ1Obligation, currentYearQ2Obligation)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(previousYearObligations, currentYearObligations)).getNumberOfOverdueQuarterlyObligations(currentDate) shouldBe 4
      }

      "there are overdue quarterly obligations as well as overdue final declarations" in {
        val previousYearQ1Obligation: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(2).plusMonths(3), currentDate.minusYears(2).plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val previousYearQ2Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(3).plusDays(1), currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val previousYearQ3Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(2).plusMonths(10).plusDays(1), "#003", isFinalDec = false, "Quarterly")
        val previousYearQ4Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(1).plusDays(1), currentDate.minusYears(1).plusMonths(1).plusDays(1), "#004", isFinalDec = false, "Quarterly")
        val previousYearObligations: Seq[DatesModel] = Seq(previousYearQ1Obligation, previousYearQ2Obligation, previousYearQ3Obligation, previousYearQ4Obligation)

        val previousYearFinalDeclaration1: DatesModel = DatesModel(currentDate.minusYears(3), currentDate.minusYears(2), currentDate.minusYears(1).minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousYearFinalDeclaration2: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(1), currentDate.minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousFinalDeclarations: Seq[DatesModel] = Seq(previousYearFinalDeclaration1, previousYearFinalDeclaration2)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(previousYearObligations), finalDeclarationDates = previousFinalDeclarations).getNumberOfOverdueQuarterlyObligations(currentDate) shouldBe 4
      }
    }
  }

  "getOverdueObligationsMessageComponents" should {
    "return empty message components" when {
      "there are no overdue obligations" in {
        val currentYearQ1Obligation: DatesModel = DatesModel(currentDate, currentDate.plusMonths(3), currentDate.plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val currentYearQ2Obligation: DatesModel = DatesModel(currentDate.plusMonths(3).plusDays(1), currentDate.plusMonths(6).plusDays(1), currentDate.plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")

        val currentYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(1), currentDate, currentDate.plusYears(1), "C", isFinalDec = true, "")
        val obligationsViewModel = baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(Seq(currentYearQ1Obligation, currentYearQ2Obligation)), finalDeclarationDates = Seq(currentYearFinalDeclaration))

        val expectedMessageComponents: OverdueObligationsMessageComponents = OverdueObligationsMessageComponents("", Seq())

        obligationsViewModel.getOverdueObligationsMessageComponents(currentDate) shouldBe expectedMessageComponents
      }
    }

    "return the correct message components" when {
      "there is only one overdue quarterly obligation" in {
        val currentYearQ1Obligation: DatesModel = DatesModel(currentDate.minusMonths(6), currentDate.minusMonths(3), currentDate.minusMonths(2), "#001", isFinalDec = false, "Quarterly")
        val currentYearQ2Obligation: DatesModel = DatesModel(currentDate.minusMonths(3).plusDays(1), currentDate.plusDays(1), currentDate.plusMonths(1).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val currentYearObligations: Seq[DatesModel] = Seq(currentYearQ1Obligation, currentYearQ2Obligation)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(currentYearObligations)).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.single-quarterly-overdue.text", Seq("2023", "2024"))
      }

      "there are multiple overdue quarterly obligations" in {
        val currentYearQ1Obligation: DatesModel = DatesModel(currentDate.minusMonths(9), currentDate.minusMonths(6), currentDate.minusMonths(5), "#001", isFinalDec = false, "Quarterly")
        val currentYearQ2Obligation: DatesModel = DatesModel(currentDate.minusMonths(6), currentDate.minusMonths(3), currentDate.minusMonths(2), "#002", isFinalDec = false, "Quarterly")
        val currentYearQ3Obligation: DatesModel = DatesModel(currentDate.minusMonths(3).plusDays(1), currentDate.plusDays(1), currentDate.plusMonths(1).plusDays(1), "#003", isFinalDec = false, "Quarterly")
        val currentYearObligations: Seq[DatesModel] = Seq(currentYearQ1Obligation, currentYearQ2Obligation, currentYearQ3Obligation)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(currentYearObligations)).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.multiple-quarterly-overdue.text", Seq("2", "6","2023", "2024"))
      }

      "there are quarterly obligations from the previous tax year that also have an associated final declaration" in {
        val previousYearQ1Obligation: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(2).plusMonths(3), currentDate.minusYears(2).plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val previousYearQ2Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(3).plusDays(1), currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val previousYearQ3Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(2).plusMonths(10).plusDays(1), "#003", isFinalDec = false, "Quarterly")
        val previousYearQ4Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(1).plusDays(1), currentDate.minusYears(1).plusMonths(1).plusDays(1), "#004", isFinalDec = false, "Quarterly")
        val previousYearObligations: Seq[DatesModel] = Seq(previousYearQ1Obligation, previousYearQ2Obligation, previousYearQ3Obligation, previousYearQ4Obligation)

        val previousYearFinalDeclaration2: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(1), currentDate.minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousFinalDeclarations: Seq[DatesModel] = Seq(previousYearFinalDeclaration2)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(previousYearObligations), finalDeclarationDates = previousFinalDeclarations).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.multiple-hybrid-overdue.text", Seq("5", "2023", "2024"))
      }

      "there is only one overdue final declaration" in {
        val previousYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(2).minusDays(1), currentDate.minusYears(1).minusDays(1), currentDate.minusDays(1), "C", isFinalDec = true, "Crystallised")
        val currentYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(1), currentDate, currentDate.plusYears(1), "C", isFinalDec = true, "")
        baseObligationsViewModel.copy(finalDeclarationDates = Seq(previousYearFinalDeclaration, currentYearFinalDeclaration)).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.single-annual-overdue.text", Seq())
      }

      "there are multiple overdue final declarations" in {
        val previousYearFinalDeclaration1: DatesModel = DatesModel(currentDate.minusYears(3).minusDays(1), currentDate.minusYears(2).minusDays(1), currentDate.minusYears(1).minusDays(1), "C", isFinalDec = true, "Crystallised")
        val previousYearFinalDeclaration2: DatesModel = DatesModel(currentDate.minusYears(2).minusDays(1), currentDate.minusYears(1).minusDays(1), currentDate.minusDays(1), "C", isFinalDec = true, "Crystallised")
        val currentYearFinalDeclaration: DatesModel = DatesModel(currentDate.minusYears(1), currentDate, currentDate.plusYears(1), "C", isFinalDec = true, "")
        baseObligationsViewModel.copy(finalDeclarationDates = Seq(previousYearFinalDeclaration1, previousYearFinalDeclaration2, currentYearFinalDeclaration)).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.multiple-annual-overdue.text", Seq("2"))
      }

      "there is hybrid reporting including overdue quarterly obligations and final declarations" in {
        val previousYearQ1Obligation: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(2).plusMonths(3), currentDate.minusYears(2).plusMonths(4), "#001", isFinalDec = false, "Quarterly")
        val previousYearQ2Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(3).plusDays(1), currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(7).plusDays(1), "#002", isFinalDec = false, "Quarterly")
        val previousYearQ3Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(6).plusDays(1), currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(2).plusMonths(10).plusDays(1), "#003", isFinalDec = false, "Quarterly")
        val previousYearQ4Obligation: DatesModel = DatesModel(currentDate.minusYears(2).plusMonths(9).plusDays(1), currentDate.minusYears(1).plusDays(1), currentDate.minusYears(1).plusMonths(1).plusDays(1), "#004", isFinalDec = false, "Quarterly")
        val previousYearObligations: Seq[DatesModel] = Seq(previousYearQ1Obligation, previousYearQ2Obligation, previousYearQ3Obligation, previousYearQ4Obligation)

        val previousYearFinalDeclaration1: DatesModel = DatesModel(currentDate.minusYears(3), currentDate.minusYears(2), currentDate.minusYears(1).minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousYearFinalDeclaration2: DatesModel = DatesModel(currentDate.minusYears(2), currentDate.minusYears(1), currentDate.minusMonths(1), "C", isFinalDec = true, "Crystallised")
        val previousFinalDeclarations: Seq[DatesModel] = Seq(previousYearFinalDeclaration1, previousYearFinalDeclaration2)

        baseObligationsViewModel.copy(quarterlyObligationsDates = Seq(previousYearObligations), finalDeclarationDates = previousFinalDeclarations).getOverdueObligationsMessageComponents(currentDate) shouldBe OverdueObligationsMessageComponents("obligation.inset.multiple-hybrid-overdue.text", Seq("6", "2023", "2024"))
      }
    }
  }
}
