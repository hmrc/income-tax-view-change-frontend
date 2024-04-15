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

import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}

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

  val eopsDeadlineTaxYear2022 = LocalDate.of(2024, 1, 31)
  val eopsDeadlineTaxYear2023 = LocalDate.of(2025, 1, 31)
  val eopsDeadlineTaxYear2024 = LocalDate.of(2026, 1, 31)

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

  val viewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = quarterlyObligationDatesFull,
    finalDeclarationDates = Seq(
      DatesModel(startDateTaxYear2023, endDateTaxYear2023, eopsDeadlineTaxYear2023, "C", isFinalDec = true, obligationType = "Crystallised"),
      DatesModel(startDateTaxYear2024, endDateTaxYear2024, eopsDeadlineTaxYear2024, "C", isFinalDec = true, obligationType = "Crystallised")
    ),
    currentTaxYear = taxYear2023,
    showPrevTaxYears = true
  )

  val obligationsViewModelSimple: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = quarterlyObligationDatesSimple,
    finalDeclarationDates = Seq(
      DatesModel(startDateTaxYear2023, endDateTaxYear2023, eopsDeadlineTaxYear2023, "C", isFinalDec = true, obligationType = "Crystallised")
    ),
    currentTaxYear = taxYear2023,
    showPrevTaxYears = true
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel("1234", List(NextUpdateModel(
      LocalDate.of(taxYear2023, 1, 6),
      LocalDate.of(taxYear2023, 4, 5),
      LocalDate.of(taxYear2023, 5, 5),
      "Quarterly",
      None,
      "#001"
    ),
      NextUpdateModel(
        LocalDate.of(taxYear2024, 1, 6),
        LocalDate.of(taxYear2024, 4, 5),
        LocalDate.of(taxYear2024, 5, 5),
        "Quarterly",
        None,
        "#002"
      )
    ))
  ))
}
