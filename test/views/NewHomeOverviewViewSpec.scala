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
                   isSupportingAgent: Boolean = false,
                   currentTaxYear: TaxYear = testTaxYear,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   noChargesToPay: Boolean = false,
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
        isSupportingAgent,
        currentTaxYear,
        yourTasksUrl,
        recentActivityUrl,
        overViewUrl,
        helpUrl,
        noChargesToPay)(testMessages, appConfig, FakeRequest())
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }


  "New Home Overview page for Individuals" should {

    "display the correct 'Charges, credits and payments' section" when {
      "the user has NO charges to pay" in new TestSetup(noChargesToPay = true) {
        val chargesSection: Element = document.selectById("charges-overview-section")
        chargesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"

        chargesSection.hasCorrectOverviewCardLink(
          linkText = "Check what you owe",
          linkHref = "/report-quarterly/income-and-expenses/view/what-you-owe"
        )

        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 1,
          linkText = "View payment, credit and refund history",
          linkHref = "/report-quarterly/income-and-expenses/view/payment-refund-history"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 2,
          linkText = "Check for money in your account",
          linkHref = "/report-quarterly/income-and-expenses/view/money-in-your-account",
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 3,
          linkText = "Adjust payments on account",
          linkHref = "/report-quarterly/income-and-expenses/view/adjust-poa/start",
        )
      }
      "the user has charges to pay" in new TestSetup() {
        val chargesSection: Element = document.selectById("charges-overview-section")
        chargesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"

        chargesSection.hasCorrectOverviewCardLink(
          linkText = "Check what you owe and make a payment",
          linkHref = "/report-quarterly/income-and-expenses/view/what-you-owe"
        )

        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 1,
          linkText = "View payment, credit and refund history",
          linkHref = "/report-quarterly/income-and-expenses/view/payment-refund-history"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 2,
          linkText = "Check for money in your account",
          linkHref = "/report-quarterly/income-and-expenses/view/money-in-your-account",
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 3,
          linkText = "Adjust payments on account",
          linkHref = "/report-quarterly/income-and-expenses/view/adjust-poa/start",
        )
      }
    }

    "display the correct 'Deadlines and reporting' section" in new TestSetup() {
      val deadlinesSection: Element = document.selectById("deadlines-overview-section")
      deadlinesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Deadlines and reporting obligations"
      deadlinesSection.hasCorrectOverviewCardLink(
        linkText = "View updates and deadlines",
        linkHref = "/report-quarterly/income-and-expenses/view/submission-deadlines"
      )
      deadlinesSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = "Check your reporting obligations",
        linkHref = "/report-quarterly/income-and-expenses/view/reporting-frequency"
      )
    }

    "display the correct 'Income sources' section" in new TestSetup() {
      val incomeSection: Element = document.selectById("income-overview-section")
      incomeSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Income sources"
      incomeSection.hasCorrectOverviewCardLink(
        linkText = "Add, manage or cease a business or income source",
        linkHref = "/report-quarterly/income-and-expenses/view/manage-your-businesses"
      )
    }

    "display the correct 'Tax year summaries' section" in new TestSetup() {
      val taxYearSection: Element = document.selectById("tax-year-overview-section")
      taxYearSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Tax year summaries"
      taxYearSection.hasCorrectOverviewCardLink(
        linkText = "View all tax years",
        linkHref = "/report-quarterly/income-and-expenses/view/tax-years"
      )
      taxYearSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast",
        linkHref = s"/report-quarterly/income-and-expenses/view/tax-year-summary/${testTaxYear.endYear}"
      )
    }

    "display the correct 'Penalties and appeals' section" in new TestSetup() {
      val penaltiesSection: Element = document.selectById("penalties-overview-section")
      penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

      penaltiesSection.hasCorrectOverviewCardLink(
        linkText = "Check Self Assessment penalties and appeals",
        linkHref = "/view-penalty/self-assessment",
        exactHrefMatch = false
      )
      penaltiesSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = "View your late payment penalties",
        linkHref = "/view-penalty/self-assessment#lppTab",
        exactHrefMatch = false
      )
      penaltiesSection.hasCorrectOverviewCardLink(
        cardIndex = 2,
        linkText = "View your late submission penalties",
        linkHref = "/view-penalty/self-assessment#lspTab",
        exactHrefMatch = false
      )
    }
  }


  "New Home Overview page for Agents" should {
    "display the correct 'Charges, credits and payments' section" when {
      "the user has charges to pay" in new TestSetup(isAgent = true) {
        val chargesSection: Element = document.selectById("charges-overview-section")
        chargesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"

        chargesSection.hasCorrectOverviewCardLink(
          linkText = "Check what you owe and make a payment",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 1,
          linkText = "View payment, credit and refund history",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/payment-refund-history"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 2,
          linkText = "Check for money in your account",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/money-in-your-account",
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 3,
          linkText = "Adjust payments on account",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start",
        )
      }

      "the user has NO charges to pay" in new TestSetup(isAgent = true, noChargesToPay = true) {
        val chargesSection: Element = document.selectById("charges-overview-section")
        chargesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"

        chargesSection.hasCorrectOverviewCardLink(
          linkText = "Check what you owe",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 1,
          linkText = "View payment, credit and refund history",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/payment-refund-history"
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 2,
          linkText = "Check for money in your account",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/money-in-your-account",
        )
        chargesSection.hasCorrectOverviewCardLink(
          cardIndex = 3,
          linkText = "Adjust payments on account",
          linkHref = "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start",
        )
      }
    }

    "display the correct 'Deadlines and reporting' section" in new TestSetup(isAgent = true) {
      val deadlinesSection: Element = document.selectById("deadlines-overview-section")
      deadlinesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Deadlines and reporting obligations"
      deadlinesSection.hasCorrectOverviewCardLink(
        linkText = "View updates and deadlines",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/submission-deadlines"
      )
      deadlinesSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = "Check your reporting obligations",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
      )
    }

    "display the correct 'Income sources' section" in new TestSetup(isAgent = true) {
      val incomeSection: Element = document.selectById("income-overview-section")
      incomeSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Income sources"
      incomeSection.hasCorrectOverviewCardLink(
        linkText = "Add, manage or cease a business or income source",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
      )
    }

    "display the correct 'Tax year summaries' section" in new TestSetup(isAgent = true) {
      val taxYearSection: Element = document.selectById("tax-year-overview-section")
      taxYearSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Tax year summaries"
      taxYearSection.hasCorrectOverviewCardLink(
        linkText = "View all tax years",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/tax-years"
      )
      taxYearSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast",
        linkHref = s"/report-quarterly/income-and-expenses/view/agents/tax-year-summary/${testTaxYear.endYear}"
      )
    }

    "display the correct 'Penalties and appeals' section" in new TestSetup(isAgent = true) {
      val penaltiesSection: Element = document.selectById("penalties-overview-section")
      penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

      penaltiesSection.hasCorrectOverviewCardLink(
        linkText = "Check Self Assessment penalties and appeals",
        linkHref = "/view-penalty/self-assessment/agent",
        exactHrefMatch = false
      )
      penaltiesSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = "View your late payment penalties",
        linkHref = "/view-penalty/self-assessment/agent#lppTab",
        exactHrefMatch = false
      )
      penaltiesSection.hasCorrectOverviewCardLink(
        cardIndex = 2,
        linkText = "View your late submission penalties",
        linkHref = "/view-penalty/self-assessment/agent#lspTab",
        exactHrefMatch = false
      )
    }
  }

  "New Home Overview page for Secondary Agents" should {
    "NOT display the 'Charges, credits and payments' section" in new TestSetup(isAgent = true, isSupportingAgent = true) {
      document.getOptionalSelector("#charges-overview-section") shouldBe None
    }

    "display the correct 'Deadlines and reporting' section" in new TestSetup(isAgent = true, isSupportingAgent = true) {
      val deadlinesSection: Element = document.selectById("deadlines-overview-section")
      deadlinesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Deadlines and reporting obligations"
      deadlinesSection.hasCorrectOverviewCardLink(
        linkText = "View updates and deadlines",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/submission-deadlines"
      )
      deadlinesSection.hasCorrectOverviewCardLink(
        cardIndex = 1,
        linkText = "Check your reporting obligations",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
      )
    }

    "display the correct 'Income sources' section" in new TestSetup(isAgent = true, isSupportingAgent = true) {
      val incomeSection: Element = document.selectById("income-overview-section")
      incomeSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Income sources"
      incomeSection.hasCorrectOverviewCardLink(
        linkText = "Add, manage or cease a business or income source",
        linkHref = "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
      )
    }

    "NOT display the 'Tax year summaries' section" in new TestSetup(isAgent = true, isSupportingAgent = true) {
      document.getOptionalSelector("#tax-year-overview-section") shouldBe None
    }

    "NOT display the 'Penalties and appeals' section" in new TestSetup(isAgent = true, isSupportingAgent = true) {
      document.getOptionalSelector("#penalties-overview-section") shouldBe None
    }
  }
}
