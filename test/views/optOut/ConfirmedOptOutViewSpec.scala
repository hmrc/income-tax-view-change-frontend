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
import enums.NextTaxYear
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Voluntary
import models.optout.ConfirmedOptOutViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.Helpers._
import services.optout._
import testUtils.TestSupport
import viewUtils.ConfirmedOptOutViewUtils
import views.html.optOut.ConfirmedOptOut
import views.messages.ConfirmedOptOutMessages

class ConfirmedOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val confirmedOptOutView: ConfirmedOptOut = app.injector.instanceOf[ConfirmedOptOut]
  val confirmedOptOutViewUtils: ConfirmedOptOutViewUtils = app.injector.instanceOf[ConfirmedOptOutViewUtils]

  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val optOutTaxYear: OptOutTaxYear = CurrentOptOutTaxYear(Voluntary, taxYear)

  val submitYourTaxReturnContent = confirmedOptOutViewUtils.submitYourTaxReturnContent(Voluntary, Voluntary, Voluntary, NextTaxYear, isMultiYear = true, isPreviousYearCrystallised = false)

  class Setup(
               isAgent: Boolean = true,
               state: OptOutState = OneYearOptOutFollowedByMandated,
               showReportingFrequencyContent: Boolean
             ) {

    private val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(state))
    val pageDocument: Document = Jsoup.parse(contentAsString(confirmedOptOutView(viewModel, isAgent, showReportingFrequencyContent, submitYourTaxReturnContent)))
  }

  "Opt-out confirmed page" should {

    "have the correct title" in new Setup(false, showReportingFrequencyContent = false) {
      pageDocument.title() shouldBe ConfirmedOptOutMessages.title
    }

    "have the correct confirmation panel content" when {

      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated, showReportingFrequencyContent = false) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe ConfirmedOptOutMessages.heading
        panel.child(1).text() shouldBe ConfirmedOptOutMessages.panelBodyOneYear
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault, showReportingFrequencyContent = false) {
        val panel = pageDocument.select(".govuk-panel--confirmation").get(0)
        panel.child(0).text() shouldBe ConfirmedOptOutMessages.heading
        panel.child(1).text() shouldBe ConfirmedOptOutMessages.panelBodyMultiYear
      }
    }

    "have the correct submit your tax return content" in new Setup(isAgent = false, showReportingFrequencyContent = false) {
      val submitTaxBlock: Element = pageDocument.getElementById("submit-tax")

      submitTaxBlock.getElementById("submit-tax-heading").text() shouldBe ConfirmedOptOutMessages.submitTaxHeading
      submitTaxBlock.getElementById("now-you-have-opted-out").text() shouldBe ConfirmedOptOutMessages.submitTaxP1
      submitTaxBlock.getElementById("tax-year-reporting-quarterly").text() shouldBe ConfirmedOptOutMessages.submitTaxP2
    }

    "Individual - revised deadlines content " in new Setup(isAgent = false, showReportingFrequencyContent = false) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text shouldBe ""
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.show().url
    }

    "Agent - revised deadlines content" in new Setup(isAgent = true, showReportingFrequencyContent = false) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe ""
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent().url
    }

    "Individual - revised deadlines with reporting frequency content" in new Setup(isAgent = false, showReportingFrequencyContent = true) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP2
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.show().url
    }

    "Agent - revised deadlines with reporting frequency content" in new Setup(isAgent = true, showReportingFrequencyContent = true) {
      val revisedDeadlinesBlock: Element = pageDocument.getElementById("revised-deadlines")
      revisedDeadlinesBlock.getElementById("revised-deadlines-heading").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesHeading
      revisedDeadlinesBlock.getElementById("revised-deadlines-p1").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP1
      revisedDeadlinesBlock.getElementById("your-reporting-frequency-block").text() shouldBe ConfirmedOptOutMessages.yourRevisedDeadlinesContentP2
      revisedDeadlinesBlock.getElementById("view-upcoming-updates-link").attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent().url
    }

    "have the correct reporting updates content" when {

      "one year opt out is followed by mandated ITSA Status" in new Setup(isAgent = false, state = OneYearOptOutFollowedByMandated, showReportingFrequencyContent = false) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe ConfirmedOptOutMessages.singleYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe ConfirmedOptOutMessages.singleYearReportingUpdatesInset
        reportingUpdateBlock.child(2).text() shouldBe ConfirmedOptOutMessages.singleYearReportingUpdatesP1
        reportingUpdateBlock.child(3).child(0).text() shouldBe ConfirmedOptOutMessages.singleYearReportingUpdatesListP1
        reportingUpdateBlock.child(3).child(1).text() shouldBe ConfirmedOptOutMessages.singleYearReportingUpdatesListP2
        reportingUpdateBlock.getElementById("software-compatible-ext").attr("href") shouldBe ConfirmedOptOutMessages.singleYearSoftwareCompatibleLink
        reportingUpdateBlock.getElementById("sign-up-criteria-ext").attr("href") shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesP3Link
      }

      "multi year opt out" in new Setup(isAgent = false, state = MultiYearOptOutDefault, showReportingFrequencyContent = false) {
        val reportingUpdateBlock: Element = pageDocument.getElementById("reporting-updates")
        reportingUpdateBlock.child(0).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesHeading
        reportingUpdateBlock.child(1).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesP1
        reportingUpdateBlock.child(2).child(0).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesListP1
        reportingUpdateBlock.child(2).child(1).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesListP2
        reportingUpdateBlock.child(3).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesInset
        reportingUpdateBlock.child(4).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesP2
        reportingUpdateBlock.child(5).text() shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesP3
        reportingUpdateBlock.getElementById("sign-up-criteria-ext").attr("href") shouldBe ConfirmedOptOutMessages.multiYearReportingUpdatesP3Link

      }
    }
  }
}
