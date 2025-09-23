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

package views.optIn.newJourney

import models.incomeSourceDetails.TaxYear
import models.optin.newJourney.SignUpCompletedViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.optIn.newJourney.SignUpCompletedView

class SignUpCompletedViewSpec extends TestSupport {

  object SignUpCompletedViewMessages {
    val currentYearCompletedHeading = "Sign up completed"
    val currentYearCompletedTitle = "Sign up completed - Manage your Self Assessment - GOV.UK"
    val currentYearCompletedPanelDesc = "You need to use Making Tax Digital for Income Tax from now on"
    val currentYearCompletedDesc = "You may have overdue updates for the 2025 to 2026 tax year. You must submit these updates with all required income and expenses through your compatible software."

    val currentYearYourRevisedDeadlinesHeading = "Your revised deadlines"
    val currentYearYourRevisedDeadlinesDesc = "Your revised deadlines will be available in the next few minutes."
    val currentYearYourRevisedDeadlinesDesc2 = "Even if they are not displayed right away on the updates and deadlines page, your account has been updated."
    val currentYearYourRevisedDeadlinesDesc3 = "You can decide at any time to opt out of Making Tax Digital for Income Tax for all your businesses on your reporting obligations page."

    val currentYearSubmitUpdatesHeading = "Submit updates in software"
    val currentYearSubmitUpdatesDesc = "For any tax year you are using Making Tax Digital for Income Tax, you need compatible software (opens in new tab)."

    val currentYearReportingObligationsHeading = "Your reporting obligations in the future"
    val currentYearReportingObligationsDesc = "You are now voluntarily signed up from the 2025 to 2026 tax year onwards, but in future you could be required to use Making Tax Digital for Income Tax if:"
    val currentYearReportingObligationsBullet1 = "HMRC lowers the income threshold for it"
    val currentYearReportingObligationsBullet2 = "you report an increase in your qualifying income in a tax return"
    val currentYearReportingObligationsInset = "For example, if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold in the 2025 to 2026 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2027."
    val currentYearReportingObligationsDesc2 = "If this happens, we will write to you to let you know."
    val currentYearReportingObligationsDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

    val currentYearReportingObligationsMandatedHeading = "Your reporting obligations from the next tax year onwards"
    val currentYearReportingObligationsMandatedDesc = "You have just voluntarily signed up from the 2025 to 2026 tax year."
    val currentYearReportingObligationsMandatedInset = "From 6 April 2026, you will be required to use Making Tax Digital for Income Tax."
    val currentYearReportingObligationsMandatedDesc2 = "This could be because:"
    val currentYearReportingObligationsMandatedBullet1 = "HMRC lowered the income threshold for it"
    val currentYearReportingObligationsMandatedBullet2 = "you reported an increase in your qualifying income in a tax return"
    val currentYearReportingObligationsMandatedDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

    val nextYearCompletedHeading = "Sign up completed"
    val nextYearCompletedTitle = "Sign up completed - Manage your Self Assessment - GOV.UK"
    val nextYearCompletedPanelDesc = "You need to use Making Tax Digital for Income Tax from the 2026 to 2027 tax year onwards"

    val nextYearYourRevisedDeadlinesHeading = "Your revised deadlines"
    val nextYearYourRevisedDeadlinesDesc = "Your deadlines for this business will be available in the next few minutes."
    val nextYearYourRevisedDeadlinesDesc2 = "Even if they are not displayed right away on the updates and deadlines page, your account has been updated."
    val nextYearYourRevisedDeadlinesDesc3 = "You can decide at any time to opt out of Making Tax Digital for Income Tax for all your businesses on your reporting obligations page."

    val nextYearSubmitUpdatesHeading = "Submit updates in software"
    val nextYearSubmitUpdatesDesc = "For any tax year you are using Making Tax Digital for Income Tax, you need compatible software (opens in new tab)."
    val nextYearSubmitUpdatesDescAnnual = "When you are opted out, you can find out here how to file your Self Assessment tax return (opens in new tab)."

