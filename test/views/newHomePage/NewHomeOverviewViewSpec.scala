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

package views.newHomePage

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testUtils.{TestSupport, ViewSpec}
import views.html.newHomePage.NewHomeOverviewView

import java.time.{LocalDate, Month}


class NewHomeOverviewViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val newHomeOverviewView: NewHomeOverviewView = app.injector.instanceOf[NewHomeOverviewView]
  val testTaxYear: TaxYear = fixedTaxYear
  val nextPaymentYear: String = "2019"
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)

  def financialDetails(transactionType: String*) = {
    transactionType.map( transaction =>{
      FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
        documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
          documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
        financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
          mainTransaction = Some(transaction), transactionId = Some("testId"),
          items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate)))))))
      }
    ).toList
  }


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
                   moneyInYourAccount: Boolean = false,
                   ctaViewModel: WYOClaimToAdjustViewModel = WYOClaimToAdjustViewModel(poaTaxYear = Some(TaxYear(2025,2026))),
                   welshLang: Boolean = false,
                   chargeItems: List[ChargeItem] = List.empty,
                   useRebrand: Boolean = false) {

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
        noChargesToPay,
        moneyInYourAccount,
        ctaViewModel,
        chargeItems,
        useRebrand,
        false,
        true,
        isRecentActivityEnabled
      )(testMessages, appConfig, FakeRequest())
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }


  "New Home Overview page for Individuals" should {

    "display the correct heading" when {
      "useRebrand is false" in new TestSetup() {
        document.getElementById("income-tax-heading").text() shouldBe "Income Tax"
      }
      "useRebrand is true" in new TestSetup(useRebrand = true) {
        document.getElementById("income-tax-heading").text() shouldBe "Self Assessment"
      }
    }

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

    "display the the correct content for a user with charges to pay and no POA to adjust" in new TestSetup(ctaViewModel=WYOClaimToAdjustViewModel(None)) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "Check what you owe and make a payment"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/what-you-owe")

      document.select(".govuk-summary-card-no-border").get(1).text() shouldBe "View payment, credit and refund history"
      document.select(".govuk-summary-card-no-border").get(1).hasCorrectHref("/report-quarterly/income-and-expenses/view/payment-refund-history")

      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/money-in-your-account")

      document.select("h2.govuk-heading-m").get(1).text() shouldBe "Deadlines and reporting obligations"
      document.select(".govuk-summary-card-no-border").get(3).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(3).hasCorrectHref("/report-quarterly/income-and-expenses/view/submission-deadlines")

      document.select(".govuk-summary-card-no-border").get(4).text() shouldBe "Check your reporting obligations"
      document.select(".govuk-summary-card-no-border").get(4).hasCorrectHref("/report-quarterly/income-and-expenses/view/reporting-frequency")

      document.select("h2.govuk-heading-m").get(2).text() shouldBe "Income sources"
      document.select(".govuk-summary-card-no-border").get(5).text() shouldBe "Add, manage or cease a business or income source"
      document.select(".govuk-summary-card-no-border").get(5).hasCorrectHref("/report-quarterly/income-and-expenses/view/manage-your-businesses")

      document.select("h2.govuk-heading-m").get(3).text() shouldBe "Tax year summaries"
      document.select(".govuk-summary-card-no-border").get(6).text() shouldBe "View all tax years"
      document.select(".govuk-summary-card-no-border").get(6).hasCorrectHref("/report-quarterly/income-and-expenses/view/tax-years")

      document.select(".govuk-summary-card-no-border").get(7).text() shouldBe s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast"
      document.select(".govuk-summary-card-no-border").get(7).hasCorrectHref(s"/report-quarterly/income-and-expenses/view/tax-year-summary/${testTaxYear.endYear}")

      document.select("h2.govuk-heading-m").get(4).text() shouldBe "Penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(8).text() shouldBe "Check Self Assessment penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(8).link.attr("href") should include("/view-penalty/self-assessment")
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

    "display the correct text when user does not have LSP or LPP" in new TestSetup() {
      val penaltiesSection: Element = document.selectById("penalties-overview-section")
      penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

      penaltiesSection.hasCorrectOverviewCardLink(
        linkText = "Check Self Assessment penalties and appeals",
        linkHref = "/view-penalty/self-assessment",
        exactHrefMatch = false
      )
    }

    "display the correct text when user has a LSP" in {
      val items = financialDetails("4027").flatMap(_.asChargeItems)
      new TestSetup(chargeItems = items) {
        val penaltiesSection: Element = document.selectById("penalties-overview-section")
        penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

        penaltiesSection.hasCorrectOverviewCardLink(
          linkText = "View your late submission penalties",
          linkHref = "/view-penalty/self-assessment#lspTab",
          exactHrefMatch = false
        )
      }
    }

    "display the correct text when user has a LPP" in {
      val items = financialDetails("4028").flatMap(_.asChargeItems)
      new TestSetup(chargeItems = items) {
        val penaltiesSection: Element = document.selectById("penalties-overview-section")
        penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

        penaltiesSection.hasCorrectOverviewCardLink(
          linkText = "View your late payment penalties",
          linkHref = "/view-penalty/self-assessment#lppTab",
          exactHrefMatch = false
        )
      }
    }

    "display the correct text when user has a both LSP & LPP" in {
      val items = financialDetails("4027", "4028").flatMap(_.asChargeItems)
      new TestSetup(chargeItems = items) {
        val penaltiesSection: Element = document.selectById("penalties-overview-section")
        penaltiesSection.select("h2.govuk-heading-m").get(0).text() shouldBe "Penalties and appeals"

        penaltiesSection.hasCorrectOverviewCardLink(
          linkText = "View your late payment penalties",
          linkHref = "/view-penalty/self-assessment#lppTab",
          exactHrefMatch = false
        )

        penaltiesSection.hasCorrectOverviewCardLink(
          cardIndex = 1,
          linkText = "View your late submission penalties",
          linkHref = "/view-penalty/self-assessment#lspTab",
          exactHrefMatch = false
        )
      }
    }

    "display the correct content for a user with money in their account" in new TestSetup(moneyInYourAccount = true) {
      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account and claim a refund"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/money-in-your-account")
    }

    "display the correct content for a user with NO money in their account" in new TestSetup(moneyInYourAccount = false) {
      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/money-in-your-account")
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

    "display the the correct content for a user with charges to pay and no POA to adjust" in new TestSetup(isAgent = true,ctaViewModel=WYOClaimToAdjustViewModel(None)) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Charges, payments, credits and refunds"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "Check what you owe and make a payment"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/what-your-client-owes")

      document.select(".govuk-summary-card-no-border").get(1).text() shouldBe "View payment, credit and refund history"
      document.select(".govuk-summary-card-no-border").get(1).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/payment-refund-history")

      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/money-in-your-account")

      document.select("h2.govuk-heading-m").get(1).text() shouldBe "Deadlines and reporting obligations"
      document.select(".govuk-summary-card-no-border").get(3).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(3).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/submission-deadlines")

      document.select(".govuk-summary-card-no-border").get(4).text() shouldBe "Check your reporting obligations"
      document.select(".govuk-summary-card-no-border").get(4).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/reporting-frequency")

      document.select("h2.govuk-heading-m").get(2).text() shouldBe "Income sources"
      document.select(".govuk-summary-card-no-border").get(5).text() shouldBe "Add, manage or cease a business or income source"
      document.select(".govuk-summary-card-no-border").get(5).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/manage-your-businesses")

      document.select("h2.govuk-heading-m").get(3).text() shouldBe "Tax year summaries"
      document.select(".govuk-summary-card-no-border").get(6).text() shouldBe "View all tax years"
      document.select(".govuk-summary-card-no-border").get(6).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/tax-years")

      document.select(".govuk-summary-card-no-border").get(7).text() shouldBe s"View your ${testTaxYear.startYear}-${testTaxYear.endYear} tax calculation and forecast"
      document.select(".govuk-summary-card-no-border").get(7).hasCorrectHref(s"/report-quarterly/income-and-expenses/view/agents/tax-year-summary/${testTaxYear.endYear}")

      document.select("h2.govuk-heading-m").get(4).text() shouldBe "Penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(8).text() shouldBe "Check Self Assessment penalties and appeals"
      document.select(".govuk-summary-card-no-border").get(8).link.attr("href") should include("/view-penalty/self-assessment/agent")
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
    }

    "display the correct content for a user with money in their account" in new TestSetup(isAgent = true, moneyInYourAccount = true) {
      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account and claim a refund"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/money-in-your-account")
    }

    "display the correct content for a user with NO money in their account" in new TestSetup(isAgent = true, moneyInYourAccount = false) {
      document.select(".govuk-summary-card-no-border").get(2).text() shouldBe "Check for money in your account"
      document.select(".govuk-summary-card-no-border").get(2).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/money-in-your-account")
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
