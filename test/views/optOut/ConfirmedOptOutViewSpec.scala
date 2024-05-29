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
import models.optout.{ConfirmedOptOutStates, ConfirmedOptOutViewModel, MultiYearOptOutDefault, OneYearOptOutFollowedByMandated}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optOut.ConfirmedOptOut

class ConfirmedOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmedOptOutView: ConfirmedOptOut = app.injector.instanceOf[ConfirmedOptOut]
  val taxYear: TaxYear = TaxYear.forYearEnd(2024)

  class Setup(isAgent: Boolean = true,
              taxYear: TaxYear = taxYear,
              state: ConfirmedOptOutStates = OneYearOptOutFollowedByMandated) {
    private val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = taxYear, state = state)
    val pageDocument: Document = Jsoup.parse(contentAsString(confirmedOptOutView(viewModel, isAgent)))
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
    val submitTaxP1: String = "For any tax years where you chose to opt out and report annually, you can submit your tax return directly through your HMRC online account or software."
    val submitTaxP2: String = "If you are still reporting quarterly for certain tax years, you are required to send those quarterly updates through compatible software."
    val nextUpdatesDueHeading: String = "Your next updates due"
    val nextUpdatesDueContent: String = "Check the next updates page for the current tax year’s deadlines. Deadlines for future years will not be visible until they become the current year."
    val reportQuarterly: String = "You could be required to report quarterly again in the future if:"
    val multiYearReportingUpdatesHeading = "Reporting quarterly again in the future"
    val multiYearReportingUpdatesP1 = "You could be required to report quarterly again in the future if:"
    val multiYearReportingUpdatesListP1 = "HMRC lowers the income threshold for Making Tax Digital"
    val multiYearReportingUpdatesListP2 = "you report an increase in your qualifying income in a tax return"
    val multiYearReportingUpdatesInset = "For example, if your qualifying income exceeds the threshold in the 2023 to 2024 tax year, you would have to report quarterly from 6 April 2025."
    val multiYearReportingUpdatesP2 = "If this happens, we will write to you to let you know."
    val multiYearReportingUpdatesP3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital (opens in new tab)."
  }

  "Opt-out confirmed page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe confirmOptOutMessages.title
    }

    "have the correct confirmation panel content" when {
      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe confirmOptOutMessages.heading
        panel.child(1).text() shouldBe confirmOptOutMessages.panelBodyOneYear
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe confirmOptOutMessages.heading
        panel.child(1).text() shouldBe confirmOptOutMessages.panelBodyMultiYear
      }
    }

    "have the correct submit your tax return content" in new Setup(isAgent = false) {
      val submitTaxBlock: Element = pageDocument.getElementById("submit-tax")

      submitTaxBlock.getElementById("submit-tax-heading").text() shouldBe confirmOptOutMessages.submitTaxHeading
      submitTaxBlock.getElementById("submit-tax-p1").text() shouldBe confirmOptOutMessages.submitTaxP1
      submitTaxBlock.getElementById("submit-tax-p2").text() shouldBe confirmOptOutMessages.submitTaxP2
    }

    "Individual - have the updates due content " in new Setup(isAgent = false) {
      val updatesDueBlock: Element = pageDocument.getElementById("updates-due")
      updatesDueBlock.getElementById("updates-due-heading").text() shouldBe confirmOptOutMessages.nextUpdatesDueHeading
      updatesDueBlock.getElementById("updates-due-content").text() shouldBe confirmOptOutMessages.nextUpdatesDueContent
      updatesDueBlock.getElementById("next-update-link").attr("href") shouldBe controllers.routes.NextUpdatesController.show().url
    }

    "Agent - have the updates due content" in new Setup(isAgent = true) {
      val updatesDueBlock: Element = pageDocument.getElementById("updates-due")
      updatesDueBlock.getElementById("updates-due-heading").text() shouldBe confirmOptOutMessages.nextUpdatesDueHeading
      updatesDueBlock.getElementById("updates-due-content").text() shouldBe confirmOptOutMessages.nextUpdatesDueContent
      updatesDueBlock.getElementById("next-update-link").attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent.url
    }

    "have the correct reporting updates content" when {

      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesInset
        reportingUpdateBlock.child(2).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesP1
        reportingUpdateBlock.child(3).child(0).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesListP1
        reportingUpdateBlock.child(3).child(1).text() shouldBe confirmOptOutMessages.singleYearReportingUpdatesListP2
        reportingUpdateBlock.getElementById("software-compatible-ext").attr("href") shouldBe confirmOptOutMessages.singleYearSoftwareCompatibleLink
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP1
        reportingUpdateBlock.child(2).child(0).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesListP1
        reportingUpdateBlock.child(2).child(1).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesListP2
        reportingUpdateBlock.child(3).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesInset
        reportingUpdateBlock.child(4).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP2
        reportingUpdateBlock.child(5).text() shouldBe confirmOptOutMessages.multiYearReportingUpdatesP3

      }
    }
  }
}
