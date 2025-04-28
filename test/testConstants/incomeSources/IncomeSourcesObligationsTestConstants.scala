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

package testConstants.incomeSources

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IncomeSourcesObligationsTestConstants {
  val taxYear: Int = 2018

  def generateQuarterDates(taxYear: Int): (LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate) = {
    val startDateQ1 = LocalDate.of(taxYear, 1, 6)
    val endDateQ1 = LocalDate.of(taxYear, 4, 5)
    val startDateQ2 = LocalDate.of(taxYear, 4, 6)
    val endDateQ2 = LocalDate.of(taxYear, 7, 5)
    val startDateQ3 = LocalDate.of(taxYear, 7, 6)
    val endDateQ3 = LocalDate.of(taxYear, 10, 5)
    val startDateQ4 = LocalDate.of(taxYear, 10, 6)
    val endDateQ4 = LocalDate.of(taxYear + 1, 1, 5)

    (startDateQ1, endDateQ1, startDateQ2, endDateQ2, startDateQ3, endDateQ3, startDateQ4, endDateQ4)
  }

  def generateTaxYearQuarterDates(taxYear: Int): (LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate, LocalDate) = {
    val startDateQ1 = LocalDate.of(taxYear, 4, 6)
    val endDateQ1 = LocalDate.of(taxYear, 7, 5)
    val startDateQ2 = LocalDate.of(taxYear, 7, 6)
    val endDateQ2 = LocalDate.of(taxYear, 10, 5)
    val startDateQ3 = LocalDate.of(taxYear, 10, 6)
    val endDateQ3 = LocalDate.of(taxYear + 1, 1, 5)
    val startDateQ4 = LocalDate.of(taxYear + 1, 1, 6)
    val endDateQ4 = LocalDate.of(taxYear + 1, 4, 5)

    (startDateQ1, endDateQ1, startDateQ2, endDateQ2, startDateQ3, endDateQ3, startDateQ4, endDateQ4)
  }

  def formatQuarterText(startDate: LocalDate, endDate: LocalDate): String = {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    s"${startDate.format(formatter)} to ${endDate.format(formatter)}"
  }

  def formatDeadlineText(deadlineDate: LocalDate): String = {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    s"${deadlineDate.format(formatter)}"
  }

  val taxYear2022 = 2022
  val taxYear2023 = 2023
  val taxYear2024 = 2024
  val taxYear2025 = 2025


  val (startDateQ1_2022, endDateQ1_2022,
  startDateQ2_2022, endDateQ2_2022,
  startDateQ3_2022, endDateQ3_2022,
  startDateQ4_2022, endDateQ4_2022) = generateQuarterDates(taxYear2022)

  val (startDateQ1_2023, endDateQ1_2023,
  startDateQ2_2023, endDateQ2_2023,
  startDateQ3_2023, endDateQ3_2023,
  startDateQ4_2023, endDateQ4_2023) = generateQuarterDates(taxYear2023)

  val (startDateQ1_2024, endDateQ1_2024,
  startDateQ2_2024, endDateQ2_2024,
  startDateQ3_2024, endDateQ3_2024,
  startDateQ4_2024, endDateQ4_2024) = generateQuarterDates(taxYear2024)

  val (startDateQ1_2025, endDateQ1_2025,
  startDateQ2_2025, endDateQ2_2025,
  startDateQ3_2025, endDateQ3_2025,
  startDateQ4_2025, endDateQ4_2025) = generateQuarterDates(taxYear2025)

  val q1Text_2022 = formatQuarterText(startDateQ1_2022, endDateQ1_2022)
  val q2Text_2022 = formatQuarterText(startDateQ2_2022, endDateQ2_2022)
  val q3Text_2022 = formatQuarterText(startDateQ3_2022, endDateQ3_2022)
  val q4Text_2022 = formatQuarterText(startDateQ4_2022, endDateQ4_2022)

  val q1DeadlineText_2022 = formatDeadlineText(endDateQ1_2022.plusMonths(1))
  val q2DeadlineText_2022 = formatDeadlineText(endDateQ2_2022.plusMonths(1))
  val q3DeadlineText_2022 = formatDeadlineText(endDateQ3_2022.plusMonths(1))
  val q4DeadlineText_2022 = formatDeadlineText(endDateQ4_2022.plusMonths(1))

  val q1Text_2023 = formatQuarterText(startDateQ1_2023, endDateQ1_2023)
  val q2Text_2023 = formatQuarterText(startDateQ2_2023, endDateQ2_2023)
  val q3Text_2023 = formatQuarterText(startDateQ3_2023, endDateQ3_2023)
  val q4Text_2023 = formatQuarterText(startDateQ4_2023, endDateQ4_2023)

  val q1DeadlineText_2023 = formatDeadlineText(endDateQ1_2023.plusMonths(1))
  val q2DeadlineText_2023 = formatDeadlineText(endDateQ2_2023.plusMonths(1))
  val q3DeadlineText_2023 = formatDeadlineText(endDateQ3_2023.plusMonths(1))
  val q4DeadlineText_2023 = formatDeadlineText(endDateQ4_2023.plusMonths(1))

  val q1Text_2024 = formatQuarterText(startDateQ1_2024, endDateQ1_2024)
  val q2Text_2024 = formatQuarterText(startDateQ2_2024, endDateQ2_2024)
  val q3Text_2024 = formatQuarterText(startDateQ3_2024, endDateQ3_2024)
  val q4Text_2024 = formatQuarterText(startDateQ4_2024, endDateQ4_2024)

  val q1DeadlineText_2024 = formatDeadlineText(endDateQ1_2024.plusMonths(1))
  val q2DeadlineText_2024 = formatDeadlineText(endDateQ2_2024.plusMonths(1))
  val q3DeadlineText_2024 = formatDeadlineText(endDateQ3_2024.plusMonths(1))
  val q4DeadlineText_2024 = formatDeadlineText(endDateQ4_2024.plusMonths(1))

  val startDateTaxYear2022 = LocalDate.of(2022, 4, 6)
  val endDateTaxYear2022 = LocalDate.of(2023, 4, 5)
  val startDateTaxYear2023 = LocalDate.of(2023, 4, 6)
  val endDateTaxYear2023 = LocalDate.of(2024, 4, 5)
  val startDateTaxYear2024 = LocalDate.of(2024, 4, 6)
  val endDateTaxYear2024 = LocalDate.of(2025, 4, 5)

  val crystallisedDeadlineTaxYear2022_2023 = LocalDate.of(2024, 1, 31)
  val crystallisedDeadlineTaxYear2023_2024 = LocalDate.of(2025, 1, 31)
  val crystallisedDeadlineTaxYear2024_2025 = LocalDate.of(2026, 1, 31)

  val finalDeclaration2022_2023taxYear =
    DatesModel(startDateTaxYear2022, endDateTaxYear2022, crystallisedDeadlineTaxYear2022_2023, "C", isFinalDec = true, obligationType = "Crystallisation")

  val finalDeclaration2023_2024taxYear =
    DatesModel(startDateTaxYear2023, endDateTaxYear2023, crystallisedDeadlineTaxYear2023_2024, "C", isFinalDec = true, obligationType = "Crystallisation")

  val finalDeclaration2024_2025taxYear =
    DatesModel(startDateTaxYear2024, endDateTaxYear2024, crystallisedDeadlineTaxYear2024_2025, "C", isFinalDec = true, obligationType = "Crystallisation")

  val quarterlyDatesYearOne = Seq(
    DatesModel(startDateQ1_2022, endDateQ1_2022, endDateQ1_2022.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ2_2022, endDateQ2_2022, endDateQ2_2022.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ3_2022, endDateQ3_2022, endDateQ3_2022.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ4_2022, endDateQ4_2022, endDateQ4_2022.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val quarterlyDatesYearTwo = Seq(
    DatesModel(startDateQ1_2023, endDateQ1_2023, endDateQ1_2023.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ2_2023, endDateQ2_2023, endDateQ2_2023.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ3_2023, endDateQ3_2023, endDateQ3_2023.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ4_2023, endDateQ4_2023, endDateQ4_2023.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val quarterlyDatesYearThree = Seq(
    DatesModel(startDateQ1_2024, endDateQ1_2024, endDateQ1_2024.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ2_2024, endDateQ2_2024, endDateQ2_2024.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ3_2024, endDateQ3_2024, endDateQ3_2024.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ4_2024, endDateQ4_2024, endDateQ4_2024.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val quarterlyDatesYearFour = Seq(
    DatesModel(startDateQ1_2025, endDateQ1_2025, endDateQ1_2025.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ2_2025, endDateQ2_2025, endDateQ2_2025.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ3_2025, endDateQ3_2025, endDateQ3_2025.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ4_2025, endDateQ4_2025, endDateQ4_2025.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val annualDatesYearOneSimple = Seq(
    DatesModel(startDateQ1_2022, endDateQ1_2022, endDateQ1_2022.plusMonths(1), "#001", isFinalDec = true, obligationType = "Annual"),
  )
  val (taxYear2022_2023q1startDate, taxYear2022_2023q1endDate,
      taxYear2022_2023q2startDate, taxYear2022_2023q2endDate,
      taxYear2022_2023q3startDate, taxYear2022_2023q3endDate,
      taxYear2022_2023q4startDate, taxYear2022_2023q4endDate
    ) = generateTaxYearQuarterDates(taxYear2022)

  val taxYear2022_2023quarterlyDates: Seq[DatesModel] = Seq(
    DatesModel(taxYear2022_2023q1startDate, taxYear2022_2023q1endDate, taxYear2022_2023q1endDate.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2022_2023q2startDate, taxYear2022_2023q2endDate, taxYear2022_2023q2endDate.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2022_2023q3startDate, taxYear2022_2023q3endDate, taxYear2022_2023q3endDate.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2022_2023q4startDate, taxYear2022_2023q4endDate, taxYear2022_2023q4endDate.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val (taxYear2023_2024q1startDate, taxYear2023_2024q1endDate,
      taxYear2023_2024q2startDate, taxYear2023_2024q2endDate,
      taxYear2023_2024q3startDate, taxYear2023_2024q3endDate,
      taxYear2023_2024q4startDate, taxYear2023_2024q4endDate
    ) = generateTaxYearQuarterDates(taxYear2023)

  val taxYear2023_2024quarterlyDates: Seq[DatesModel] = Seq(
    DatesModel(taxYear2023_2024q1startDate, taxYear2023_2024q1endDate, taxYear2023_2024q1endDate.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2023_2024q2startDate, taxYear2023_2024q2endDate, taxYear2023_2024q2endDate.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2023_2024q3startDate, taxYear2023_2024q3endDate, taxYear2023_2024q3endDate.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val (taxYear2024_2025q1startDate, taxYear2024_2025q1endDate,
      taxYear2024_2025q2startDate, taxYear2024_2025q2endDate,
      taxYear2024_2025q3startDate, taxYear2024_2025q3endDate,
      taxYear2024_2025q4startDate, taxYear2024_2025q4endDate
    ) = generateTaxYearQuarterDates(taxYear2024)

  val taxYear2024_2025quarterlyDates: Seq[DatesModel] = Seq(
    DatesModel(taxYear2024_2025q1startDate, taxYear2024_2025q1endDate, taxYear2024_2025q1endDate.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2024_2025q2startDate, taxYear2024_2025q2endDate, taxYear2024_2025q2endDate.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2024_2025q3startDate, taxYear2024_2025q3endDate, taxYear2024_2025q3endDate.plusMonths(1), "#003", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(taxYear2024_2025q4startDate, taxYear2024_2025q4endDate, taxYear2024_2025q4endDate.plusMonths(1), "#004", isFinalDec = false, obligationType = "Quarterly")
  )

  val quarterlyDatesYearOneSimple = Seq(
    DatesModel(startDateQ1_2022, endDateQ1_2022, endDateQ1_2022.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
  )
  val quarterlyDatesYearTwoSimple = Seq(
    DatesModel(startDateQ1_2023, endDateQ1_2023, endDateQ1_2023.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
  )
  val quarterlyDatesYearThreeSimple = Seq(
    DatesModel(startDateQ1_2024, endDateQ1_2024, endDateQ1_2024.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
  )
  val quarterlyObligationDatesFull = Seq(quarterlyDatesYearOne, quarterlyDatesYearTwo, quarterlyDatesYearThree)
  val quarterlyObligationDatesSimple = Seq(quarterlyDatesYearOneSimple, quarterlyDatesYearTwoSimple, quarterlyDatesYearThreeSimple)

  val singleYearTwoQuarterlyDates = Seq(Seq(
    DatesModel(startDateQ1_2022, endDateQ1_2022, endDateQ1_2022.plusMonths(1), "#001", isFinalDec = false, obligationType = "Quarterly"),
    DatesModel(startDateQ2_2022, endDateQ2_2022, endDateQ2_2022.plusMonths(1), "#002", isFinalDec = false, obligationType = "Quarterly"),
  ))

  val previousYearsQuarterlyObligationDates = Seq(quarterlyDatesYearOne, quarterlyDatesYearTwo, quarterlyDatesYearThree)
  val currentAndPreviousYearsQuarterlyObligationDates = Seq(quarterlyDatesYearTwo, quarterlyDatesYearThree)

  val viewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = quarterlyObligationDatesFull,
    finalDeclarationDates = Seq(
      DatesModel(startDateTaxYear2023, endDateTaxYear2023, crystallisedDeadlineTaxYear2023_2024, "C", isFinalDec = true, obligationType = "Crystallisation"),
      DatesModel(startDateTaxYear2024, endDateTaxYear2024, crystallisedDeadlineTaxYear2024_2025, "C", isFinalDec = true, obligationType = "Crystallisation")
    ),
    currentTaxYear = taxYear2023,
    showPrevTaxYears = true
  )

  val obligationsViewModelSimple: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = quarterlyObligationDatesSimple,
    finalDeclarationDates = Seq(
      DatesModel(startDateTaxYear2023, endDateTaxYear2023, crystallisedDeadlineTaxYear2023_2024, "C", isFinalDec = true, obligationType = "Crystallisation")
    ),
    currentTaxYear = taxYear2023,
    showPrevTaxYears = true
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel("1234", List(SingleObligationModel(
      LocalDate.of(taxYear2023, 1, 6),
      LocalDate.of(taxYear2023, 4, 5),
      LocalDate.of(taxYear2023, 5, 5),
      "Quarterly",
      None,
      "#001",
      status = StatusFulfilled
    ),
      SingleObligationModel(
        LocalDate.of(taxYear2024, 1, 6),
        LocalDate.of(taxYear2024, 4, 5),
        LocalDate.of(taxYear2024, 5, 5),
        "Quarterly",
        None,
        "#002",
        StatusFulfilled
      )
    ))
  ))
}
