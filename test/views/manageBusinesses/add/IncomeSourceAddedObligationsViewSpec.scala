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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.ChosenReportingMethod
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._
import testUtils.ViewSpec
import views.html.manageBusinesses.add.IncomeSourceAddedObligations

import java.time.LocalDate

class IncomeSourceAddedObligationsViewSpec extends ViewSpec {

  object IncomeSourceAddedMessages {
    val h1ForeignProperty: String = "Foreign property"
    val h1UKProperty: String = "UK property"
    val h1SelfEmployment: String = "Test Name"
    val headingBase: String = "has been added to your account"
    val submitTaxReturn: String = "Submit your tax return"
    val submitSoftware: String = "Submit updates in software"
    val deadlinesHeading: String = "Your revised deadlines"
    val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
    val finalDecHeading: String = "Submit final declarations and pay your tax"
    val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
    val tableHeading1: String = "Tax year"
    val tableHeading2: String = "Deadline"
    val prevYearsHeading: String = "Previous tax years"
    val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
    val viewAllBusinessesText: String = "View all your businesses"
    val insetSingleOverdueUpdateText: (String, Int) => String = (month, year) => s"As your business started in $month $year, you have 1 overdue update."
    val insetMultipleOverdueUpdateText: (String, Int, Int) => String = (month, year, overdueUpdates) => s"As your business started in $month $year, you have $overdueUpdates overdue updates."
    val insetWarningOverdueUpdatesText: Int => String = startTaxYear => s"You must make sure that you have sent all the required income and expenses for tax years earlier than $startTaxYear to ${startTaxYear + 1}."
  }

  val testId: String = "XAIS00000000005"

  val view: IncomeSourceAddedObligations = app.injector.instanceOf[IncomeSourceAddedObligations]
  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

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

