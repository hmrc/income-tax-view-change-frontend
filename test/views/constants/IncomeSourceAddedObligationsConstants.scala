/*
 * Copyright 2025 HM Revenue & Customs
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

package views.constants

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._

import java.time.LocalDate

object IncomeSourceAddedObligationsConstants {

  val aug5th2024 = "5 August 2024"
  val jan31st2025 = "31 January 2025"
  val jan31st2026 = "31 January 2026"

  val addIncomeSourceShowURL: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url
  val addIncomeSourceShowAgentURL: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent().url

  val nextUpdatesUrl: String = controllers.routes.NextUpdatesController.show().url
  val nextUpdatesAgentUrl: String = controllers.routes.NextUpdatesController.showAgent().url

  def reportingFrequencyPageUrl(isAgent: Boolean): String = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent).url

  val manageBusinessesUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val manageBusinessesAgentUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url

  val testId: String = "XAIS00000000005"
  val submitSoftwareUrl = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
  val testName = Some("Test Name")

  val day: LocalDate = LocalDate.of(2022, 1, 1)

  val dayFirstQuarter2024_2025: LocalDate = LocalDate.of(2024, 5, 10)
  val dayAfterFirstQuarterDeadline2024_2025: LocalDate = LocalDate.of(2024, 8, 10)
  val dayAfterSecondQuarterDeadline2024_2025: LocalDate = LocalDate.of(2024, 11, 10)
  val dayAfterThirdQuarterDeadline2024_2025: LocalDate = LocalDate.of(2025, 2, 10)

  val dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025: LocalDate = LocalDate.of(2025, 2, 10)
  val dayBeforeLastQuarterlyDeadline2023_2024: LocalDate = LocalDate.of(2024, 5, 1)
  val dayAfterFinalDeclarationDeadline2023_2024: LocalDate = LocalDate.of(2025, 2, 4)

  val dayJustAfterTaxYearStart2024_2025: LocalDate = LocalDate.of(2024, 4, 10)

  val dayJustBeforeTaxYearEnd2023_2024: LocalDate = LocalDate.of(2024, 4, 4)

  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    Seq(finalDeclarationDates),
    2023,
    showPrevTaxYears = true
  )

  val viewModelWithCurrentYearAnnual: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Nil,
      finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithCurrentYearQuarterly: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
      finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithAnnualYearThenAnnualYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Nil,
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithAnnualYearThenFullQuarterlyYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelOneQuarterYearThenAnnualYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(Seq(DatesModel(
        taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1),
        "#004", isFinalDec = false, obligationType = "Quarterly"
      ))),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelTwoQuartersYearThenAnnualYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(Seq(
        DatesModel(
          taxYear2023_2024q3startDate, taxYear2023_2024q3endDate, taxYear2023_2024q3endDate.plusMonths(1),
          "#003", isFinalDec = false, obligationType = "Quarterly"
        ),
        DatesModel(taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1),
          "#004", isFinalDec = false, obligationType = "Quarterly"
        )
      )),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelOneQuarterYearThenQuarterlyYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(Seq(
        DatesModel(
          taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1),
          "#004", isFinalDec = false, obligationType = "Quarterly"
        )),
        taxYear2024_2025quarterlyDates
      ),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelTwoQuarterYearThenQuarterlyYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(
        Seq(
          DatesModel(
            taxYear2023_2024q3startDate, taxYear2023_2024q3endDate, taxYear2023_2024q3endDate.plusMonths(1),
            "#003", isFinalDec = false, obligationType = "Quarterly"
          ),
          DatesModel(
            taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1),
            "#004", isFinalDec = false, obligationType = "Quarterly"
          )
        ),
        taxYear2024_2025quarterlyDates
      ),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithFullQuarterlyYearThenAnnualYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(taxYear2023_2024quarterlyDates),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithFullQuarterlyYearThenFullQuarterlyYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(taxYear2023_2024quarterlyDates, taxYear2024_2025quarterlyDates),
      finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Nil,
      finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
      currentTaxYear = 2025,
      showPrevTaxYears = true
    )

  val viewModelWithFutureBusinessStartReportingAnnually: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Nil,
      finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
      currentTaxYear = 2024,
      showPrevTaxYears = true
    )

  val viewModelWithFutureBusinessStartReportingQuarterly: ObligationsViewModel =
    ObligationsViewModel(
      quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
      finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
      currentTaxYear = 2024,
      showPrevTaxYears = true
    )

}
