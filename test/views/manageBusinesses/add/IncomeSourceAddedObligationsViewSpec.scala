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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.twirl.api.HtmlFormat
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.{finalDeclaration2024_2025taxYear, taxYear2024_2025quarterlyDates}
import testUtils.ViewSpec
import views.constants.IncomeSourceAddedObligationsConstants._
import views.html.manageBusinesses.add.IncomeSourceAddedObligationsView
import views.messages.IncomeSourceAddedMessages._

import java.time.LocalDate

class IncomeSourceAddedObligationsViewSpec extends ViewSpec {

  val view: IncomeSourceAddedObligationsView = app.injector.instanceOf[IncomeSourceAddedObligationsView]

  object IdSelectors {
    val mayHaveOverdueUpdates = "may-have-overdue-updates"
    val yourRevisedDeadlinesH2 = "your-revised-deadlines"
    val fewMinutesWarning = "few-minutes-warning"
    val accountUpdated = "account-updated"
    val viewReportingObligations = "view-reporting-obligations"
    val viewBusinesses = "view-businesses"
    val submitUpdatesSoftware = "submit-updates-in-software"
    val submitCompatibleSoftwareQuarterly = "submit-via-compatible-software-quarterly"
    val submitCompatibleSoftwareAnnual = "submit-via-compatible-software-annual"
    val warningInset = "warning-inset"
    val quarterlyList = "quarterly-list"

    val annualInset = "annual-warning-inset"
    val quarterlyInset = "quarterly-warning-inset"

    val finalDeclaration = "final-declaration"
  }

  val getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url
  val getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url
  val getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val softwareLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, 2023, showPrevTaxYears = false)

