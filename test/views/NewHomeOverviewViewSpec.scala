/*
 * Copyright 2026 HM Revenue & Customs
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

package views

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testConstants.ANewCreditAndRefundModel
import testUtils.{TestSupport, ViewSpec}
import views.html.NewHomeOverviewView
import models.incomeSourceDetails.TaxYear


class NewHomeOverviewViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val newHomeOverviewView: NewHomeOverviewView = app.injector.instanceOf[NewHomeOverviewView]
  val testTaxYear: TaxYear = fixedTaxYear

  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   currentTaxYear: TaxYear = testTaxYear,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false) {

    val testMessages: Messages = if (welshLang) {
      app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
    } else {
      messages
    }

    val testUrl = "testUrl"

    lazy val page: HtmlFormat.Appendable =
      newHomeOverviewView(
        origin,
        isAgent,
        currentTaxYear,
        yourTasksUrl,
        recentActivityUrl,
        overViewUrl,
        helpUrl)(testMessages, appConfig, FakeRequest())
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }


  "New Home Overview page for individuals" should {
    "display the the correct content" in new TestSetup() {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "Check what you owe and make a payment"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/what-you-owe")

      document.select(".govuk-summary-card-no-border").get(1).text() shouldBe "View payment, credit and refund history"
      document.select(".govuk-summary-card-no-border").get(1).hasCorrectHref("/report-quarterly/income-and-expenses/view/payment-refund-history")

      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/money-in-your-account")

      document.select(".govuk-summary-card-no-border").get(3).text() shouldBe "Adjust payments on account"
      document.select(".govuk-summary-card-no-border").get(3).hasCorrectHref("/report-quarterly/income-and-expenses/view/adjust-poa/start")

      document.select("h2.govuk-heading-m").get(1).text() shouldBe "Deadlines and reporting obligations"
      document.select(".govuk-summary-card-no-border").get(4).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(4).hasCorrectHref("/report-quarterly/income-and-expenses/view/submission-deadlines")

      document.select(".govuk-summary-card-no-border").get(5).text() shouldBe "Check your reporting obligations"
      document.select(".govuk-summary-card-no-border").get(5).hasCorrectHref("/report-quarterly/income-and-expenses/view/reporting-frequency")

      document.select("h2.govuk-heading-m").get(2).text() shouldBe "Income sources"
      document.select(".govuk-summary-card-no-border").get(6).text() shouldBe "Add, manage or cease a business or income source"
      document.select(".govuk-summary-card-no-border").get(6).hasCorrectHref("/report-quarterly/income-and-expenses/view/manage-your-businesses")

      document.select("h2.govuk-heading-m").get(3).text() shouldBe "Tax year summaries"
      document.select(".govuk-summary-card-no-border").get(7).text() shouldBe "View all tax years"
      document.select(".govuk-summary-card-no-border").get(7).hasCorrectHref("/report-quarterly/income-and-expenses/view/tax-years")

      document.select(".govuk-summary-card-no-border").get(8).text() shouldBe s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast"
      document.select(".govuk-summary-card-no-border").get(8).hasCorrectHref(s"/report-quarterly/income-and-expenses/view/tax-year-summary/${testTaxYear.endYear}")

      document.select("h2.govuk-heading-m").get(4).text() shouldBe "Penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(9).text() shouldBe "Check Self Assessment penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(9).link.attr("href") should include("/view-penalty/self-assessment")

      document.select(".govuk-summary-card-no-border").get(10).text() shouldBe "View your late payment penalties"
      document.select(".govuk-summary-card-no-border").get(10).link.attr("href") should include("/view-penalty/self-assessment#lppTab")

      document.select(".govuk-summary-card-no-border").get(11).text() shouldBe "View your late submission penalties"
      document.select(".govuk-summary-card-no-border").get(11).link.attr("href") should include("/view-penalty/self-assessment#lspTab")
    }
  }


  "New Home Overview page for agents" should {
    "display the the correct content" in new TestSetup(isAgent = true) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "Check what you owe and make a payment"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/what-your-client-owes")

      document.select(".govuk-summary-card-no-border").get(1).text() shouldBe "View payment, credit and refund history"
      document.select(".govuk-summary-card-no-border").get(1).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/payment-refund-history")

      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/money-in-your-account")

      document.select(".govuk-summary-card-no-border").get(3).text() shouldBe "Adjust payments on account"
      document.select(".govuk-summary-card-no-border").get(3).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/adjust-poa/start")

      document.select("h2.govuk-heading-m").get(1).text() shouldBe "Deadlines and reporting obligations"
      document.select(".govuk-summary-card-no-border").get(4).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(4).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/submission-deadlines")

      document.select(".govuk-summary-card-no-border").get(5).text() shouldBe "Check your reporting obligations"
      document.select(".govuk-summary-card-no-border").get(5).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/reporting-frequency")

      document.select("h2.govuk-heading-m").get(2).text() shouldBe "Income sources"
      document.select(".govuk-summary-card-no-border").get(6).text() shouldBe "Add, manage or cease a business or income source"
      document.select(".govuk-summary-card-no-border").get(6).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/manage-your-businesses")

      document.select("h2.govuk-heading-m").get(3).text() shouldBe "Tax year summaries"
      document.select(".govuk-summary-card-no-border").get(7).text() shouldBe "View all tax years"
      document.select(".govuk-summary-card-no-border").get(7).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/tax-years")

      document.select(".govuk-summary-card-no-border").get(8).text() shouldBe s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast"
      document.select(".govuk-summary-card-no-border").get(8).hasCorrectHref(s"/report-quarterly/income-and-expenses/view/agents/tax-year-summary/${testTaxYear.endYear}")

      document.select("h2.govuk-heading-m").get(4).text() shouldBe "Penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(9).text() shouldBe "Check Self Assessment penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(9).link.attr("href") should include("/view-penalty/self-assessment/agent")

      document.select(".govuk-summary-card-no-border").get(10).text() shouldBe "View your late payment penalties"
      document.select(".govuk-summary-card-no-border").get(10).link.attr("href") should include("/view-penalty/self-assessment/agent#lppTab")

      document.select(".govuk-summary-card-no-border").get(11).text() shouldBe "View your late submission penalties"
      document.select(".govuk-summary-card-no-border").get(11).link.attr("href") should include("/view-penalty/self-assessment/agent#lspTab")
    }
  }

}
