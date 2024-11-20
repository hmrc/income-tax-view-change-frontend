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

package views.optOut

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.ConfirmedOptOutViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import services.optout._
import testUtils.TestSupport
import views.html.optOut.ConfirmedOptOut

class ConfirmedOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmedOptOutView: ConfirmedOptOut = app.injector.instanceOf[ConfirmedOptOut]
  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val optOutTaxYear: OptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, taxYear)

  class Setup(isAgent: Boolean = true,
              state: OptOutState = OneYearOptOutFollowedByMandated,
              showReportingFrequencyContent: Boolean) {
    private val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(state))
    val pageDocument: Document = Jsoup.parse(contentAsString(confirmedOptOutView(viewModel, isAgent, showReportingFrequencyContent)))
  }

  object confirmOptOutMessages {
    val singleYearReportingUpdatesInset = s"From 6 April ${taxYear.endYear}, you’ll be required to send quarterly updates through software compatible with Making Tax Digital for Income Tax (opens in new tab)"
    val singleYearReportingUpdatesListP1 = "HMRC lowered the income threshold for Making Tax Digital"
    val singleYearReportingUpdatesListP2 = "you reported an increase in your qualifying income in last year’s tax return"
    val singleYearReportingUpdatesP1 = "This could be because:"
    val singleYearReportingUpdatesHeading = "Reporting quarterly from next tax year onwards"
    val singleYearSoftwareCompatibleLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"


    val heading: String = "Opt out completed"
    val title: String = messages("htmlTitle", "Opt out completed")
    val panelBodyOneYear: String = s"You are reporting annually for the ${taxYear.startYear} to ${taxYear.endYear} tax year"
    val panelBodyMultiYear: String = s"You are reporting annually from the ${taxYear.startYear} to ${taxYear.endYear} tax year onwards"
    val submitTaxHeading: String = "Submit your tax return"
    val submitTaxP1: String = "When reporting annually, you can submit your tax return directly through your HMRC online account or software compatible."
    val submitTaxP2: String = "However, compatible software is required for any tax years for which you are reporting quarterly."
    val yourRevisedDeadlinesHeading: String = "Your revised deadlines"
    val yourRevisedDeadlinesContentP1: String = s"Your tax return for the ${taxYear.startYear} to ${taxYear.endYear} tax year is due by 31 January ${taxYear.nextYear.endYear}."
    val yourRevisedDeadlinesContentP2: String = "You can decide at any time to opt back in to reporting quarterly for all of your businesses on your reporting frequency page."
    val reportQuarterly: String = "You could be required to report quarterly again in the future if:"
    val multiYearReportingUpdatesHeading = "Reporting quarterly again in the future"
    val multiYearReportingUpdatesP1 = "You could be required to report quarterly again in the future if:"
    val multiYearReportingUpdatesListP1 = "HMRC lowers the income threshold for Making Tax Digital"
    val multiYearReportingUpdatesListP2 = "you report an increase in your qualifying income in a tax return"
    val multiYearReportingUpdatesInset = s"For example, if your qualifying income exceeds the threshold in the ${optOutTaxYear.taxYear.startYear} to ${optOutTaxYear.taxYear.endYear} tax year, you would have to report quarterly from 6 April ${optOutTaxYear.taxYear.nextYear.endYear}."
    val multiYearReportingUpdatesP2 = "If this happens, we will write to you to let you know."
    val multiYearReportingUpdatesP3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital (opens in new tab) ."
    val multiYearReportingUpdatesP3Link = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-will-need-to-sign-up"
  }

  "Opt-out confirmed page" should {

    "have the correct title" in new Setup(false, showReportingFrequencyContent = false) {
      pageDocument.title() shouldBe confirmOptOutMessages.title
    }

    "have the correct confirmation panel content" when {
      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated, showReportingFrequencyContent = false) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe confirmOptOutMessages.heading
        panel.child(1).text() shouldBe confirmOptOutMessages.panelBodyOneYear
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault, showReportingFrequencyContent = false) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe confirmOptOutMessages.heading
        panel.child(1).text() shouldBe confirmOptOutMessages.panelBodyMultiYear
      }
    }

    "have the correct submit your tax return content" in new Setup(isAgent = false, showReportingFrequencyContent = false) {
      val submitTaxBlock: Element = pageDocument.getElementById("submit-tax")

      submitTaxBlock.getElementById("submit-tax-heading").text() shouldBe confirmOptOutMessages.submitTaxHeading
      submitTaxBlock.getElementById("submit-tax-p1").text() shouldBe confirmOptOutMessages.submitTaxP1
      submitTaxBlock.getElementById("submit-tax-p2").text() shouldBe confirmOptOutMessages.submitTaxP2
    }

    "Individual - revised deadlines content " in new Setup(isAgent = false, showReportingFrequencyContent = false) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text shouldBe ""
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.show().url
    }

    "Agent - revised deadlines content" in new Setup(isAgent = true, showReportingFrequencyContent = false) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe ""
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent.url
    }

    "Individual - revised deadlines with reporting frequency content" in new Setup(isAgent = false, showReportingFrequencyContent = true) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP2
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.show().url
    }

    "Agent - revised deadlines with reporting frequency content" in new Setup(isAgent = true, showReportingFrequencyContent = true) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe confirmOptOutMessages.yourRevisedDeadlinesContentP2
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent.url
    }

    "have the correct reporting updates content" when {

      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated, showReportingFrequencyContent = false) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesInset
        reportingUpdateBlock.child(2).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesP1
        reportingUpdateBlock.child(3).child(0).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesListP1
        reportingUpdateBlock.child(3).child(1).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesListP2
        reportingUpdateBlock.getElementById("software-compatible-ext").attr("href") shouldBe confirmOptOutMessages.singleYearSoftwareCompatibleLink
        reportingUpdateBlock.getElementById("sign-up-criteria-ext").attr("href") shouldBe confirmOptOutMessages.multiYearReportingUpdatesP3Link
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault, showReportingFrequencyContent = false) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP1
        reportingUpdateBlock.child(2).child(0).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesListP1
        reportingUpdateBlock.child(2).child(1).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesListP2
        reportingUpdateBlock.child(3).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesInset
        reportingUpdateBlock.child(4).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP2
        reportingUpdateBlock.child(5).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP3
        reportingUpdateBlock.getElementById("sign-up-criteria-ext").attr("href") shouldBe confirmOptOutMessages.multiYearReportingUpdatesP3Link

      }
    }
  }
}