//  final-declaration

  def viewHelper(
                  viewModel: ObligationsViewModel,
                  incomeSourceType: IncomeSourceType,
                  reportingMethod: ChosenReportingMethod,
                  scenario: SignedUpForMTD,
                  reportingFrequencyEnabled: Boolean
                ) =
    view(
      viewModel = viewModel,
      isAgent = false,
      incomeSourceType = incomeSourceType,
      businessName = None,
      currentDate = day,
      currentTaxYear = 2025,
      nextTaxYear = 2026,
      isBusinessHistoric = false,
      reportingMethod = reportingMethod,
      getSoftwareUrl = appConfig.compatibleSoftwareLink,
      getReportingFrequencyUrl = getReportingFrequencyUrl,
      getNextUpdatesUrl = getNextUpdatesUrl,
      getManageBusinessUrl = getManageBusinessUrl,
      scenario = scenario,
      reportingFrequencyEnabled = reportingFrequencyEnabled
    )

  "IncomeSourceAddedObligationsView - Individual" when {

    "ReportingFrequency Feature switch is enabled" when {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"given a income source type: $incomeSourceType" should {

          "show the panel and banner" in {

            val businessName = {
              incomeSourceType match {
                case SelfEmployment => Some(h1SoleTraderBusinessName)
                case UkProperty => None
                case ForeignProperty => None
              }
            }

            val page =
              view(
                viewModel = viewModel,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = businessName,
                currentDate = day,
                currentTaxYear = 2025,
                nextTaxYear = 2026,
                isBusinessHistoric = true,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = getReportingFrequencyUrl,
                getNextUpdatesUrl = getNextUpdatesUrl,
                getManageBusinessUrl = getManageBusinessUrl,
                scenario = SignUpNextYearOnly,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(page.body)

            val layoutContent: Element = document.selectHead("#main-content")
            val banner: Element = layoutContent.getElementsByTag("h1").first()
            val subText: Elements = layoutContent.select("div").eq(3)

            incomeSourceType match {
              case SelfEmployment =>
                banner.text() shouldBe h1SoleTraderBusinessName
                subText.text shouldBe s"$h1SoleTraderBusinessName $headingBase"
              case UkProperty =>
                banner.text() shouldBe h1UKProperty
                subText.text shouldBe s"$h1UKProperty $headingBase"
              case ForeignProperty =>
                banner.text() shouldBe h1ForeignProperty
                subText.text shouldBe s"$h1ForeignProperty $headingBase"
            }
          }
        }

        s"SignUpNextYearOnly - $incomeSourceType" should {

          "have the correct content" in {

            val view: HtmlFormat.Appendable =
              viewHelper(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType = incomeSourceType,
                reportingMethod = ChosenReportingMethod.Quarterly,
                scenario = SignUpNextYearOnly,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"
            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning
            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe viewReportingObligationsParagraph
            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
          }
        }

        s"NotSigningUp - $incomeSourceType" should {

          "have the correct content" in {

            val view: HtmlFormat.Appendable =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                NotSigningUp,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"
            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning
            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "This new business is opted out of Making Tax Digital for Income Tax. You can decide at any time to opt all your businesses out of Making Tax Digital for Income Tax on your reporting obligations page."
            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph

          }
        }

        s"SignUpCurrentYearOnly - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                SignUpCurrentYearOnly,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe viewReportingObligationsParagraph
            document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"SignUpBothYears - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                SignUpBothYears,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe viewReportingObligationsParagraph
            document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"OnlyOneYearAvailableToSignUp - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                OnlyOneYearAvailableToSignUp,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "This new business is opted out of Making Tax Digital for Income Tax. Find out more about your reporting obligations"
            document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"OptedOut - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                OptedOut,
                reportingFrequencyEnabled = true
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "This new business is opted out of Making Tax Digital for Income Tax. Find out more about your reporting obligations"
            document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"Obligations Section - $incomeSourceType" when {

          "getOverdueObligationsComponents.messageKey == hybrid" should {

            "have the correct content" in {

              val dayAfterFinalDeclarationDeadline2023_2024: LocalDate = LocalDate.of(2025, 2, 4)

              val viewHelper =
                view(
                  viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = None,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  currentTaxYear = 2025,
                  nextTaxYear = 2026,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = getReportingFrequencyUrl,
                  getNextUpdatesUrl = getNextUpdatesUrl,
                  getManageBusinessUrl = getManageBusinessUrl,
                  scenario = SignUpNextYearOnly,
                  reportingFrequencyEnabled = true
                )

              val document: Document = Jsoup.parse(viewHelper.body)

              document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
              document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by <b>5 February 2025</b> for the quarterly period 6 October 2024 to 5 January 2025"

              document.getElementById(IdSelectors.annualInset).text() shouldBe "You have 1 overdue update. You must submit your yearly tax return and pay the tax you owe."
              document.getElementById(IdSelectors.quarterlyInset).text() shouldBe "You have 4 overdue updates. You must submit these updates with all required income and expenses through your compatible software."

              document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

              document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
              document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

              document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
              document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

              document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
              document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

              document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
              document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
              document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
            }
          }

          "getOverdueObligationsComponents.messageKey != hybrid" should {

            "display the correct content and the warning inset" in {

              val viewModelWithCurrentYearQuarterly: ObligationsViewModel =
                ObligationsViewModel(
                  quarterlyObligationsDates = Seq(taxYear2024_2025quarterlyDates),
                  finalDeclarationDates = Seq(finalDeclaration2024_2025taxYear),
                  currentTaxYear = 2025,
                  showPrevTaxYears = true
                )

              val viewHelper =
                view(
                  viewModel = viewModelWithCurrentYearQuarterly,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = testName,
                  currentDate = dayAfterThirdQuarterDeadline2024_2025,
                  currentTaxYear = 2025,
                  nextTaxYear = 2026,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Quarterly,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = controllers.routes.ReportingFrequencyPageController.show(false).url,
                  getNextUpdatesUrl = controllers.routes.NextUpdatesController.show().url,
                  getManageBusinessUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url,
                  scenario = SignUpNextYearOnly,
                  reportingFrequencyEnabled = true
                )

              val document: Document = Jsoup.parse(viewHelper.body)

              document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
              document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2024 to 2025 tax year is due by <b>5 May 2025</b> for the quarterly period 6 January 2025 to 5 April 2025"

              document.getElementById(IdSelectors.warningInset).text() shouldBe "You have 3 overdue updates for 9 months of the 2024 to 2025 tax year. You must submit these updates with all required income and expenses through your compatible software."

              document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

              document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
              document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

              document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
              document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

              document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
              document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

              document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
              document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
              document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
            }
          }
        }

        s"Final Declaration Section - $incomeSourceType" when {

          "getOverdueObligationsComponents.messageKey == hybrid" should {

            "have the correct content" in {

              val dayAfterFinalDeclarationDeadline2023_2024: LocalDate = LocalDate.of(2025, 2, 4)

              val viewHelper =
                view(
                  viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                  isAgent = false,
                  incomeSourceType = incomeSourceType,
                  businessName = None,
                  currentDate = dayAfterFinalDeclarationDeadline2023_2024,
                  currentTaxYear = 2025,
                  nextTaxYear = 2026,
                  isBusinessHistoric = false,
                  reportingMethod = ChosenReportingMethod.Annual,
                  getSoftwareUrl = appConfig.compatibleSoftwareLink,
                  getReportingFrequencyUrl = getReportingFrequencyUrl,
                  getNextUpdatesUrl = getNextUpdatesUrl,
                  getManageBusinessUrl = getManageBusinessUrl,
                  scenario = SignUpNextYearOnly,
                  reportingFrequencyEnabled = true
                )

              val document: Document = Jsoup.parse(viewHelper.body)

              document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading

              document.getElementById(IdSelectors.finalDeclaration).text() shouldBe "Your tax return for the 2024 to 2025 tax year is due by <b>31 January 2026</b>"

              document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

              document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
              document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

              document.getElementById(IdSelectors.viewReportingObligations).text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
              document.getElementById("reporting-obligations-link").link.attr("href") shouldBe getReportingFrequencyUrl

              document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
              document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

              document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
              document.getElementById(IdSelectors.submitCompatibleSoftwareAnnual).text() shouldBe submitCompatibleSoftwareAnnualParagraph
              document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
            }
          }
        }
      }
    }

    "ReportingFrequency Feature switch is disabled" when {

      Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

        s"given a income source type: $incomeSourceType" should {

          "show the panel and banner" in {

            val businessName = {
              incomeSourceType match {
                case SelfEmployment => Some(h1SoleTraderBusinessName)
                case UkProperty => None
                case ForeignProperty => None
              }
            }

            val page =
              view(
                viewModel = viewModel,
                isAgent = false,
                incomeSourceType = incomeSourceType,
                businessName = businessName,
                currentDate = day,
                currentTaxYear = 2025,
                nextTaxYear = 2026,
                isBusinessHistoric = true,
                reportingMethod = ChosenReportingMethod.Quarterly,
                getSoftwareUrl = appConfig.compatibleSoftwareLink,
                getReportingFrequencyUrl = getReportingFrequencyUrl,
                getNextUpdatesUrl = getNextUpdatesUrl,
                getManageBusinessUrl = getManageBusinessUrl,
                scenario = SignUpNextYearOnly,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(page.body)

            val layoutContent: Element = document.selectHead("#main-content")
            val banner: Element = layoutContent.getElementsByTag("h1").first()
            val subText: Elements = layoutContent.select("div").eq(3)

            incomeSourceType match {
              case SelfEmployment =>
                banner.text() shouldBe h1SoleTraderBusinessName
                subText.text shouldBe s"$h1SoleTraderBusinessName $headingBase"
              case UkProperty =>
                banner.text() shouldBe h1UKProperty
                subText.text shouldBe s"$h1UKProperty $headingBase"
              case ForeignProperty =>
                banner.text() shouldBe h1ForeignProperty
                subText.text shouldBe s"$h1ForeignProperty $headingBase"
            }
          }
        }

        s"SignUpNextYearOnly - $incomeSourceType" should {

          "have the correct content" in {

            val view: HtmlFormat.Appendable =
              viewHelper(
                viewModel = viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType = incomeSourceType,
                reportingMethod = ChosenReportingMethod.Quarterly,
                scenario = SignUpNextYearOnly,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl


            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"NotSigningUp - $incomeSourceType" should {

          "have the correct content" in {

            val view: HtmlFormat.Appendable =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                NotSigningUp,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"SignUpCurrentYearOnly - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                SignUpCurrentYearOnly,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.mayHaveOverdueUpdates).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink

          }
        }

        s"SignUpBothYears - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                SignUpBothYears,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"OnlyOneYearAvailableToSignUp - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                OnlyOneYearAvailableToSignUp,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }

        s"OptedOut - $incomeSourceType" should {

          "have the correct content" in {

            val view =
              viewHelper(
                viewModelTwoQuarterYearThenQuarterlyYear,
                incomeSourceType,
                ChosenReportingMethod.Quarterly,
                OptedOut,
                reportingFrequencyEnabled = false
              )

            val document: Document = Jsoup.parse(view.body)

            document.getElementById(IdSelectors.yourRevisedDeadlinesH2).text() shouldBe yourRevisedDeadlinesHeading
            document.getElementById(IdSelectors.quarterlyList).text() shouldBe "Your next quarterly update for the 2023 to 2024 tax year is due by <b>5 February 2024</b> for the quarterly period 6 October 2023 to 5 January 2024"

            document.getElementById(IdSelectors.fewMinutesWarning).text() shouldBe fewMinutesWarning

            document.getElementById(IdSelectors.accountUpdated).text() shouldBe accountUpdatedParagraph
            document.getElementById("updates-and-deadlines-link").link.attr("href") shouldBe nextUpdatesUrl

            Option(document.getElementById(IdSelectors.viewReportingObligations)) shouldBe None

            document.getElementById(IdSelectors.viewBusinesses).text() shouldBe viewBusinessesParagraph
            document.getElementById("view-businesses-link").link.attr("href") shouldBe getManageBusinessUrl

            document.getElementById(IdSelectors.submitUpdatesSoftware).text() shouldBe submitUpdatesInSoftwareH2
            document.getElementById(IdSelectors.submitCompatibleSoftwareQuarterly).text() shouldBe submitCompatibleSoftwareQuarterlyParagraph
            document.getElementById("compatible-software-link").link.attr("href") shouldBe softwareLink
          }
        }
      }
    }
  }
}