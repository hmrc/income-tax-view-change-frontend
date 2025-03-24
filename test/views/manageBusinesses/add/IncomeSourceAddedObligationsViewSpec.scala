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
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.ViewSpec
import views.constants.IncomeSourceAddedObligationsConstants._
import views.html.manageBusinesses.add.IncomeSourceAddedObligationsView
import views.messages.IncomeSourceAddedMessages._

class IncomeSourceAddedObligationsViewSpec extends ViewSpec {

  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

  val view: IncomeSourceAddedObligationsView = app.injector.instanceOf[IncomeSourceAddedObligationsView]

  object SelectorHelper {
    val yourRevisedDeadlinesH2 = "your-revised-deadlines"
    val warningInset = "warning-inset"
    val quarterlyList = "quarterly-list"
    val obligationsList = "obligations-list"
    val annualInset = "annual-warning-inset"
    val quarterlyInset = "quarterly-warning-inset"
    val finalDeclaration = "final-declaration"
    val annualCompatibleSoftwareParagraph = "submit-via-compatible-software-annual"
    val quarterlyCompatibleSoftwareParagraph = "submit-via-compatible-software-quarterly"
  }

  "Income Source Added Obligations - Individual" when {

    "Quarterly" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
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

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for both CY-1 and CY" in {

              val validOneQuarterThenQuarterlyCallNoOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenQuarterlyCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          "the business is going to start at a future date in CY+1" in {

            val validFutureTaxYearQuarterlyCall: Html =
              view(
                viewModel = viewModelWithFutureBusinessStartReportingQuarterly,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayJustBeforeTaxYearEnd2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validFutureTaxYearQuarterlyCall.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }
        }

        s"display the correct inset warning text - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting quarterly and there is one overdue obligation" in {

              val validCurrentTaxYearQuarterlyCallOneOverdue: Html = view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallOneOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "it is reporting quarterly and there are multiple overdue obligations" in {

            val validCurrentTaxYearQuarterlyCallMultipleOverdue: Html =
              view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayAfterThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallMultipleOverdue.body)

            val insetText = document.getElementById(SelectorHelper.warningInset)
            insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "3 overdue updates"
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting quarterly for both CY-1 and CY" when {

            "there is one overdue quarterly obligation from CY-1" in {

              val validOneQuarterThenQuarterlyCallOneQuarterlyOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            "there are multiple overdue quarterly obligations from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue: Html = view(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue: Html = view(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 3 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "3 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY, and one overdue annual obligation from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue.body)

              val annualInsetText = document.getElementById(SelectorHelper.annualInset)
              val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "4 overdue updates"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
              val obligationsList = document.getElementById(SelectorHelper.obligationsList)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

              quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              quarterlyList.select("b").text() shouldBe aug5th2024

              obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              obligationsList.select("b").text() shouldBe jan31st2026
            }
          }

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for both CY-1 and CY" when {

              "the current date is before the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayJustAfterTaxYearStart2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                quarterlyList.select("b").text() shouldBe "5 May 2024"

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is between the CY-1 Q4 deadline and the CY Q1 deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayFirstQuarter2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
                quarterlyList.select("b").text() shouldBe aug5th2024

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 final declaration deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
                quarterlyList.select("b").text() shouldBe "5 February 2025"

                obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                obligationsList.select("b").text() shouldBe jan31st2026
              }
            }
          }

          "the business will start in the next tax year" when {

            "it is reporting quarterly" in {

              val validFutureTaxYearQuarterlyCall: Html = view(
                viewModel = viewModelWithFutureBusinessStartReportingQuarterly,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayJustBeforeTaxYearEnd2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validFutureTaxYearQuarterlyCall.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
              val obligationsList = document.getElementById(SelectorHelper.obligationsList)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

              quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              quarterlyList.select("b").text() shouldBe aug5th2024

              obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              obligationsList.select("b").text() shouldBe jan31st2026
            }
          }
        }

        s"display the View overdue and upcoming updates link when there are no overdue obligations - $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallOneOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFirstQuarterDeadline2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallOneOverdue.body)
          val viewUpcomingUpdatesLink = document.getElementById("view-upcoming-updates")

          viewUpcomingUpdatesLink.text() shouldBe "View your overdue and upcoming updates"
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"not display the change reporting frequency text - $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)

          Option(document.getElementById("change-frequency")) shouldBe None
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val subHeading: Element = layoutContent.getElementsByTag("h2").last()
          val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
          val compatibleSoftwareLink = compatibleSoftwareParagraph.select("a")

          subHeading.text shouldBe submitUpdatesInSoftware

          compatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
          compatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
          compatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl
        }
      }
    }

    "Annual" should {
      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Annually for both CY-1 and CY" in {

              val validAnnualThenAnnualCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = SelfEmployment,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          "the business started before CY-1 and has a historic start date" in {

            val validHistoricAnnualThenAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenAnnualYear,
              isAgent = false,
              incomeSourceType = SelfEmployment,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

            val document: Document = Jsoup.parse(validHistoricAnnualThenAnnualCallNoOverdue.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }

          "the business is going to start at a future date in CY" in {

            val validFutureAnnualCallSameTaxYear: Html =
              view(
                viewModel = viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Annual
                ,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

            val document: Document = Jsoup.parse(validFutureAnnualCallSameTaxYear.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting annually for both CY-1 and CY" when {

            "there is one overdue obligation" in {

              val validAnnualThenAnnualCallOneOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallOneOverdue.body)

              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting annually" in {

              val validCurrentTaxYearAnnualCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithCurrentYearAnnual,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              finalDeclarationContent.select("b").text() shouldBe jan31st2026
            }
          }

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting annually for both CY-1 and CY" when {

              "the current date is before the deadline for CY-1 final declaration" in {

                val validAnnualThenAnnualCallNoOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

                val document: Document = Jsoup.parse(validAnnualThenAnnualCallNoOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
                finalDeclarationContent.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
                finalDeclarationContent.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the deadline for CY-1 final declaration" in {

                val validAnnualThenAnnualCallOneOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)
                val document: Document = Jsoup.parse(validAnnualThenAnnualCallOneOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
                finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
                finalDeclarationContent.select("b").text() shouldBe jan31st2026
              }
            }
          }

          "the business will start in the next tax year" when {

            "it is reporting annually" in {

              val validFutureTaxYearAnnualCall: Html =
                view(
                  viewModel = viewModelWithFutureBusinessStartReportingAnnually,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayJustBeforeTaxYearEnd2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validFutureTaxYearAnnualCall.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              finalDeclarationContent.select("b").text() shouldBe jan31st2026
            }
          }
        }

        s"display the View upcoming updates link when there are no overdue obligations - $incomeSourceType" in {

          val validCallWithData: Html =
            view(
              viewModel = viewModelWithAllData,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

          val document: Document = Jsoup.parse(validCallWithData.body)
          val viewUpcomingUpdatesLink = document.getElementById("view-upcoming-updates")

          viewUpcomingUpdatesLink.text() shouldBe "View your upcoming updates"
        }

        s"render the view all your business link - $incomeSourceType" in {

          val validCallWithData: Html =
            view(
              viewModel = viewModelWithAllData,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

          val document: Document = Jsoup.parse(validCallWithData.body)

          document.getElementById("view-all-businesses-link").text() shouldBe viewAllBusinessesText
          document.getElementById("view-all-businesses-link").select("a").attr("href") shouldBe manageBusinessesUrl
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting Annually" in {

            val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

            val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "You are set to report annually for your new property. Find out more about your reporting frequency."
              case _ => reportingFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
            }

            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(false)
          }

          "reporting Hybrid" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }
            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(false)
          }

          "reporting methods page was skipped - defaults to Annual reporting " in {

            val validCurrentTaxYearDefaultAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.DefaultAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

            val document: Document = Jsoup.parse(validCurrentTaxYearDefaultAnnualCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "You are set to report annually for your new property. Find out more about your reporting frequency."
              case _ => reportingFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
            }

            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(false)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting Annually" in {

            val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

            val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
            val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)


            subHeading.text shouldBe submitUpdatesInSoftware
            compatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or software compatible with Making Tax Digital for Income Tax (opens in new tab)."
          }

          "reporting Quarterly" in {

            val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
            val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val compatibleSoftwareLink = compatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            compatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            compatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            compatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl
          }

          "reporting Hybrid" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }

    "QuarterlyAnnual" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Quarterly for CY-1 and Annually for CY" in {

              val validOneQuarterThenAnnualCallNoOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenAnnualYear,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }

          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting quarterly for CY-1 and annually for CY" when {

            "there is one overdue quarterly obligation" in {

              val validOneQuarterThenAnnualCallOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelOneQuarterYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "there are multiple overdue quarterly obligations" in {

            val validTwoQuartersThenAnnualCallTwoQuarterlyOverdue: Html = view(
              viewModel = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

            val document: Document = Jsoup.parse(validTwoQuartersThenAnnualCallTwoQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "2 overdue updates"
          }

          "there is one overdue quarterly obligation and one overdue annual obligation" in {

            val validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue: Html =
              view(
                viewModel = viewModelOneQuarterYearThenAnnualYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById(SelectorHelper.annualInset)
            val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue quarterly obligations and one overdue annual obligation" in {

            val validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue: Html = view(
              viewModel = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

            val document: Document = Jsoup.parse(validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById(SelectorHelper.annualInset)
            val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "2 overdue updates"
          }
        }

        s"the business started before CY-1 and has a historic start date - $incomeSourceType" when {

          "there is one overdue obligation" in {

            val validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFirstQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 1 overdue update. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue obligations" in {

            val validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterThirdQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "4 overdue updates"
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for CY-1 and Annually for CY" when {

              "the current date is before the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenAnnualCallBeforeQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayJustAfterTaxYearStart2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallBeforeQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                quarterlyList.select("b").text() shouldBe "5 May 2024"

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenAnnualCallAfterQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayFirstQuarter2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallAfterQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                Option(quarterlyList) shouldBe None

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 final declaration deadline" in {

                val validFullQuarterlyThenAnnualCallAfterFinalDecDeadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallAfterFinalDecDeadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                Option(quarterlyList) shouldBe None

                obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                obligationsList.select("b").text() shouldBe jan31st2026
              }
            }
          }
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting QuarterlyAnnual" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }
            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(false)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting QuarterlyAnnual" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }

    "AnnualQuarterly" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Annually for CY-1 and Quarterly for CY" in {

              val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting annually for CY-1 and quarterly for CY" when {

            "there is one overdue quarterly obligation" in {

              val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFirstQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            "there are multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterSecondQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates for 6 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there is one overdue annual obligation and multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)

              val annualInsetText = document.getElementById(SelectorHelper.annualInset)
              val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "3 overdue updates"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting annually for CY-1 and quarterly for CY" when {

              "the current date is before the CY-1 final declaration deadline and CY Q1 deadline" in {

                val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is before the CY-1 final declaration deadline and between CY Q1 and Q2 deadlines" in {

                val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFirstQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is after the CY-1 final declaration deadline and between CY Q2 and Q3 deadlines" in {

                val validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is after the CY-1 final declaration deadline and the CY Q3 deadline" in {

                val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html =
                  view(
                    viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                    isAgent = false,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayAfterThirdQuarterDeadline2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }
            }
          }
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting AnnualQuarterly" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }
            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(false)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting AnnualQuarterly" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }
  }

  "Income Source Added Obligations - Agent" when {

    "Quarterly" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for both CY-1 and CY" in {

              val validOneQuarterThenQuarterlyCallNoOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenQuarterlyYear,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenQuarterlyCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          "the business is going to start at a future date in CY+1" in {

            val validFutureTaxYearQuarterlyCall: Html =
              view(
                viewModel = viewModelWithFutureBusinessStartReportingQuarterly,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayJustBeforeTaxYearEnd2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validFutureTaxYearQuarterlyCall.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }
        }

        s"display the correct inset warning text - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting quarterly and there is one overdue obligation" in {

              val validCurrentTaxYearQuarterlyCallOneOverdue: Html = view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallOneOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "it is reporting quarterly and there are multiple overdue obligations" in {

            val validCurrentTaxYearQuarterlyCallMultipleOverdue: Html =
              view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayAfterThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallMultipleOverdue.body)

            val insetText = document.getElementById(SelectorHelper.warningInset)
            insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "3 overdue updates"
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting quarterly for both CY-1 and CY" when {

            "there is one overdue quarterly obligation from CY-1" in {

              val validOneQuarterThenQuarterlyCallOneQuarterlyOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            "there are multiple overdue quarterly obligations from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue: Html = view(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue: Html = view(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 3 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "3 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY, and one overdue annual obligation from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue.body)

              val annualInsetText = document.getElementById(SelectorHelper.annualInset)
              val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "4 overdue updates"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
              val obligationsList = document.getElementById(SelectorHelper.obligationsList)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

              quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              quarterlyList.select("b").text() shouldBe aug5th2024

              obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              obligationsList.select("b").text() shouldBe jan31st2026
            }
          }

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for both CY-1 and CY" when {

              "the current date is before the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayJustAfterTaxYearStart2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                quarterlyList.select("b").text() shouldBe "5 May 2024"

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is between the CY-1 Q4 deadline and the CY Q1 deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayFirstQuarter2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
                quarterlyList.select("b").text() shouldBe aug5th2024

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 final declaration deadline" in {

                val validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.Quarterly,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
                quarterlyList.select("b").text() shouldBe "5 February 2025"

                obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                obligationsList.select("b").text() shouldBe jan31st2026
              }
            }
          }

          "the business will start in the next tax year" when {

            "it is reporting quarterly" in {

              val validFutureTaxYearQuarterlyCall: Html = view(
                viewModel = viewModelWithFutureBusinessStartReportingQuarterly,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayJustBeforeTaxYearEnd2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validFutureTaxYearQuarterlyCall.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
              val obligationsList = document.getElementById(SelectorHelper.obligationsList)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

              quarterlyList.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
              quarterlyList.select("b").text() shouldBe aug5th2024

              obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
              obligationsList.select("b").text() shouldBe jan31st2026
            }
          }
        }

        s"display the View overdue and upcoming updates link when there are no overdue obligations - $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallOneOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFirstQuarterDeadline2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallOneOverdue.body)
          val viewUpcomingUpdatesLink = document.getElementById("view-upcoming-updates")

          viewUpcomingUpdatesLink.text() shouldBe "View your overdue and upcoming updates"
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the no change reporting frequency text when Quarterly- $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)

          Option(document.getElementById("change-frequency")) shouldBe None
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" in {

          val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
            view(
              viewModel = viewModelWithCurrentYearQuarterly,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val subHeading: Element = layoutContent.getElementsByTag("h2").last()
          val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
          val compatibleSoftwareLink = compatibleSoftwareParagraph.select("a")

          subHeading.text shouldBe submitUpdatesInSoftware

          compatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
          compatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
          compatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl
        }
      }
    }

    "Annual" should {
      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Annually for both CY-1 and CY" in {

              val validAnnualThenAnnualCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = true,
                  incomeSourceType = SelfEmployment,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          "the business started before CY-1 and has a historic start date" in {

            val validHistoricAnnualThenAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenAnnualYear,
              isAgent = true,
              incomeSourceType = SelfEmployment,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

            val document: Document = Jsoup.parse(validHistoricAnnualThenAnnualCallNoOverdue.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }

          "the business is going to start at a future date in CY" in {

            val validFutureAnnualCallSameTaxYear: Html =
              view(
                viewModel = viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Annual
                ,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

            val document: Document = Jsoup.parse(validFutureAnnualCallSameTaxYear.body)

            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting annually for both CY-1 and CY" when {

            "there is one overdue obligation" in {

              val validAnnualThenAnnualCallOneOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallOneOverdue.body)

              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting annually" in {

              val validCurrentTaxYearAnnualCallNoOverdue: Html =
                view(
                  viewModel = viewModelWithCurrentYearAnnual,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              finalDeclarationContent.select("b").text() shouldBe jan31st2026
            }
          }

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting annually for both CY-1 and CY" when {

              "the current date is before the deadline for CY-1 final declaration" in {

                val validAnnualThenAnnualCallNoOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

                val document: Document = Jsoup.parse(validAnnualThenAnnualCallNoOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
                finalDeclarationContent.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
                finalDeclarationContent.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the deadline for CY-1 final declaration" in {

                val validAnnualThenAnnualCallOneOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)
                val document: Document = Jsoup.parse(validAnnualThenAnnualCallOneOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
                finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
                finalDeclarationContent.select("b").text() shouldBe jan31st2026
              }
            }
          }

          "the business will start in the next tax year" when {

            "it is reporting annually" in {

              val validFutureTaxYearAnnualCall: Html =
                view(
                  viewModel = viewModelWithFutureBusinessStartReportingAnnually,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayJustBeforeTaxYearEnd2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)

              val document: Document = Jsoup.parse(validFutureTaxYearAnnualCall.body)

              val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
              val finalDeclarationContent = document.getElementById(SelectorHelper.finalDeclaration)

              yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              finalDeclarationContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
              finalDeclarationContent.select("b").text() shouldBe jan31st2026
            }
          }
        }

        s"display the View upcoming updates link when there are no overdue obligations - $incomeSourceType" in {

          val validCallWithData: Html =
            view(
              viewModel = viewModelWithAllData,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

          val document: Document = Jsoup.parse(validCallWithData.body)
          val viewUpcomingUpdatesLink = document.getElementById("view-upcoming-updates")

          viewUpcomingUpdatesLink.text() shouldBe "View your upcoming updates"
        }

        s"render the view all your business link - $incomeSourceType" in {

          val validCallWithData: Html =
            view(
              viewModel = viewModelWithAllData,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

          val document: Document = Jsoup.parse(validCallWithData.body)

          document.getElementById("view-all-businesses-link").text() shouldBe viewAllBusinessesText
          document.getElementById("view-all-businesses-link").select("a").attr("href") shouldBe manageBusinessesAgentUrl
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting Annually" in {

            val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

            val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "You are set to report annually for your new property. Find out more about your reporting frequency."
              case _ => reportingFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
            }
          }

          "reporting Hybrid" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")


            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }

            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(true)
          }

          "reporting methods page was skipped - defaults to Annual reporting " in {

            val validCurrentTaxYearDefaultAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.DefaultAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

            val document: Document = Jsoup.parse(validCurrentTaxYearDefaultAnnualCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "You are set to report annually for your new property. Find out more about your reporting frequency."
              case _ => reportingFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
            }

            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(true)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting Annually" in {

            val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
              viewModel = viewModelWithCurrentYearAnnual,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Annual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)

            val document: Document = Jsoup.parse(validCurrentTaxYearAnnualCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
            val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            subHeading.text shouldBe submitUpdatesInSoftware
            compatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or software compatible with Making Tax Digital for Income Tax (opens in new tab)."

          }

          "reporting Quarterly" in {

            val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithCurrentYearQuarterly,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
            val compatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val compatibleSoftwareLink = compatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            compatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            compatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            compatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl
          }

          "reporting Hybrid" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }

    "QuarterlyAnnual" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.select("div").eq(3)

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe s"$h1SoleTraderContent $headingBase"
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe s"$h1UKProperty $headingBase"
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe s"$h1ForeignProperty $headingBase"
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Quarterly for CY-1 and Annually for CY" in {

              val validOneQuarterThenAnnualCallNoOverdue: Html = view(
                viewModel = viewModelOneQuarterYearThenAnnualYear,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }

          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting quarterly for CY-1 and annually for CY" when {

            "there is one overdue quarterly obligation" in {

              val validOneQuarterThenAnnualCallOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelOneQuarterYearThenAnnualYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "there are multiple overdue quarterly obligations" in {

            val validTwoQuartersThenAnnualCallTwoQuarterlyOverdue: Html = view(
              viewModel = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

            val document: Document = Jsoup.parse(validTwoQuartersThenAnnualCallTwoQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "2 overdue updates"
          }

          "there is one overdue quarterly obligation and one overdue annual obligation" in {

            val validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue: Html =
              view(
                viewModel = viewModelOneQuarterYearThenAnnualYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById(SelectorHelper.annualInset)
            val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue quarterly obligations and one overdue annual obligation" in {

            val validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue: Html = view(
              viewModel = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

            val document: Document = Jsoup.parse(validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById(SelectorHelper.annualInset)
            val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "2 overdue updates"
          }
        }

        s"the business started before CY-1 and has a historic start date - $incomeSourceType" when {

          "there is one overdue obligation" in {

            val validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFirstQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 1 overdue update. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue obligations" in {

            val validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterThirdQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "4 overdue updates"
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting quarterly for CY-1 and Annually for CY" when {

              "the current date is before the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenAnnualCallBeforeQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayJustAfterTaxYearStart2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallBeforeQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                quarterlyList.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
                quarterlyList.select("b").text() shouldBe "5 May 2024"

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 Q4 deadline" in {

                val validFullQuarterlyThenAnnualCallAfterQ4Deadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayFirstQuarter2024_2025,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallAfterQ4Deadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                Option(quarterlyList) shouldBe None

                obligationsList.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
                obligationsList.select("b").text() shouldBe jan31st2025
              }

              "the current date is after the CY-1 final declaration deadline" in {

                val validFullQuarterlyThenAnnualCallAfterFinalDecDeadline: Html =
                  view(
                    viewModel = viewModelWithFullQuarterlyYearThenAnnualYear,
                    isAgent = true,
                    incomeSourceType = incomeSourceType,
                    businessName = testName,
                    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                    isBusinessHistoric = false,
                    reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                    getSoftwareUrl = appConfig.compatibleSoftwareLink,
                    getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                    getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                    getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                  )

                val document: Document = Jsoup.parse(validFullQuarterlyThenAnnualCallAfterFinalDecDeadline.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)
                val quarterlyList = document.getElementById(SelectorHelper.quarterlyList)
                val obligationsList = document.getElementById(SelectorHelper.obligationsList)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading

                Option(quarterlyList) shouldBe None

                obligationsList.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
                obligationsList.select("b").text() shouldBe jan31st2026
              }
            }
          }
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting QuarterlyAnnual" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }
            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(true)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting QuarterlyAnnual" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }

    "AnnualQuarterly" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          Option(document.getElementById("back")) shouldBe None
        }

        s"display the correct banner message - $incomeSourceType" in {

          val businessName =
            incomeSourceType match {
              case SelfEmployment => Some("Test Name")
              case UkProperty => None
              case ForeignProperty => None
            }

          val page =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(page.body)
          val layoutContent: Element = document.selectHead("#main-content")

          val banner: Element = layoutContent.getElementsByTag("h1").first()
          val subText: Elements = layoutContent.getElementsByClass("govuk-panel__body")

          incomeSourceType match {
            case SelfEmployment =>
              banner.text() shouldBe h1SoleTraderContent
              subText.text shouldBe headingBase
            case UkProperty =>
              banner.text() shouldBe h1UKProperty
              subText.text shouldBe headingBase
            case ForeignProperty =>
              banner.text() shouldBe h1ForeignProperty
              subText.text shouldBe headingBase
          }
        }

        s"display the correct heading for your-revised-deadlines - $incomeSourceType" in {

          val validSoleTreaderBusinessCall =
            view(
              viewModel = viewModel,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the previous tax year (CY-1)" when {

            "it is reporting Annually for CY-1 and Quarterly for CY" in {

              val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = true,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting annually for CY-1 and quarterly for CY" when {

            "there is one overdue quarterly obligation" in {

              val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFirstQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            "there are multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterSecondQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates for 6 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there is one overdue annual obligation and multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html =
                view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)

              val annualInsetText = document.getElementById(SelectorHelper.annualInset)
              val quarterlyInsetText = document.getElementById(SelectorHelper.quarterlyInset)

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "3 overdue updates"
            }
          }
        }

        s"display the correct revised deadlines content - $incomeSourceType" when {

          "the business started in the previous tax year (CY-1)" when {

            "it is reporting annually for CY-1 and quarterly for CY" when {

              "the current date is before the CY-1 final declaration deadline and CY Q1 deadline" in {

                val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is before the CY-1 final declaration deadline and between CY Q1 and Q2 deadlines" in {

                val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFirstQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is after the CY-1 final declaration deadline and between CY Q2 and Q3 deadlines" in {

                val validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }

              "the current date is after the CY-1 final declaration deadline and the CY Q3 deadline" in {

                val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
                  viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                )

                val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)

                val yourRevisedDeadlinesH2 = document.getElementById(SelectorHelper.yourRevisedDeadlinesH2)

                yourRevisedDeadlinesH2.text() shouldBe yourRevisedDeadlinesHeading
              }
            }
          }
        }

        // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
        s"display the correct change reporting frequency text - $incomeSourceType" when {

          "reporting AnnualQuarterly" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html =
              view(
                viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
                isAgent = true,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.AnnualQuarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
                getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
              )

            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val reportingFrequency = document.getElementById("change-frequency")

            incomeSourceType match {
              case UkProperty | ForeignProperty => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your properties."
              case _ => reportingFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
            }
            reportingFrequency.select("a").attr("href") shouldBe reportingFrequencyPageUrl(true)
          }
        }

        s"display the correct submit tax return / updates subheading and text - $incomeSourceType" when {

          "reporting AnnualQuarterly" in {

            val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
              viewModel = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = true,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.AnnualQuarterly,
              getSoftwareUrl = appConfig.compatibleSoftwareLink,
              getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(true).url,
              getNextUpdatesUrl = controllers.routes.NextUpdatesController.showAgent().url,
              getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
            )
            val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallNoOverdue.body)
            val layoutContent: Element = document.selectHead("#main-content")

            val subHeading: Element = layoutContent.getElementsByTag("h2").last()

            val annualCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.annualCompatibleSoftwareParagraph)

            val quarterlyCompatibleSoftwareParagraph = document.getElementById(SelectorHelper.quarterlyCompatibleSoftwareParagraph)
            val quarterlyCompatibleSoftwareLink = quarterlyCompatibleSoftwareParagraph.select("a")

            subHeading.text shouldBe submitUpdatesInSoftware

            quarterlyCompatibleSoftwareParagraph.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
            quarterlyCompatibleSoftwareLink.attr("href") shouldBe submitSoftwareUrl

            annualCompatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
          }
        }
      }
    }
  }
}