    val nextYearReportingObligationsHeading = "Your reporting obligations in the future"
    val nextYearReportingObligationsDesc = "You have just chosen to sign up from the next tax year onwards, but in future you could have to use Making Tax Digital for Income Tax if:"
    val nextYearReportingObligationsBullet1 = "HMRC lowers the income threshold for it"
    val nextYearReportingObligationsBullet2 = "you report an increase in your qualifying income in a tax return"
    val nextYearReportingObligationsInset = "For example, if your income from self-employment or property, or both, exceeds the threshold in the 2026 to 2027 tax year, you would have to report quarterly from 6 April 2028."
    val nextYearReportingObligationsDesc2 = "If this happens, we will write to you to let you know."
    val nextYearReportingObligationsDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

    val compatibleSoftwareLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    val fileATaxReturnLink = "https://www.gov.uk/log-in-file-self-assessment-tax-return"
    val criteriaForMtdLink = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-will-need-to-sign-up"
  }

  val view: SignUpCompletedView = app.injector.instanceOf[SignUpCompletedView]

  class Setup(
             isAgent: Boolean = false,
             taxYear: TaxYear,
             isCurrentYear: Boolean,
             isCurrentYearAnnual: Boolean = false,
             isNextYearMandated: Boolean = false
             ) {
    val model: SignUpCompletedViewModel = SignUpCompletedViewModel(
      isAgent = isAgent,
      signUpTaxYear = taxYear,
      isCurrentYear = isCurrentYear,
      isCurrentYearAnnual = isCurrentYearAnnual,
      isNextYearMandated = isNextYearMandated
    )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(model)))
  }

  "SignUpCompletedView" when {
    "signing up for the current year" should {
      "render the correct heading" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.currentYearCompletedHeading
      }
      "render the correct title" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.title() shouldBe SignUpCompletedViewMessages.currentYearCompletedTitle
      }
      "render the correct panel description" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.select(".govuk-panel__body").text() shouldBe SignUpCompletedViewMessages.currentYearCompletedPanelDesc
      }
      "render the inset text with overdue updates information" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.getElementById("overdue-updates-inset").text() shouldBe SignUpCompletedViewMessages.currentYearCompletedDesc
      }
      "render the correct 'Your revised deadlines' section" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.getElementById("your-revised-deadlines-heading").text() shouldBe SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesHeading
        pageDocument.getElementById("your-revised-deadlines-desc").text() shouldBe SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc
        pageDocument.getElementById("your-revised-deadlines-desc-2").text() shouldBe SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc2
        pageDocument.getElementById("your-revised-deadlines-desc-2").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        pageDocument.getElementById("your-revised-deadlines-desc-3").text() shouldBe SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc3
        pageDocument.getElementById("your-revised-deadlines-desc-3").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/reporting-frequency"
      }

      "render the correct 'Submit updates in software' section" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.getElementById("submit-updates-heading").text() shouldBe SignUpCompletedViewMessages.currentYearSubmitUpdatesHeading
        pageDocument.getElementById("submit-updates-desc").text() shouldBe SignUpCompletedViewMessages.currentYearSubmitUpdatesDesc
        pageDocument.getElementById("submit-updates-desc").select("a").attr("href") shouldBe SignUpCompletedViewMessages.compatibleSoftwareLink
      }

      "render the correct your reporting obligations section when CY+1 isn't mandated" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.getElementById("your-reporting-obligations-heading").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsHeading
        pageDocument.getElementById("your-reporting-obligations-desc").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsDesc
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsBullet1 + " " + SignUpCompletedViewMessages.currentYearReportingObligationsBullet2
        pageDocument.getElementById("your-reporting-obligations-inset").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsInset
        pageDocument.getElementById("your-reporting-obligations-desc-2").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsDesc2
        pageDocument.getElementById("your-reporting-obligations-desc-3").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsDesc3
        pageDocument.getElementById("your-reporting-obligations-desc-3").select("a").attr("href") shouldBe SignUpCompletedViewMessages.criteriaForMtdLink
      }

      "render the correct your reporting obligations section when CY+1 is mandated" in new Setup(taxYear = TaxYear(2025, 2026), isCurrentYear = true, isNextYearMandated = true) {
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-heading").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedHeading
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-desc").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-inset").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedInset
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-desc-2").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc2
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedBullet1 + " " + SignUpCompletedViewMessages.currentYearReportingObligationsMandatedBullet2
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-desc-3").text() shouldBe SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc3
        pageDocument.getElementById("your-reporting-obligations-ny-mandated-desc-3").select("a").attr("href") shouldBe SignUpCompletedViewMessages.criteriaForMtdLink
      }

      "render the correct links for agents" in new Setup(isAgent = true, taxYear = TaxYear(2025, 2026), isCurrentYear = true) {
        pageDocument.getElementById("your-revised-deadlines-desc-2").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/next-updates"
        pageDocument.getElementById("your-revised-deadlines-desc-3").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
      }
    }
    "signing up for the next year" should {
      "render the correct heading" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.select("h1").text() shouldBe SignUpCompletedViewMessages.nextYearCompletedHeading
      }
      "render the correct title" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.title() shouldBe SignUpCompletedViewMessages.nextYearCompletedTitle
      }
      "render the correct panel description" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.select(".govuk-panel__body").text() shouldBe SignUpCompletedViewMessages.nextYearCompletedPanelDesc
      }
      "render the correct 'Your revised deadlines' section" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.getElementById("your-revised-deadlines-heading").text() shouldBe SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesHeading
        pageDocument.getElementById("your-revised-deadlines-inset").text() shouldBe SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc
        pageDocument.getElementById("your-revised-deadlines-desc-2").text() shouldBe SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc2
        pageDocument.getElementById("your-revised-deadlines-desc-2").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        pageDocument.getElementById("your-revised-deadlines-desc-3").text() shouldBe SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc3
        pageDocument.getElementById("your-revised-deadlines-desc-3").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/reporting-frequency"
      }

      "render the correct 'Submit updates in software' section" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.getElementById("submit-updates-heading").text() shouldBe SignUpCompletedViewMessages.nextYearSubmitUpdatesHeading
        pageDocument.getElementById("submit-updates-desc").text() shouldBe SignUpCompletedViewMessages.nextYearSubmitUpdatesDesc
        pageDocument.getElementById("submit-updates-desc").select("a").attr("href") shouldBe SignUpCompletedViewMessages.compatibleSoftwareLink
      }
      "render the correct 'Submit updates in software' section when CY is annual" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false, isCurrentYearAnnual = true) {
        pageDocument.getElementById("submit-updates-heading").text() shouldBe SignUpCompletedViewMessages.nextYearSubmitUpdatesHeading
        pageDocument.getElementById("submit-updates-desc").text() shouldBe SignUpCompletedViewMessages.nextYearSubmitUpdatesDesc
        pageDocument.getElementById("submit-updates-desc").select("a").attr("href") shouldBe SignUpCompletedViewMessages.compatibleSoftwareLink
        pageDocument.getElementById("submit-updates-desc-2").text() shouldBe SignUpCompletedViewMessages.nextYearSubmitUpdatesDescAnnual
        pageDocument.getElementById("submit-updates-desc-2").select("a").attr("href") shouldBe SignUpCompletedViewMessages.fileATaxReturnLink
      }
      "render the correct your reporting obligations section" in new Setup(taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.getElementById("your-reporting-obligations-heading").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsHeading
        pageDocument.getElementById("your-reporting-obligations-desc").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsDesc
        pageDocument.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsBullet1 + " " + SignUpCompletedViewMessages.nextYearReportingObligationsBullet2
        pageDocument.getElementById("your-reporting-obligations-inset").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsInset
        pageDocument.getElementById("your-reporting-obligations-desc-2").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsDesc2
        pageDocument.getElementById("your-reporting-obligations-desc-3").text() shouldBe SignUpCompletedViewMessages.nextYearReportingObligationsDesc3
        pageDocument.getElementById("your-reporting-obligations-desc-3").select("a").attr("href") shouldBe SignUpCompletedViewMessages.criteriaForMtdLink
      }

      "render the correct links for agents" in new Setup(isAgent = true, taxYear = TaxYear(2026, 2027), isCurrentYear = false) {
        pageDocument.getElementById("your-revised-deadlines-desc-2").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/next-updates"
        pageDocument.getElementById("your-revised-deadlines-desc-3").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
      }
    }
  }
}
