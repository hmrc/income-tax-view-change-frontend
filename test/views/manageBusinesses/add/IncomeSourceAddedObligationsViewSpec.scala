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

  val validUKPropertyBusinessCall: Html = view(
    sources = viewModel, isAgent = false, incomeSourceType = UkProperty, businessName = None, currentDate = day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validUKPropertyBusinessAgentCall: Html = view(
    viewModel, isAgent = true, UkProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validForeignPropertyBusinessCall: Html = view(
    viewModel, isAgent = false, ForeignProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )
  val validForeignPropertyBusinessAgentCall: Html = view(
    viewModel, isAgent = true, ForeignProperty, None, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validSoleTreaderBusinessCall: Html = view(
    viewModel, isAgent = false, SelfEmployment, testName, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validSoleTreaderBusinessAgentCall: Html =
    view(
      sources = viewModel, isAgent = true, incomeSourceType = SelfEmployment, businessName = testName, currentDate = day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
    )

  val validCallWithData: Html = view(
    viewModelWithAllData, isAgent = false, SelfEmployment, testName, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validAgentCallWithData: Html = view(
    viewModelWithAllData, isAgent = true, SelfEmployment, testName, day, isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validCurrentTaxYearAnnualCallNoOverdue: Html = view(
    viewModelWithCurrentYearAnnual, isAgent = false, SelfEmployment, testName, dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validCurrentTaxYearQuarterlyCallNoOverdue: Html =
    view(
      viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, testName, dayFirstQuarter2024_2025,
      isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly
    )

  val validCurrentTaxYearQuarterlyCallOneOverdue: Html = view(
    viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, testName, dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validCurrentTaxYearQuarterlyCallMultipleOverdue: Html = view(
    viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, testName, dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validAnnualThenAnnualCallNoOverdue: Html = view(
    viewModelWithAnnualYearThenAnnualYear, isAgent = false, SelfEmployment, testName, dayFirstQuarter2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Annual
  )

  val validAnnualThenAnnualCallOneOverdue: Html = view(
    sources = viewModelWithAnnualYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Annual
  )

  val validAnnualThenFullQuarterlyCallNoOverdue: Html = view(
    sources = viewModelWithAnnualYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, testName, dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue: Html = view(
    viewModelWithAnnualYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, testName, dayAfterSecondQuarterDeadline2024_2025,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue: Html = view(
    sources = viewModelWithAnnualYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
    sources = viewModelWithAnnualYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validOneQuarterThenAnnualCallNoOverdue: Html = view(
    sources = viewModelOneQuarterYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validOneQuarterThenAnnualCallOneQuarterlyOverdue: Html = view(
    sources = viewModelOneQuarterYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validTwoQuartersThenAnnualCallTwoQuarterlyOverdue: Html = view(
    sources = viewModelTwoQuartersYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue: Html =
    view(
      sources = viewModelOneQuarterYearThenAnnualYear,
      isAgent = false,
      incomeSourceType = SelfEmployment,
      businessName = testName,
      currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
      isBusinessHistoric = false,
      reportingMethod = ChosenReportingMethod.QuarterlyAnnual
    )

  val validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue: Html = view(
    sources = viewModelTwoQuartersYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validOneQuarterThenQuarterlyCallNoOverdue: Html = view(
    sources = viewModelOneQuarterYearThenQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayBeforeLastQuarterlyDeadline2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validOneQuarterThenQuarterlyCallOneQuarterlyOverdue: Html = view(
    sources = viewModelOneQuarterYearThenQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue: Html = view(
    sources = viewModelTwoQuarterYearThenQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue: Html = view(
    sources = viewModelTwoQuarterYearThenQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue: Html = view(
    sources = viewModelTwoQuarterYearThenQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validHistoricAnnualThenAnnualCallNoOverdue: Html = view(
    sources = viewModelWithAnnualYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = true,
    reportingMethod = ChosenReportingMethod.Annual
  )

  val validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
    sources = viewModelWithAnnualYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFirstQuarterDeadline2024_2025,
    isBusinessHistoric = true,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
    sources = viewModelWithAnnualYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterThirdQuarterDeadline2024_2025,
    isBusinessHistoric = true,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validFutureAnnualCallSameTaxYear: Html = view(
    sources = viewModelWithFutureBusinessStartReportingAnnuallySameYaxYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Annual
  )

  val validFullQuarterlyThenAnnualCallBeforeQ4Deadline: Html = view(
    sources = viewModelWithFullQuarterlyYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayJustAfterTaxYearStart2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validFullQuarterlyThenAnnualCallAfterQ4Deadline: Html = view(
    sources = viewModelWithFullQuarterlyYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validFullQuarterlyThenAnnualCallAfterFinalDecDeadline: Html = view(
    sources = viewModelWithFullQuarterlyYearThenAnnualYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.QuarterlyAnnual
  )

  val validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline: Html = view(
    sources = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayJustAfterTaxYearStart2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline: Html = view(
    sources = viewModelWithFullQuarterlyYearThenFullQuarterlyYear,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline: Html = view(
    viewModelWithFullQuarterlyYearThenFullQuarterlyYear, isAgent = false, SelfEmployment, testName, dayAfterFinalDeclarationDeadline2023_2024,
    isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validFutureTaxYearAnnualCall: Html = view(
    sources = viewModelWithFutureBusinessStartReportingAnnually,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayJustBeforeTaxYearEnd2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Annual
  )

  val validFutureTaxYearQuarterlyCall: Html = view(
    sources = viewModelWithFutureBusinessStartReportingQuarterly,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayJustBeforeTaxYearEnd2023_2024,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.Quarterly
  )

  val validCurrentTaxYearDefaultAnnualCallNoOverdue: Html = view(
    sources = viewModelWithCurrentYearAnnual,
    isAgent = false,
    incomeSourceType = SelfEmployment,
    businessName = testName,
    currentDate = dayFirstQuarter2024_2025,
    isBusinessHistoric = false,
    reportingMethod = ChosenReportingMethod.DefaultAnnual
  )

  object SelectorHelper {
    val yourRevisedDeadlinesH2 = "your-revised-deadlines"
    val warningInset = "warning-inset"
  }

  "Income Source Added Obligations - Individual" when {

    "QuarterlyQuarterly" should {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"not display a back button - $incomeSourceType" in {

          val page =
            view(
              sources = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly
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
              sources = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly
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
              sources = viewModel,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = None,
              currentDate = day,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.Quarterly
            )

          val document: Document = Jsoup.parse(validSoleTreaderBusinessCall.body)

          document.getElementById(SelectorHelper.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
        }

        s"not display inset warning text because there are no overdue obligations - $incomeSourceType" when {

          s"the business started in the current tax year" when {

            "it is reporting quarterly" in {

              val validCurrentTaxYearQuarterlyCallNoOverdue =
                view(
                  sources = viewModelWithCurrentYearQuarterly,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly
                )

              val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallNoOverdue.body)
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          s"the business started in the previous tax year (CY-1)" when {

            "It is reporting annually for both CY-1 and CY" in {

              val validAnnualThenAnnualCallNoOverdue: Html =
                view(
                  sources = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = SelfEmployment,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual
                )

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallNoOverdue.body)

              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }

            "It is reporting annually for CY-1 and quarterly for CY" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }

            "It is reporting quarterly for CY-1 and annually for CY" in new Setup(validOneQuarterThenAnnualCallNoOverdue) {
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }

            "It is reporting quarterly for both CY-1 and CY" in new Setup(validOneQuarterThenQuarterlyCallNoOverdue) {
              Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
            }
          }

          "the business started before CY-1 and has a historic start date" in new Setup(validHistoricAnnualThenAnnualCallNoOverdue) {
            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }

          "the business is going to start at a future date in CY" in new Setup(validFutureAnnualCallSameTaxYear) {
            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }

          "the business is going to start at a future date in CY+1" in new Setup(validFutureTaxYearQuarterlyCall) {
            Option(document.getElementById(SelectorHelper.warningInset)) shouldBe None
          }
        }

        s"display the correct inset warning text - $incomeSourceType" when {

          "the business started in the current tax year" when {

            "it is reporting quarterly and there is one overdue obligation" in {

              val validCurrentTaxYearQuarterlyCallOneOverdue: Html = view(
                sources = viewModelWithCurrentYearQuarterly,
                isAgent = false,
                incomeSourceType = SelfEmployment,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly
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
                viewModelWithCurrentYearQuarterly, isAgent = false, SelfEmployment, testName, dayAfterThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false, reportingMethod = ChosenReportingMethod.Quarterly
              )

            val document: Document = Jsoup.parse(validCurrentTaxYearQuarterlyCallMultipleOverdue.body)

            val insetText = document.getElementById(SelectorHelper.warningInset)
            insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "3 overdue updates"
          }
        }

        s"the business started in the previous tax year (CY-1) - $incomeSourceType" when {

          "it is reporting annually for both CY-1 and CY" when {

            "there is one overdue obligation" in {

              val validAnnualThenAnnualCallOneOverdue: Html =
                view(
                  sources = viewModelWithAnnualYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual
                )

              val document: Document = Jsoup.parse(validAnnualThenAnnualCallOneOverdue.body)

              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "it is reporting annually for CY-1 and quarterly for CY" when {

            "there is one overdue quarterly obligation" in {

              val validAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html =
                view(
                  sources = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFirstQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            "there are multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue: Html =
                view(
                  sources = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterSecondQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates for 6 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there is one overdue annual obligation and multiple overdue quarterly obligations" in {

              val validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html =
                view(
                  sources = viewModelWithAnnualYearThenFullQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterThirdQuarterDeadline2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual
                )

              val document: Document = Jsoup.parse(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)

              val annualInsetText = document.getElementById("annual-warning-inset")
              val quarterlyInsetText = document.getElementById("quarterly-warning-inset")

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "3 overdue updates"
            }
          }

          "it is reporting quarterly for CY-1 and annually for CY" when {

            "there is one overdue quarterly obligation" in {

              val validOneQuarterThenAnnualCallOneQuarterlyOverdue: Html =
                view(
                  sources = viewModelOneQuarterYearThenAnnualYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayFirstQuarter2024_2025,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.QuarterlyAnnual
                )

              val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }
          }

          "there are multiple overdue quarterly obligations" in {

            val validTwoQuartersThenAnnualCallTwoQuarterlyOverdue: Html = view(
              sources = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayFirstQuarter2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual
            )

            val document: Document = Jsoup.parse(validTwoQuartersThenAnnualCallTwoQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            insetText.select("b").text() shouldBe "2 overdue updates"
          }

          "there is one overdue quarterly obligation and one overdue annual obligation" in {

            val validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue: Html =
              view(
                sources = viewModelOneQuarterYearThenAnnualYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.QuarterlyAnnual
              )

            val document: Document = Jsoup.parse(validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById("annual-warning-inset")
            val quarterlyInsetText = document.getElementById("quarterly-warning-inset")

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue quarterly obligations and one overdue annual obligation" in {

            val validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue: Html = view(
              sources = viewModelTwoQuartersYearThenAnnualYear,
              isAgent = false,
              incomeSourceType = incomeSourceType,
              businessName = testName,
              currentDate = dayAfterFinalDeclarationDeadline2023_2024AndThirdQuarterDeadline2024_2025,
              isBusinessHistoric = false,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual
            )

            val document: Document = Jsoup.parse(validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue.body)

            val annualInsetText = document.getElementById("annual-warning-inset")
            val quarterlyInsetText = document.getElementById("quarterly-warning-inset")

            annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
            annualInsetText.select("b").text() shouldBe "1 overdue update"

            quarterlyInsetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
            quarterlyInsetText.select("b").text() shouldBe "2 overdue updates"
          }

          "it is reporting quarterly for both CY-1 and CY" when {

            "there is one overdue quarterly obligation from CY-1" in {

              val validOneQuarterThenQuarterlyCallOneQuarterlyOverdue: Html = view(
                sources = viewModelOneQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly
              )

              val document: Document = Jsoup.parse(validOneQuarterThenQuarterlyCallOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "1 overdue update"
            }

            //TODO Here left off

            "there are multiple overdue quarterly obligations from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue: Html = view(
                sources = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayFirstQuarter2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "2 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue: Html = view(
                sources = viewModelTwoQuarterYearThenQuarterlyYear,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = testName,
                currentDate = dayAfterFirstQuarterDeadline2024_2025,
                isBusinessHistoric = false,
                reportingMethod = ChosenReportingMethod.Quarterly
              )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue.body)
              val insetText = document.getElementById(SelectorHelper.warningInset)

              insetText.text() shouldBe "You have 3 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              insetText.select("b").text() shouldBe "3 overdue updates"
            }

            "there are multiple overdue quarterly obligations from CY-1 and CY, and one overdue annual obligation from CY-1" in {

              val validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue: Html =
                view(
                  sources = viewModelTwoQuarterYearThenQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly
                )

              val document: Document = Jsoup.parse(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue.body)

              val annualInsetText = document.getElementById("annual-warning-inset")
              val quarterlyInsetText = document.getElementById("quarterly-warning-inset")

              annualInsetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              annualInsetText.select("b").text() shouldBe "1 overdue update"

              quarterlyInsetText.text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
              quarterlyInsetText.select("b").text() shouldBe "4 overdue updates"
            }
          }
        }

        s"the business started before CY-1 and has a historic start date - $incomeSourceType" when {

          "there is one overdue obligation" in {

            val validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue: Html = view(
              sources = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = SelfEmployment,
              businessName = testName,
              currentDate = dayAfterFirstQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 1 overdue update. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "1 overdue update"
          }

          "there are multiple overdue obligations" in {

            val validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue: Html = view(
              sources = viewModelWithAnnualYearThenFullQuarterlyYear,
              isAgent = false,
              incomeSourceType = SelfEmployment,
              businessName = testName,
              currentDate = dayAfterThirdQuarterDeadline2024_2025,
              isBusinessHistoric = true,
              reportingMethod = ChosenReportingMethod.QuarterlyAnnual
            )

            val document: Document = Jsoup.parse(validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue.body)
            val insetText = document.getElementById(SelectorHelper.warningInset)

            insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
            insetText.select("b").text() shouldBe "4 overdue updates"
          }
        }
      }
    }
  }
}

//    "display the correct revised deadlines text" when {
//      "the business started in the current tax year" when {
//        "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//          Option(document.getElementById("final-declaration")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//            case _ => fail("No upcoming annual obligation text was found.")
//          }
//        }
//
//        "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//          Option(document.getElementById("quarterly-list")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//              upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//            case _ => fail("No upcoming quarterly obligation text was found.")
//          }
//
//          Option(document.getElementById("obligations-list")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//            case _ => fail("No upcoming annual obligation text was found.")
//          }
//        }
//      }
//
//      "the business started in the previous tax year (CY-1)" when {
//        "It is reporting annually for both CY-1 and CY" when {
//          "the current date is before the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallNoOverdue) {
//            Option(document.getElementById("final-declaration")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is after the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallOneOverdue) {
//            Option(document.getElementById("final-declaration")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//        }
//
//        "It is reporting annually for CY-1 and quarterly for CY" when {
//          "the current date is before the CY-1 final declaration deadline and CY Q1 deadline" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is before the CY-1 final declaration deadline and between CY Q1 and Q2 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 November 2024 for the quarterly period 6 July 2024 to 5 October 2024"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 November 2024"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is after the CY-1 final declaration deadline and between CY Q2 and Q3 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue) {
//            val upcomingDeadlineMessage = document.getElementById("quarterly-list")
//            upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
//            upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
//
//            val obligationsListContent = document.getElementById("obligations-list")
//            obligationsListContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//            obligationsListContent.select("b").text() shouldBe "31 January 2026"
//
//          }
//
//          "the current date is after the CY-1 final declaration deadline and the CY Q3 deadline" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 May 2025 for the quarterly period 6 January 2025 to 5 April 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2025"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//        }
//
//        "It is reporting quarterly for CY-1 and Annually for CY" when {
//          "the current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallBeforeQ4Deadline) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is after the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterQ4Deadline) {
//            Option(document.getElementById("quarterly-list")) shouldBe None
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterFinalDecDeadline) {
//            Option(document.getElementById("quarterly-list")) shouldBe None
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//        }
//
//        "It is reporting quarterly for both CY-1 and CY" when {
//          "the current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is between the CY-1 Q4 deadline and the CY Q1 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//
//          "the current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline) {
//            Option(document.getElementById("quarterly-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
//              case _ => fail("No upcoming quarterly obligation text was found.")
//            }
//
//            Option(document.getElementById("obligations-list")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//              case _ => fail("No upcoming annual obligation text was found.")
//            }
//          }
//        }
//      }
//
//      "the business will start in the next tax year" when {
//        "It is reporting annually" in new Setup(validFutureTaxYearAnnualCall) {
//          Option(document.getElementById("final-declaration")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//            case _ => fail("No upcoming annual obligation text was found.")
//          }
//        }
//
//        "It is reporting quarterly" in new Setup(validFutureTaxYearQuarterlyCall) {
//          Option(document.getElementById("quarterly-list")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//              upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//            case _ => fail("No upcoming quarterly obligation text was found.")
//          }
//
//          Option(document.getElementById("obligations-list")) match {
//            case Some(upcomingDeadlineMessage) =>
//              upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//              upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//            case _ => fail("No upcoming annual obligation text was found.")
//          }
//        }
//      }
//    }
//
//    "display the view upcoming updates link when there are no overdue obligations" in new Setup(validCallWithData) {
//      Option(document.getElementById("view-upcoming-updates")) match {
//        case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your upcoming updates"
//        case None => fail("No view or change your reporting frequency link was found")
//      }
//    }
//
//    "display the view overdue and upcoming updates link when there are no overdue obligations" in new Setup(validCurrentTaxYearQuarterlyCallOneOverdue) {
//      Option(document.getElementById("view-upcoming-updates")) match {
//        case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your overdue and upcoming updates"
//        case None => fail("No view or change your reporting frequency link was found")
//      }
//    }
//
//    "render the view all your business link" in new Setup(validCallWithData) {
//      document.getElementById("view-all-businesses-link").text() shouldBe viewAllBusinessesText
//      document.getElementById("view-all-businesses-link").select("a").attr("href") shouldBe manageBusinessesUrl
//    }
//
//    "display the correct change reporting frequency text" when {
//      // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
//      "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//        Option(document.getElementById("change-frequency")) match {
//          case Some(changeFrequency) =>
//            changeFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
//            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//          case None => fail("No view or change your reporting frequency link was found")
//        }
//      }
//
//      "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//        document.getElementById("change-frequency").text() shouldBe "You can decide at any time to opt out of quarterly reporting and report annually for all your businesses on your reporting frequency page."
//      }
//
//      "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//        Option(document.getElementById("change-frequency")) match {
//          case Some(changeFrequency) =>
//            changeFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
//            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//          case None => fail("No view or change your reporting frequency link was found")
//        }
//      }
//
//      "It defaults to annual reporting because the reporting methods page was skipped" in new Setup(validCurrentTaxYearDefaultAnnualCallNoOverdue) {
//        Option(document.getElementById("change-frequency")) match {
//          case Some(changeFrequency) =>
//            changeFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
//            changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//          case None => fail("No view or change your reporting frequency link was found")
//        }
//      }
//    }
//
//    "display the correct submit tax return / updates subheading and text" when {
//
//      "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//
//        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//
//        subHeading.text shouldBe submitUpdatesInSoftware
//
//        val compatibleSoftwareParagraph = document.getElementById("submit-via-compatible-software-annual")
//
//        compatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
//      }
//
//      "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//        subHeading.text shouldBe submitUpdatesInSoftware
//
//        Option(document.getElementById("submit-via-compatible-software")) match {
//          case Some(upcomingDeadlineMessage) =>
//            upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//            upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//            upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
//          case _ => fail("No submit text was found.")
//        }
//      }
//
//      "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//        val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//        subHeading.text shouldBe submitUpdatesInSoftware
//
//        Option(document.getElementById("submit-via-compatible-software")) match {
//          case Some(upcomingDeadlineMessage) =>
//            upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//            upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//            upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
//          case _ => fail("No submit text was found.")
//        }
//
//        Option(document.getElementById("submit-via-compatible-software-annual")) match {
//          case Some(upcomingDeadlineMessage) =>
//            upcomingDeadlineMessage.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
//          case _ => fail("No submit text was found.")
//        }
//      }
//    }

//      "AnnualAnnual" should {
//
//        "display the correct banner message" when {
//
//          "Business type is UK Property Business" in new Setup(validUKPropertyBusinessCall) {
//            val banner: Element = layoutContent.getElementsByTag("h1").first()
//            val subText: Elements = layoutContent.select("div").eq(3)
//
//            banner.text() shouldBe h1UKProperty
//            subText.text shouldBe h1UKProperty + " " + headingBase
//          }
//
//          "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessCall) {
//            val banner: Element = layoutContent.getElementsByTag("h1").first()
//            val subText: Elements = layoutContent.select("div").eq(3)
//
//            banner.text() shouldBe h1ForeignProperty
//            subText.text shouldBe h1ForeignProperty + " " + headingBase
//          }
//
//          "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessCall) {
//            val banner: Element = layoutContent.getElementsByTag("h1").first()
//            val subText: Elements = layoutContent.select("div").eq(3)
//
//            banner.text() shouldBe h1SelfEmployment
//            subText.text shouldBe h1SelfEmployment + " " + headingBase
//          }
//        }
//
//        "not display a back button" in new Setup(validCallWithData) {
//          Option(document.getElementById("back")) shouldBe None
//        }
//
//        "display the correct heading for your-revised-deadlines" in new Setup(validCallWithData) {
//          document.getElementById(Selectors.yourRevisedDeadlinesH2).text.contains(yourRevisedDeadlinesHeading)
//        }
//
//        "not display inset warning text because there are no overdue obligations" when {
//
//          "the business started in the current tax year" when {
//
//            "it is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//
//            "it is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//          }
//
//          "the business started in the previous tax year (CY-1)" when {
//
//            "It is reporting annually for both CY-1 and CY" in new Setup(validAnnualThenAnnualCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//
//            "It is reporting annually for CY-1 and quarterly for CY" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//
//            "It is reporting quarterly for CY-1 and annually for CY" in new Setup(validOneQuarterThenAnnualCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//
//            "It is reporting quarterly for both CY-1 and CY" in new Setup(validOneQuarterThenQuarterlyCallNoOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) shouldBe None
//            }
//          }
//
//          "the business started before CY-1 and has a historic start date" in new Setup(validHistoricAnnualThenAnnualCallNoOverdue) {
//            Option(document.getElementById(Selectors.warningInset)) shouldBe None
//          }
//
//          "the business is going to start at a future date in CY" in new Setup(validFutureAnnualCallSameTaxYear) {
//            Option(document.getElementById(Selectors.warningInset)) shouldBe None
//          }
//
//          "the business is going to start at a future date in CY+1" in new Setup(validFutureTaxYearQuarterlyCall) {
//            Option(document.getElementById(Selectors.warningInset)) shouldBe None
//          }
//        }
//
//        "display the correct inset warning text" when {
//
//          "the business started in the current tax year" when {
//            "It is reporting quarterly and there is one overdue obligation" in new Setup(validCurrentTaxYearQuarterlyCallOneOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) match {
//                case Some(insetText) =>
//                  insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
//                  insetText.select("b").text() shouldBe "1 overdue update"
//                case None => fail("No inset text was found")
//              }
//            }
//
//            "It is reporting quarterly and there are multiple overdue obligations" in new Setup(validCurrentTaxYearQuarterlyCallMultipleOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) match {
//                case Some(insetText) =>
//                  insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
//                  insetText.select("b").text() shouldBe "3 overdue updates"
//                case None => fail("No inset text was found")
//              }
//            }
//          }
//
//          "the business started in the previous tax year (CY-1)" when {
//            "It is reporting annually for both CY-1 and CY" when {
//              "there is one overdue obligation" in new Setup(validAnnualThenAnnualCallOneOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No inset text was found")
//                }
//              }
//            }
//
//            "It is reporting annually for CY-1 and quarterly for CY" when {
//              "there is one overdue quarterly obligation" in new Setup(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update for 3 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations" in new Setup(validAnnualThenFullQuarterlyCallTwoQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 2 overdue updates for 6 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "2 overdue updates"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there is one overdue annual obligation and multiple overdue quarterly obligations" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
//                Option(document.getElementById("annual-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No annual inset text was found")
//                }
//
//                Option(document.getElementById("quarterly-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "3 overdue updates"
//                  case None => fail("No quarterly inset text was found")
//                }
//              }
//            }
//
//            "It is reporting quarterly for CY-1 and annually for CY" when {
//              "there is one overdue quarterly obligation" in new Setup(validOneQuarterThenAnnualCallOneQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations" in new Setup(validTwoQuartersThenAnnualCallTwoQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "2 overdue updates"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there is one overdue quarterly obligation and one overdue annual obligation" in new Setup(validOneQuarterThenAnnualCallOneQuarterlyOneAnnualOverdue) {
//                Option(document.getElementById("annual-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No annual inset text was found")
//                }
//
//                Option(document.getElementById("quarterly-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No quarterly inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations and one overdue annual obligation" in new Setup(validTwoQuarterThenAnnualCallTwoQuarterlyOneAnnualOverdue) {
//                Option(document.getElementById("annual-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No annual inset text was found")
//                }
//
//                Option(document.getElementById("quarterly-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "2 overdue updates"
//                  case None => fail("No quarterly inset text was found")
//                }
//              }
//            }
//
//            "It is reporting quarterly for both CY-1 and CY" when {
//              "there is one overdue quarterly obligation from CY-1" in new Setup(validOneQuarterThenQuarterlyCallOneQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations from CY-1" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 2 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "2 overdue updates"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations from CY-1 and CY" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneQuarterlyOverdue) {
//                Option(document.getElementById(Selectors.warningInset)) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 3 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "3 overdue updates"
//                  case None => fail("No inset text was found")
//                }
//              }
//
//              "there are multiple overdue quarterly obligations from CY-1 and CY, and one overdue annual obligation from CY-1" in new Setup(validTwoQuartersThenQuarterlyCallTwoQuarterlyOneAnnualOneQuarterlyOverdue) {
//                Option(document.getElementById("annual-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
//                    insetText.select("b").text() shouldBe "1 overdue update"
//                  case None => fail("No annual inset text was found")
//                }
//
//                Option(document.getElementById("quarterly-warning-inset")) match {
//                  case Some(insetText) =>
//                    insetText.text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."
//                    insetText.select("b").text() shouldBe "4 overdue updates"
//                  case None => fail("No quarterly inset text was found")
//                }
//              }
//            }
//          }
//
//          "the business started before CY-1 and has a historic start date" when {
//            "there is one overdue obligation" in new Setup(validHistoricAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) match {
//                case Some(insetText) =>
//                  insetText.text() shouldBe "You have 1 overdue update. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
//                  insetText.select("b").text() shouldBe "1 overdue update"
//                case None => fail("No inset text was found")
//              }
//            }
//
//            "there are multiple overdue obligations" in new Setup(validHistoricAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
//              Option(document.getElementById(Selectors.warningInset)) match {
//                case Some(insetText) =>
//                  insetText.text() shouldBe "You have 4 overdue updates. You must make sure that you have sent all the required income and expenses for tax years earlier than 2023 to 2024."
//                  insetText.select("b").text() shouldBe "4 overdue updates"
//                case None => fail("No inset text was found")
//              }
//            }
//          }
//        }
//
//        "display the correct revised deadlines text" when {
//          "the business started in the current tax year" when {
//            "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//              Option(document.getElementById("final-declaration")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//                  upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                case _ => fail("No upcoming annual obligation text was found.")
//              }
//            }
//
//            "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//              Option(document.getElementById("quarterly-list")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                  upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//                case _ => fail("No upcoming quarterly obligation text was found.")
//              }
//
//              Option(document.getElementById("obligations-list")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                  upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                case _ => fail("No upcoming annual obligation text was found.")
//              }
//            }
//          }
//
//          "the business started in the previous tax year (CY-1)" when {
//            "It is reporting annually for both CY-1 and CY" when {
//              "the current date is before the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallNoOverdue) {
//                Option(document.getElementById("final-declaration")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is after the deadline for CY-1 final declaration" in new Setup(validAnnualThenAnnualCallOneOverdue) {
//                Option(document.getElementById("final-declaration")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//            }
//
//            "It is reporting annually for CY-1 and quarterly for CY" when {
//              "the current date is before the CY-1 final declaration deadline and CY Q1 deadline" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is before the CY-1 final declaration deadline and between CY Q1 and Q2 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneQuarterlyOverdue) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 November 2024 for the quarterly period 6 July 2024 to 5 October 2024"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 November 2024"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is after the CY-1 final declaration deadline and between CY Q2 and Q3 deadlines" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualTwoQuarterlyOverdue) {
//                val upcomingDeadlineMessage = document.getElementById("quarterly-list")
//                upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
//                upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
//
//                val obligationsListContent = document.getElementById("obligations-list")
//                obligationsListContent.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                obligationsListContent.select("b").text() shouldBe "31 January 2026"
//
//              }
//
//              "the current date is after the CY-1 final declaration deadline and the CY Q3 deadline" in new Setup(validAnnualThenFullQuarterlyCallOneAnnualThreeQuarterlyOverdue) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 May 2025 for the quarterly period 6 January 2025 to 5 April 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2025"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//            }
//
//            "It is reporting quarterly for CY-1 and Annually for CY" when {
//              "the current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallBeforeQ4Deadline) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is after the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterQ4Deadline) {
//                Option(document.getElementById("quarterly-list")) shouldBe None
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenAnnualCallAfterFinalDecDeadline) {
//                Option(document.getElementById("quarterly-list")) shouldBe None
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//            }
//
//            "It is reporting quarterly for both CY-1 and CY" when {
//              "the current date is before the CY-1 Q4 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallBeforeFirstQ4Deadline) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by 5 May 2024 for the quarterly period 6 January 2024 to 5 April 2024"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 May 2024"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is between the CY-1 Q4 deadline and the CY Q1 deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstQ4Deadline) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2025"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//
//              "the current date is after the CY-1 final declaration deadline" in new Setup(validFullQuarterlyThenFullyQuarterlyCallAfterFirstFinalDecDeadline) {
//                Option(document.getElementById("quarterly-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 February 2025 for the quarterly period 6 October 2024 to 5 January 2025"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "5 February 2025"
//                  case _ => fail("No upcoming quarterly obligation text was found.")
//                }
//
//                Option(document.getElementById("obligations-list")) match {
//                  case Some(upcomingDeadlineMessage) =>
//                    upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                    upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                  case _ => fail("No upcoming annual obligation text was found.")
//                }
//              }
//            }
//          }
//
//          "the business will start in the next tax year" when {
//            "It is reporting annually" in new Setup(validFutureTaxYearAnnualCall) {
//              Option(document.getElementById("final-declaration")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026."
//                  upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                case _ => fail("No upcoming annual obligation text was found.")
//              }
//            }
//
//            "It is reporting quarterly" in new Setup(validFutureTaxYearQuarterlyCall) {
//              Option(document.getElementById("quarterly-list")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by 5 August 2024 for the quarterly period 6 April 2024 to 5 July 2024"
//                  upcomingDeadlineMessage.select("b").text() shouldBe "5 August 2024"
//                case _ => fail("No upcoming quarterly obligation text was found.")
//              }
//
//              Option(document.getElementById("obligations-list")) match {
//                case Some(upcomingDeadlineMessage) =>
//                  upcomingDeadlineMessage.text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by 31 January 2026"
//                  upcomingDeadlineMessage.select("b").text() shouldBe "31 January 2026"
//                case _ => fail("No upcoming annual obligation text was found.")
//              }
//            }
//          }
//        }
//
//        "display the view upcoming updates link when there are no overdue obligations" in new Setup(validCallWithData) {
//          Option(document.getElementById("view-upcoming-updates")) match {
//            case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your upcoming updates"
//            case None => fail("No view or change your reporting frequency link was found")
//          }
//        }
//
//        "display the view overdue and upcoming updates link when there are no overdue obligations" in new Setup(validCurrentTaxYearQuarterlyCallOneOverdue) {
//          Option(document.getElementById("view-upcoming-updates")) match {
//            case Some(upcomingUpdatesLink) => upcomingUpdatesLink.text() shouldBe "View your overdue and upcoming updates"
//            case None => fail("No view or change your reporting frequency link was found")
//          }
//        }
//
//        "render the view all your business link" in new Setup(validCallWithData) {
//          document.getElementById("view-all-businesses-link").text() shouldBe viewAllBusinessesText
//          document.getElementById("view-all-businesses-link").select("a").attr("href") shouldBe manageBusinessesUrl
//        }
//
//        "display the correct change reporting frequency text" when {
//          // TODO the links of these tests will need to change to the new entry point for the opt in/out journeys once the page is made
//          "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//            Option(document.getElementById("change-frequency")) match {
//              case Some(changeFrequency) =>
//                changeFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
//                changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//              case None => fail("No view or change your reporting frequency link was found")
//            }
//          }
//
//          "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//            document.getElementById("change-frequency").text() shouldBe "You can decide at any time to opt out of quarterly reporting and report annually for all your businesses on your reporting frequency page."
//          }
//
//          "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//            Option(document.getElementById("change-frequency")) match {
//              case Some(changeFrequency) =>
//                changeFrequency.text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting frequency for all your businesses."
//                changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//              case None => fail("No view or change your reporting frequency link was found")
//            }
//          }
//
//          "It defaults to annual reporting because the reporting methods page was skipped" in new Setup(validCurrentTaxYearDefaultAnnualCallNoOverdue) {
//            Option(document.getElementById("change-frequency")) match {
//              case Some(changeFrequency) =>
//                changeFrequency.text() shouldBe "You are set to report annually for your new business. Find out more about your reporting frequency."
//                changeFrequency.select("a").attr("href") shouldBe nextUpdatesUrl
//              case None => fail("No view or change your reporting frequency link was found")
//            }
//          }
//        }
//
//        "display the correct submit tax return / updates subheading and text" when {
//
//          "It is reporting annually" in new Setup(validCurrentTaxYearAnnualCallNoOverdue) {
//
//            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//
//            subHeading.text shouldBe submitUpdatesInSoftware
//
//            val compatibleSoftwareParagraph = document.getElementById("submit-via-compatible-software-annual")
//
//            compatibleSoftwareParagraph.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
//          }
//
//          "It is reporting quarterly" in new Setup(validCurrentTaxYearQuarterlyCallNoOverdue) {
//            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//            subHeading.text shouldBe submitUpdatesInSoftware
//
//            Option(document.getElementById("submit-via-compatible-software")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//                upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//                upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
//              case _ => fail("No submit text was found.")
//            }
//          }
//
//          "It has hybrid reporting" in new Setup(validAnnualThenFullQuarterlyCallNoOverdue) {
//            val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//            subHeading.text shouldBe submitUpdatesInSoftware
//
//            Option(document.getElementById("submit-via-compatible-software")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//                upcomingDeadlineMessage.select("a").text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
//                upcomingDeadlineMessage.select("a").attr("href") shouldBe submitSoftwareUrl
//              case _ => fail("No submit text was found.")
//            }
//
//            Option(document.getElementById("submit-via-compatible-software-annual")) match {
//              case Some(upcomingDeadlineMessage) =>
//                upcomingDeadlineMessage.text() shouldBe "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."
//              case _ => fail("No submit text was found.")
//            }
//          }
//        }
//      }

//    "Income Source Added Obligations - Agent" should {
//
//      "display the correct banner message and heading" when {
//
//        "Business type is UK Property Business" in new Setup(validUKPropertyBusinessAgentCall) {
//          val banner: Element = layoutContent.getElementsByTag("h1").first()
//          banner.text() shouldBe h1UKProperty
//
//
//          val subText: Option[Element] = layoutContent.select("div").eq(3)
//
//          subText match {
//            case Some(heading) => heading.text shouldBe h1UKProperty + " " + headingBase
//            case _ => fail("No 2nd h2 element found.")
//          }
//
//          val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//          subHeading.text shouldBe submitUpdatesInSoftware
//        }
//
//        "Business type is Foreign Property Business" in new Setup(validForeignPropertyBusinessAgentCall) {
//          val banner: Element = layoutContent.getElementsByTag("h1").first()
//          banner.text() shouldBe h1ForeignProperty
//
//          val subText: Option[Element] = layoutContent.select("div").eq(3)
//
//          subText match {
//            case Some(heading) => heading.text shouldBe h1ForeignProperty + " " + headingBase
//            case _ => fail("No 2nd h2 element found.")
//          }
//
//          val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//          subHeading.text shouldBe submitUpdatesInSoftware
//        }
//
//        "Business type is Sole Trader Business" in new Setup(validSoleTreaderBusinessAgentCall) {
//          val banner: Element = layoutContent.getElementsByTag("h1").first()
//          banner.text() shouldBe h1SelfEmployment
//
//          val subText: Option[Element] = layoutContent.select("div").eq(3)
//
//          subText match {
//            case Some(heading) => heading.text shouldBe h1SelfEmployment + " " + headingBase
//            case _ => fail("No 2nd h2 element found.")
//          }
//
//          val subHeading: Element = layoutContent.getElementsByTag("h2").last()
//          subHeading.text shouldBe submitUpdatesInSoftware
//        }
//      }
//
//      "not display the view overdue and upcoming updates link when the user does not have them" in new Setup(validAgentCallWithData) {
//        Option(document.getElementById("view-overdue-upcoming-updates")) shouldBe None
//      }
//
//      "not display any obligation sections when user has no obligations" in new Setup(validUKPropertyBusinessAgentCall) {
//        Option(layoutContent.getElementById("quarterly")) shouldBe None
//        Option(layoutContent.getElementById("prevyears")) shouldBe None
//      }
//
//      "render the view all your businesses link" in new Setup(validAgentCallWithData) {
//        document.getElementById("view-all-businesses-link").text() shouldBe viewAllBusinessesText
//      }
//    }