  val viewModelWithCurrentYearAnnual: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Nil,
    finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithCurrentYearQuarterly: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
    finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithAnnualYearThenAnnualYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Nil,
    finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithAnnualYearThenFullQuarterlyYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
    finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelOneQuarterYearThenAnnualYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(Seq(DatesModel(
      taxYear2023_2024q4startDate, taxYear2023_2024q4endDate, taxYear2023_2024q4endDate.plusMonths(1),
      "#004", isFinalDec = false, obligationType = "Quarterly"
    ))),
    finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelTwoQuartersYearThenAnnualYear: ObligationsViewModel = ObligationsViewModel(
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

  val viewModelOneQuarterYearThenQuarterlyYear: ObligationsViewModel = ObligationsViewModel(
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

  val viewModelTwoQuarterYearThenQuarterlyYear: ObligationsViewModel = ObligationsViewModel(
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

  val viewModelWithFullQuarterlyYearThenAnnualYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(taxYear2023_2024quarterlyDates),
    finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithFullQuarterlyYearThenFullQuarterlyYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(taxYear2023_2024quarterlyDates, taxYear2024_2025quarterlyDates),
    finalDeclarationDates = Seq(finalDeclaration2023_2024taxYear, finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Nil,
    finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
    currentTaxYear = 2025,
    showPrevTaxYears = true
  )

  val viewModelWithFutureBusinessStartReportingAnnually: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Nil,
    finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
    currentTaxYear = 2024,
    showPrevTaxYears = true
  )

  val viewModelWithFutureBusinessStartReportingQuarterly: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
    finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
    currentTaxYear = 2024,
    showPrevTaxYears = true
  )

  val validUKPropertyBusinessCall: Html = view(
    viewModel, isAgent = false, UkProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validUKPropertyBusinessAgentCall: Html = view(
    viewModel, isAgent = true, UkProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)

  val validForeignPropertyBusinessCall: Html = view(
    viewModel, isAgent = false, ForeignProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validForeignPropertyBusinessAgentCall: Html = view(
    viewModel, isAgent = true, ForeignProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)

  val validSoleTreaderBusinessCall: Html = view(
    viewModel, isAgent = false, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validSoleTreaderBusinessAgentCall: Html = view(
    viewModel, isAgent = true, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)

  val validCallWithData: Html = view(
    viewModelWithAllData, isAgent = false, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validAgentCallWithData: Html = view(
    viewModelWithAllData, isAgent = true, SelfEmployment, Some("Test Name"), day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)


  val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
    viewModelWithCurrentYearAnnual, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validCurrentTaxYearQuarterlyCallNoOverdue: Html = view(
    viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validCurrentTaxYearQuarterlyCallOneOverdue: Html = view(
    viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validCurrentTaxYearQuarterlyCallMultipleOverdue: Html = view(
    viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)

  val validAnnualThenAnnualCallNoOverdue: Html = view(
    viewModelWithAnnualYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validAnnualThenAnnualCallOneOverdue: Html = view(
    viewModelWithAnnualYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"),
    dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)

  val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterSecondQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)

  val validOneQuarterThenAnnualCallNoOverdue: Html = view(
    viewModelOneQuarterYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayBeforeLastQuarterlyDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validOneQuarterThenAnnualCallOneQuarterlyOverdue: Html = view(
    viewModelOneQuarterYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validTwoQuartersThenAnnualCallTwoQuarterlyOverdue: Html = view(
    viewModelTwoQuartersYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue: Html = view(
    viewModelOneQuarterYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"),
    dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue: Html = view(
    viewModelTwoQuartersYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"),
    dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)

  val validOneQuarterThenQuarterlyCallNoOverdue: Html = view(
    viewModelOneQuarterYearThenQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayBeforeLastQuarterlyDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validOneQuarterThenQuarterlyCallOneQuarterlyOverdue: Html = view(
    viewModelOneQuarterYearThenQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue: Html = view(
    viewModelTwoQuarterYearThenQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue: Html = view(
    viewModelTwoQuarterYearThenQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue: Html = view(
    viewModelTwoQuarterYearThenQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)

  val validHistoricAnnualThenAnnualCallNoOverdue: Html = view(
    viewModelWithAnnualYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = true, reportingMethod = ChosenReportingMethod.Annual)
  val validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = true, reportingMethod = ChosenReportingMethod.Hybrid)
  val validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = true, reportingMethod = ChosenReportingMethod.Hybrid)

  val validFutureAnnualCallSameTaxYear: Html = view(
    viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)


  val validFullQuarterlyThenAnnualCallBeforeQ4Deadline: Html = view(
    viewModelWithFullQuarterlyYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayJustAfterTaxYearStart2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validFullQuarterlyThenAnnualCallAfterQ4Deadline: Html = view(
    viewModelWithFullQuarterlyYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)
  val validFullQuarterlyThenAnnualCallAfterFinalDecDeadline: Html = view(
    viewModelWithFullQuarterlyYearThenAnnualYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Hybrid)

  val validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline: Html = view(
    viewModelWithFullQuarterlyYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayJustAfterTaxYearStart2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline: Html = view(
    viewModelWithFullQuarterlyYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)
  val validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline: Html = view(
    viewModelWithFullQuarterlyYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, Some("Test Name"), dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)

  val validFutureTaxYearAnnualCall: Html = view(
    viewModelWithFutureBusinessStartReportingAnnually, isAgent = false, SelfEmployment, Some("Test Name"),
    dayJustBeforeTaxYearEnd2023_2024, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual)
  val validFutureTaxYearQuarterlyCall: Html = view(
    viewModelWithFutureBusinessStartReportingQuarterly, isAgent = false, SelfEmployment, Some("Test Name"),
    dayJustBeforeTaxYearEnd2023_2024, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly)


  val validCurrentTaxYearDefaultAnnualCallNoOverdue: Html = view(
    viewModelWithCurrentYearAnnual, isAgent = false, SelfEmployment, Some("Test Name"), dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.DefaultAnnual)

  val addIncomeSourceShowURL: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url
  val addIncomeSourceShowAgentURL: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent().url

  val nextUpdatesUrl: String = controllers.routes.NextUpdatesController.show().url
  val nextUpdatesAgentUrl: String = controllers.routes.NextUpdatesController.showAgent.url

  val manageBusinessesUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val manageBusinessesAgentUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url

  val submitSoftwareUrl = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  "Income Source Added Obligations - Individual" should {
    "Display the correct banner message" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1UKProperty

        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1UKProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1ForeignProperty

        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1ForeignProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }
      }
      "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1SelfEmployment

        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1SelfEmployment + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }
      }
    }

    "Not display a back button" in new Setup(validCallWithData) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }

    "Display the correct heading for deadlines" in new Setup(validCallWithData) {
      document.getElementById("deadlines").text.contains(IncomeSourceAddedMessages.deadlinesHeading)
    }

    "Not display inset warning text because there are no overdue obligations" when {
      "The business started in the current tax year" when {
        "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }

        "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }
      }

      "The business started in the previous tax year (CY-1)" when {
        "It is reporting annually for both CY-1 and CY" in new Setup(validAnnualThenAnnualCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }

        "It is reporting annually for CY-1 and quarterly for CY" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }

        "It is reporting quarterly for CY-1 and annually for CY" in new Setup(validOneQuarterThenAnnualCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }

        "It is reporting quarterly for both CY-1 and CY" in new Setup(validOneQuarterThenQuarterlyCallNoOverdue) {
          withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
        }
      }

      "The business started before CY-1 and has a historic start date" in new Setup(validHistoricAnnualThenAnnualCallNoOverdue) {
        withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
      }

      "The business is going to start at a future date in CY" in new Setup(validFutureAnnualCallSameTaxYear) {
        withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
      }

      "The business is going to start at a future date in CY+1" in new Setup(validFutureTaxYearQuarterlyCall) {
        withClue("Inset text was present when it should not have been.")(Option(document.getElementById("warning-inset")).isDefined shouldBe false)
      }
    }

    "Display the correct inset warning text" when {
      "The business started in the current tax year" when {
        "It is reporting quarterly and there is one overdue obligation" in new Setup(validCurrentTaxYearQuarterlyCallOneOverdue) {
          Option(document.getElementById("warning-inset")) match {
            case Some(insetText) =>
              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            case None => fail("No inset text was found")
          }
        }

        "It is reporting quarterly and there are multiple overdue obligations" in new Setup(validCurrentTaxYearQuarterlyCallMultipleOverdue) {
          Option(document.getElementById("warning-inset")) match {
            case Some(insetText) =>
              insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "3 overdue updates"
            case None => fail("No inset text was found")
          }
        }
      }

      "The business started in the previous tax year (CY-1)" when {
        "It is reporting annually for both CY-1 and CY" when {
          "There is one overdue obligation" in new Setup(validAnnualThenAnnualCallOneOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No inset text was found")
            }
          }
        }

        "It is reporting annually for CY-1 and quarterly for CY" when {
          "There is one overdue quarterly obligation" in new Setup(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations" in new Setup(validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 2 overdue updates for 6 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "2 overdue updates"
              case None => fail("No inset text was found")
            }
          }

          "There is one overdue annual obligation and multiple overdue quarterly obligations" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
            Option(document.getElementById("annual-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No annual inset text was found")
            }

            Option(document.getElementById("quarterly-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "3 overdue updates"
              case None => fail("No quarterly inset text was found")
            }
          }
        }

        "It is reporting quarterly for CY-1 and annually for CY" when {
          "There is one overdue quarterly obligation" in new Setup(validOneQuarterThenAnnualCallOneQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations" in new Setup(validTwoQuartersThenAnnualCallTwoQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "2 overdue updates"
              case None => fail("No inset text was found")
            }
          }

          "There is one overdue quarterly obligation and one overdue annual obligation" in new Setup(validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue) {
            Option(document.getElementById("annual-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No annual inset text was found")
            }

            Option(document.getElementById("quarterly-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No quarterly inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations and one overdue annual obligation" in new Setup(validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue) {
            Option(document.getElementById("annual-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No annual inset text was found")
            }

            Option(document.getElementById("quarterly-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "2 overdue updates"
              case None => fail("No quarterly inset text was found")
            }
          }
        }

        "It is reporting quarterly for both CY-1 and CY" when {
          "There is one overdue quarterly obligation from CY-1" in new Setup(validOneQuarterThenQuarterlyCallOneQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations from CY-1" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "2 overdue updates"
              case None => fail("No inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations from CY-1 and CY" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue) {
            Option(document.getElementById("warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 3 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "3 overdue updates"
              case None => fail("No inset text was found")
            }
          }

          "There are multiple overdue quarterly obligations from CY-1 and CY, and one overdue annual obligation from CY-1" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue) {
            Option(document.getElementById("annual-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
                insetText.select("b").text() shouldBe "1 overdue update"
              case None => fail("No annual inset text was found")
            }

            Option(document.getElementById("quarterly-warning-inset")) match {
              case Some(insetText) =>
                insetText.text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
                insetText.select("b").text() shouldBe "4 overdue updates"
              case None => fail("No quarterly inset text was found")
            }
          }
        }
      }

      "The business started before CY-1 and has a historic start date" when {
        "There is one overdue obligation" in new Setup(validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
          Option(document.getElementById("warning-inset")) match {
            case Some(insetText) =>
              insetText.text() shouldBe "You have 1 overdue update. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
              insetText.select("b").text() shouldBe "1 overdue update"
            case None => fail("No inset text was found")
          }
        }

        "There are multiple overdue obligations" in new Setup(validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
          Option(document.getElementById("warning-inset")) match {
            case Some(insetText) =>
              insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
              insetText.select("b").text() shouldBe "4 overdue updates"
            case None => fail("No inset text was found")
          }
        }
      }
    }

    "Display the correct revised deadlines text" when {
      "The business started in the current tax year" when {
        "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
          Option(document.getElementById("final-declaration")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
            case _ => fail("No upcoming annual obligation text was found.")
          }
        }

        "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
          Option(document.getElementById("quarterly-list")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
            case _ => fail("No upcoming quarterly obligation text was found.")
          }

          Option(document.getElementById("obligations-list")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
            case _ => fail("No upcoming annual obligation text was found.")
          }
        }
      }

      "The business started in the previous tax year (CY-1)" when {
        "It is reporting annually for both CY-1 and CY" when {
          "The current date is before the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallNoOverdue) {
            Option(document.getElementById("final-declaration")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallOneOverdue) {
            Option(document.getElementById("final-declaration")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }
        }

        "It is reporting annually for CY-1 and quarterly for CY" when {
          "The current date is before the CY-1 final declaration deadline and CY Q1 deadline" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is before the CY-1 final declaration deadline and between CY Q1 and Q2 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 November 2024 for the quarterly period 6 July 2024 to 5 October 2024"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 November 2024"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the CY-1 final declaration deadline and between CY Q2 and Q3 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the CY-1 final declaration deadline and the CY Q3 deadline" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 May 2025 for the quarterly period 6 January 2025 to 5 April 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2025"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }
        }

        "It is reporting quarterly for CY-1 and Annually for CY" when {
          "The current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallBeforeQ4Deadline) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterQ4Deadline) {
            withClue("Quarterly list id was present when it should not have been.")(Option(document.getElementById("quarterly-list")).isDefined shouldBe false)

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterFinalDecDeadline) {
            withClue("Quarterly list id was present when it should not have been.")(Option(document.getElementById("quarterly-list")).isDefined shouldBe false)

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }
        }

        "It is reporting quarterly for both CY-1 and CY" when {
          "The current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is between the CY-1 Q4 deadline and the CY Q1 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }

          "The current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline) {
            Option(document.getElementById("quarterly-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
                upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
              case _ => fail("No upcoming quarterly obligation text was found.")
            }

            Option(document.getElementById("obligations-list")) match {
              case Some(upcomingDeadlineMessage) =>
                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
              case _ => fail("No upcoming annual obligation text was found.")
            }
          }
        }
      }

      "The business will start in the next tax year" when {
        "It is reporting annually" in new Setup(validFutureTaxYearAnnualCall) {
          Option(document.getElementById("final-declaration")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
            case _ => fail("No upcoming annual obligation text was found.")
          }
        }

        "It is reporting quarterly" in new Setup(validFutureTaxYearQuarterlyCall) {
          Option(document.getElementById("quarterly-list")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
            case _ => fail("No upcoming quarterly obligation text was found.")
          }

          Option(document.getElementById("obligations-list")) match {
            case Some(upcomingDeadlineMessage) =>
              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
            case _ => fail("No upcoming annual obligation text was found.")
          }
        }
      }
    }

    "Display the view upcoming updates link when there are no overdue obligations" in new Setup(validCallWithData) {
      Option(document.getElementById("view-upcoming-updates")) match {
        case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your upcoming updates"
        case None => fail("No upcoming updates link was found")
      }
    }
    "Display the view overdue and upcoming updates link when there are no overdue obligations" in new Setup(validCurrentTaxYearQuarterlyCallOneOverdue) {
      Option(document.getElementById("view-upcoming-updates")) match {
        case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your overdue and upcoming updates"
        case None => fail("No upcoming updates link was found")
      }
    }

    "Render the view all your business link" in new Setup(validCallWithData) {
      document.getElementById("view-all-businesses-link").text() shouldBe IncomeSourceAddedMessages.viewAllBusinessesText
      document.getElementById("view-all-businesses-link").select("a").attr("href") shouldBe manageBusinessesUrl
    }

    "Display the correct change reporting frequency text" when {
      // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
      "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
        Option(document.getElementById("change-frequency")) match {
          case Some(changeFrequency) =>
            changeFrequency.text() shouldBe "You can decide at any time to opt out of quarterly reporting and report annually for all your businesses on your reporting frequency page."
            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
          case None => fail("No upcoming updates link was found")
        }
      }

      "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
        Option(document.getElementById("change-frequency")) match {
          case Some(changeFrequency) =>
            changeFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
          case None => fail("No upcoming updates link was found")
        }
      }

      "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
        Option(document.getElementById("change-frequency")) match {
          case Some(changeFrequency) =>
            changeFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
          case None => fail("No upcoming updates link was found")
        }
      }

      "It defaults to annual reporting because the reporting methods page was skipped" in new Setup(validCurrentTaxYearDefaultAnnualCallNoOverdue) {
        Option(document.getElementById("change-frequency")) match {
          case Some(changeFrequency) =>
            changeFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
          case None => fail("No upcoming updates link was found")
        }
      }
    }

    "Display the correct submit tax return / updates subheading and text" when {
      "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitTaxReturn

        Option(document.getElementById("submit-text")) match {
          case Some(upcomingDeadlineMessage) =>
            upcomingDeadlineMessage.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
          case _ => fail("No submit text was found.")
        }
      }

      "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitSoftware

        Option(document.getElementById("submit-text")) match {
          case Some(upcomingDeadlineMessage) =>
            upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
          case _ => fail("No submit text was found.")
        }
      }

      "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitSoftware

        Option(document.getElementById("quarterly-submit-text")) match {
          case Some(upcomingDeadlineMessage) =>
            upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
          case _ => fail("No submit text was found.")
        }

        Option(document.getElementById("annual-submit-text")) match {
          case Some(upcomingDeadlineMessage) =>
            upcomingDeadlineMessage.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          case _ => fail("No submit text was found.")
        }
      }
    }
  }

  "Income Source Added Obligations - Agent" should {
    "Display the correct banner message and heading" when {
      "Business type is UK Property Business" in new Setup(validUKPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1UKProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1UKProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitTaxReturn
      }
      "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1ForeignProperty


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1ForeignProperty + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitTaxReturn
      }
      "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessAgentCall) {
        val banner: Element = layoutContent.getElementsByTag("h1").first()
        banner.text() shouldBe IncomeSourceAddedMessages.h1SelfEmployment


        val subText: Option[Element] = layoutContent.select("div").eq(3)

        subText match {
          case Some(heading) => heading.text shouldBe IncomeSourceAddedMessages.h1SelfEmployment + " " + IncomeSourceAddedMessages.headingBase
          case _ => fail("No 2nd h2 element found.")
        }

        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
        subHeading.text shouldBe IncomeSourceAddedMessages.submitTaxReturn
      }
    }

    "Not display the view overdue and upcoming updates link when the user does not have them" in new Setup(validAgentCallWithData) {
      Option(document.getElementById("view-overdue-upcoming-updates")).isDefined shouldBe false
    }

    "Not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessAgentCall) {
      Option(layoutContent.getElementById("quarterly")) shouldBe None
      Option(layoutContent.getElementById("prevyears")) shouldBe None
    }

    "render the view all your businesses link" in new Setup(validAgentCallWithData) {
      document.getElementById("view-all-businesses-link").text() shouldBe IncomeSourceAddedMessages.viewAllBusinessesText
    }
  }
}
