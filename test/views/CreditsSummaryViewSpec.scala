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

package views

import _root_.implicits.ImplicitCurrencyFormatter._
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.creditDetailModel.CreditDetailModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.helpers.CreditHistoryDataHelper
import testConstants.BaseTestConstants.testMtditid
import testConstants.FinancialDetailsTestConstants._
import testUtils.{TestSupport, ViewSpec}
import views.html.CreditsSummary

import java.net.URL


class CreditsSummaryViewSpec extends TestSupport with FeatureSwitching
  with ImplicitDateFormatter with ViewSpec with CreditHistoryDataHelper {

  val creditsSummaryView: CreditsSummary = app.injector.instanceOf[CreditsSummary]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testCalendarYear: Int = 2018
  val expectedDate: String = "29 Mar 2018"
  val utr: Option[String] = Some(testMtditid)
  val testMaybeBalanceDetails: Option[BigDecimal] = financialDetailCreditCharge.balanceDetails.availableCredit

  val creditsSummaryHeading: String = messages("credits.heading", s"$testCalendarYear")
  val creditsSummaryTitle: String = messages("htmlTitle", creditsSummaryHeading)
  val creditsSummaryTitleAgent: String = messages("htmlTitle.agent", creditsSummaryHeading)
  val creditsTableHeadDateText: String = messages("credits.tableHead.date")
  val creditsTableHeadTypeText: String = messages("credits.tableHead.type")
  val creditsTableHeadStatusText: String = messages("credits.tableHead.status")
  val creditsTableHeadAmountText: String = messages("credits.tableHead.amount")
  val creditsTableHeadTypeValue: String = messages("credits.tableHead.type.value")
  val creditsTableStatusFullyAllocatedValue: String = messages("credits.table.status-fully-allocated")
  val creditsTableStatusNotYetAllocatedValue: String = messages("credits.table.status-not-yet-allocated")
  val creditsTableStatusPartiallyAllocatedValue: String = messages("credits.table.status-partially-allocated")
  val creditAndRefundClaimRefundBtn: String = messages("credit-and-refund.claim-refund-btn")
  val getPageHelpLinkTextBtn: String = s"${messages("getpagehelp.linkText")} ${messages("pagehelp.opensInNewTabText")}"
  val creditsDropDownListCreditFomHmrcAdjustment: String = messages("credits.drop-down-list.credit-from-hmrc-adjustment")
  val creditsDropDownListCreditFomHmrcAdjustmentValue: String = messages("credits.drop-down-list.credit-from-hmrc-adjustment.value")
  val creditsDropDownListCreditFromAnEarlierTaxYear: String = messages("credits.drop-down-list.credit-from-an-earlier-tax-year")
  val saNoteMigratedIndividual: String = s"${messages("credits.drop-down-list.credit-from-an-earlier-tax-year.sa-note")} ${messages("credits.drop-down-list.sa-link")} ${messages("pagehelp.opensInNewTabText")}."
  val saNoteMigratedAgent: String = s"${messages("credits.drop-down-list.credit-from-an-earlier-tax-year.agent.sa-note")} ${messages("credits.drop-down-list.sa-link-agent")} ${messages("pagehelp.opensInNewTabText")}."
  val saNoteMigratedOnlineAccountLink: String = s"/self-assessment/ind/$testMtditid/account"
  val saNoteMigratedOnlineAccountAgentLink: String = s"https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
  val saNoteMigratedOnlineAccountLinkText: String = s"${messages("credits.drop-down-list.sa-link")} ${messages("pagehelp.opensInNewTabText")}"
  val saNoteMigratedOnlineAccountAgentLinkText: String = s"${messages("credits.drop-down-list.sa-link-agent")} ${messages("pagehelp.opensInNewTabText")}"
  val moneyInYourAccountHeading: String = messages("credits.money-in-your-account-section.name")
  val moneyInYourAccountAgentHeading: String = messages("credits.money-in-your-account-section.agent.name")
  val moneyInYourAccountMoneyClaimARefundLinkText: String = messages("credits.money-in-your-account-section.claim-a-refund-link")
  val moneyInYourAccountContent: String = s"""${messages("credits.money-in-your-account-section.content", s"${financialDetailCreditCharge.balanceDetails.availableCredit.get.toCurrencyString}")} ${moneyInYourAccountMoneyClaimARefundLinkText}."""
  val moneyInYourAccountAgentContent: String = s"""${messages("credits.money-in-your-account-section.agent.content", s"${financialDetailCreditCharge.balanceDetails.availableCredit.get.toCurrencyString}")} ${moneyInYourAccountMoneyClaimARefundLinkText}."""
  val moneyInYourAccountMoneyClaimARefundLink: String = "/report-quarterly/income-and-expenses/view/claim-refund"
  val moneyInYourAccountMoneyClaimARefundAgentLink: String = "/report-quarterly/income-and-expenses/view/agents/claim-refund"

  class TestSetup(creditCharges: List[CreditDetailModel] = List.empty,
              maybeAvailableCredit: Option[BigDecimal] = None,
              isAgent: Boolean = false,
              backUrl: String = "testString") {
    lazy val page: HtmlFormat.Appendable =
      creditsSummaryView(
        calendarYear = testCalendarYear,
        backUrl = backUrl,
        utr = utr,
        btaNavPartial = None,
        charges = creditCharges,
        maybeAvailableCredit = maybeAvailableCredit,
        isAgent = isAgent
      )(FakeRequest(), implicitly, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "display the Credits Summary page" when {
    "a user have multiple credits" in new TestSetup(creditCharges = List(
      creditDetailModelasCutOver,
      creditDetailModelasMfa,
      creditDetailModelasBCC,
      creditDetailModelasSetInterest
    ), maybeAvailableCredit = testMaybeBalanceDetails) {

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText


      document.select("td:nth-child(1)").get(0).text() shouldBe "25 Aug 2022"
      document.select("td:nth-child(2)").get(0).text() shouldBe "Credit from an earlier tax year"
      document.select("td:nth-child(3)").get(0).text() shouldBe creditsTableStatusPartiallyAllocatedValue
      document.select("td:nth-child(4)").get(0).text() shouldBe "£120.00"

      document.select("td:nth-child(1)").get(1).text() shouldBe "29 Mar 2022"
      document.select("td:nth-child(2)").get(1).text() shouldBe "Credit from HMRC adjustment"
      document.select("td:nth-child(3)").get(1).text() shouldBe creditsTableStatusNotYetAllocatedValue
      document.select("td:nth-child(4)").get(1).text() shouldBe "£150.00"

      document.select("td:nth-child(1)").get(2).text() shouldBe "29 Mar 2022"
      document.select("td:nth-child(2)").get(2).text() shouldBe "Credit from overpaid tax"
      document.select("td:nth-child(3)").get(2).text() shouldBe creditsTableStatusPartiallyAllocatedValue
      document.select("td:nth-child(4)").get(2).text() shouldBe "£150.00"

      document.select("td:nth-child(1)").get(3).text() shouldBe "29 Mar 2022"
      document.select("td:nth-child(2)").get(3).text() shouldBe "Credit from repayment interest"
      document.select("td:nth-child(3)").get(3).text() shouldBe creditsTableStatusNotYetAllocatedValue
      document.select("td:nth-child(4)").get(3).text() shouldBe "£250.00"

      document.selectById("h2-credit-from-hmrc-adjustment").text() shouldBe creditsDropDownListCreditFomHmrcAdjustment
      document.selectById("h2-credit-from-hmrc-adjustment").nextElementSibling().text() shouldBe creditsDropDownListCreditFomHmrcAdjustmentValue
      document.selectById("h2-credit-from-an-earlier-tax-year").text() shouldBe creditsDropDownListCreditFromAnEarlierTaxYear
      document.selectById("sa-note-migrated").text() shouldBe saNoteMigratedIndividual
      document.selectById("sa-note-migrated-online-account-link").text() shouldBe saNoteMigratedOnlineAccountLinkText
      new URL(document.selectById("sa-note-migrated-online-account-link").attr("href")).getPath shouldBe saNoteMigratedOnlineAccountLink

      document.selectById("h2-money-in-your-account").text() shouldBe moneyInYourAccountHeading
      document.selectById("p-money-in-your-account").text() shouldBe moneyInYourAccountContent
      document.selectById("money-in-your-account-claim-a-refund-link").text() shouldBe moneyInYourAccountMoneyClaimARefundLinkText
      document.selectById("money-in-your-account-claim-a-refund-link").attr("href") shouldBe moneyInYourAccountMoneyClaimARefundLink

      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Fully allocated" in new TestSetup(creditCharges = creditAndRefundCreditDetailListFullyAllocatedMFA) {

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusFullyAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£20.00"
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Not yet allocated" in new TestSetup(creditCharges = creditAndRefundCreditDetailListNotYetAllocatedMFA) {

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusNotYetAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£3,000.00"
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Partially allocated" in new TestSetup(creditCharges = creditAndRefundCreditDetailListPartiallyAllocatedMFA) {

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusPartiallyAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£1,000.00"
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has no credit" in new TestSetup(creditCharges = List.empty) {

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th").isEmpty shouldBe true
      document.select("td").isEmpty shouldBe true
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

  }


  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new TestSetup(creditCharges = creditAndRefundCreditDetailListMultipleChargesMFA, isAgent = true, maybeAvailableCredit = testMaybeBalanceDetails) {

        document.title() shouldBe creditsSummaryTitleAgent
        layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
        document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
        document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
        document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
        document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText

        document.selectById("balancing-charge-type-0").select("td:nth-child(1)").first().text() shouldBe "16 Apr 2018"
        document.selectById("balancing-charge-type-0").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
        document.selectById("balancing-charge-type-0").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusPartiallyAllocatedValue
        document.selectById("balancing-charge-type-0").select("td:nth-child(4)").first().text() shouldBe "£800.00"

        document.selectById("balancing-charge-type-1").select("td:nth-child(1)").first().text() shouldBe "30 Jul 2018"
        document.selectById("balancing-charge-type-1").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
        document.selectById("balancing-charge-type-1").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusNotYetAllocatedValue
        document.selectById("balancing-charge-type-1").select("td:nth-child(4)").first().text() shouldBe "£1,400.00"

        document.selectById("h2-credit-from-hmrc-adjustment").text() shouldBe creditsDropDownListCreditFomHmrcAdjustment
        document.selectById("h2-credit-from-hmrc-adjustment").nextElementSibling().text() shouldBe creditsDropDownListCreditFomHmrcAdjustmentValue
        document.selectById("h2-credit-from-an-earlier-tax-year").text() shouldBe creditsDropDownListCreditFromAnEarlierTaxYear
        document.selectById("sa-note-migrated-agent").text() shouldBe saNoteMigratedAgent
        document.selectById("sa-note-migrated-agent-online-account-link").text() shouldBe saNoteMigratedOnlineAccountAgentLinkText
        document.selectById("sa-note-migrated-agent-online-account-link").attr("href") shouldBe saNoteMigratedOnlineAccountAgentLink

        document.selectById("h2-money-in-your-account").text() shouldBe moneyInYourAccountAgentHeading
        document.selectById("p-money-in-your-account").text() shouldBe moneyInYourAccountAgentContent
        document.selectById("money-in-your-account-claim-a-refund-link").text() shouldBe moneyInYourAccountMoneyClaimARefundLinkText
        document.selectById("money-in-your-account-claim-a-refund-link").attr("href") shouldBe moneyInYourAccountMoneyClaimARefundAgentLink

        document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
      }
    }
  }
}
