/*
 * Copyright 2022 HM Revenue & Customs
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
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.CreditAndRefundConstants.{balanceDetailsModel, documentDetailWithDueDateFinancialDetailListModel, documentDetailWithDueDateFinancialDetailListModelMFA}
import testUtils.{TestSupport, ViewSpec}
import utils.CreditAndRefundUtils.UnallocatedCreditType
import utils.CreditAndRefundUtils.UnallocatedCreditType.{UnallocatedCreditFromOnePayment, UnallocatedCreditFromSingleCreditItem}
import views.html.CreditAndRefunds

import java.time.LocalDate


class CreditAndRefundsViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val creditAndRefundView: CreditAndRefunds = app.injector.instanceOf[CreditAndRefunds]
  val creditAndRefundHeading: String = messages("credit-and-refund.heading")
  val subHeadingWithCreditsPart1: String = messages("credit-and-refund.subHeading.has-credits-1")
  val subHeadingWithCreditsPart2: String = messages("credit-and-refund.subHeading.has-credits-2")
  val subHeadingWithUnallocatedCreditsOnePayment: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-one-payment-1")} £500.00 ${messages("credit-and-refund.subHeading.unallocated-credits-one-payment-2")}"
  val subHeadingWithUnallocatedCreditsSingleCredit: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-single-credit-1")} £500.00 ${messages("credit-and-refund.subHeading.unallocated-credits-single-credit-2")}"
  val paymentText: String = messages("credit-and-refund.payment")
  val claimBtn: String = messages("credit-and-refund.claim-refund-btn")
  val checkBtn: String = messages("credit-and-refund.check-refund-btn")
  val creditAndRefundHeadingWithTitleServiceNameGovUk: String = messages("titlePattern.serviceName.govUk", creditAndRefundHeading)
  val creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent: String = messages("htmlTitle.agent", creditAndRefundHeading)
  val creditAndRefundFromHMRCTitlePart1: String = messages("credit-and-refund.credit-from-hmrc-title-prt-1")
  val creditAndRefundFromHMRCTitlePart2: String = messages("credit-and-refund.credit-from-hmrc-title-prt-2")
  val creditAndRefundPaymentFromEarlierYearLinkText: String = messages("paymentHistory.paymentFromEarlierYear")
  val creditAndRefundAgentHasCredits2: String = messages("credit-and-refund.agent.subHeading.has-credits-2")
  val creditAndRefundAgentNoCredit: String = messages("credit-and-refund.agent.no-credit")
  val creditAndRefundAgentHasCreditBullet1Prt1: String = messages("credit-and-refund.agent.bullet-one-prt-1")
  val creditAndRefundAgentHasCreditBullet1Link: String = messages("credit-and-refund.agent.bullet-one-link")
  val creditAndRefundAgentHasCreditBullet1Prt2: String = messages("credit-and-refund.bullet-one-prt-2")
  val creditAndRefundAgentHasCreditBullet2Prt1: String = messages("credit-and-refund.bullet-two-prt-1")

  val link = "/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=1040000123"
  val linkCreditsSummaryPage = "/report-quarterly/income-and-expenses/view/credits-from-hmrc/2018"
  val linkPaymentMadeToHmrc = "/report-quarterly/income-and-expenses/view/agents/payment-made-to-hmrc?documentNumber=1040000123"
  class Setup(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)] = List(documentDetailWithDueDateFinancialDetailListModel()),
              balance: Option[BalanceDetails] = Some(balanceDetailsModel()),
              creditAndRefundType: Option[UnallocatedCreditType] = None,
              isAgent: Boolean = false,
              backUrl: String = "testString",
              isMFACreditsAndDebitsEnabled: Boolean = false) {
    lazy val page: HtmlFormat.Appendable =
      creditAndRefundView(
        creditCharges,
        balance,
        creditAndRefundType,
        isAgent = isAgent,
        backUrl,
        isMFACreditsAndDebitsEnabled = isMFACreditsAndDebitsEnabled
      )(FakeRequest(), implicitly, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "displaying individual credit and refund page" should {
    "display the page" when {
      "a user has requested a refund" in new Setup() {

        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        document.select("h2").first().select("span").text() shouldBe s"$subHeadingWithCreditsPart1 $subHeadingWithCreditsPart2"
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"
        document.select("p").get(2).select("a").attr("href") shouldBe link
        document.select("p").eachText().contains("Total") shouldBe false

        document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn
        document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe checkBtn
      }

      "a user has not requested a refund" in new Setup(balance = Some(balanceDetailsModel(None, None))) {

        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        document.select("h2").first().select("span").text() shouldBe s"$subHeadingWithCreditsPart1 $subHeadingWithCreditsPart2"
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"
        document.select("p").get(2).select("a").attr("href") shouldBe link
        document.select("p").eachText().contains("Total") shouldBe false

        document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn

      }

      "a user has a Refund claimed for full amount and claim is in a pending state" in
        new Setup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(Some(-6.00))),
          balance = Some(balanceDetailsModel(availableCredit = Some(0)))
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart2) shouldBe false
          document.select("p").get(2).text() shouldBe s"£6.00 $paymentText 15 May 2019"
          document.select("p").get(2).select("a").attr("href") shouldBe link
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe checkBtn
        }

      "a user has no available credit or current pending refunds" in
        new Setup(balance = None) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("p").last.text() shouldBe messages("credit-and-refund.no-credit")

          document.getElementsByClass("govuk-button").first().text() shouldBe checkBtn
        }

      "a user has a Credit from HMRC adjustment" in
        new Setup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA()),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart2) shouldBe false
          document.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $creditAndRefundFromHMRCTitlePart1 $creditAndRefundFromHMRCTitlePart2 0"
          document.select("p").get(2).select("a").attr("href") shouldBe linkCreditsSummaryPage
          document.select("p").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe checkBtn
        }

      "a user has a Multiple Credit from HMRC adjustment sorted in descending of credit" in
        new Setup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA(),
          documentDetailWithDueDateFinancialDetailListModelMFA(Some(-1000.0))),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart2) shouldBe false
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            s"£1,400.00 $creditAndRefundFromHMRCTitlePart1 $creditAndRefundFromHMRCTitlePart2 0"
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            s"£1,000.00 $creditAndRefundFromHMRCTitlePart1 $creditAndRefundFromHMRCTitlePart2 1"
          document.select("p").get(2).select("a").attr("href") shouldBe linkCreditsSummaryPage
          document.select("p").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe checkBtn
        }

      "a user has a multiple Refund claimed for full amount show sorted in descending of amount" in
        new Setup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(),
          documentDetailWithDueDateFinancialDetailListModel(Some(-1000.0))),
          balance = Some(balanceDetailsModel(availableCredit = Some(0)))
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart2) shouldBe false
          document.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $paymentText 15 May 2019"
          document.select("p").get(3).select("p:nth-child(1)").first().text() shouldBe
            s"£1,000.00 $paymentText 15 May 2019"
          document.select("p").get(2).select("a").attr("href") shouldBe link
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe checkBtn
        }

      "a user has an unallocated credits from exactly one payment" in
        new Setup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(Some(-500.00), dueDate = Some(LocalDate.of(2022, 1, 12)), originalAmount = Some(-1000))),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(500.00)
            )
          ),
          creditAndRefundType =  Some(UnallocatedCreditFromOnePayment)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").first().text() shouldBe subHeadingWithUnallocatedCreditsOnePayment
          document.select("h2").first().select("span").next().select("a").text() shouldBe "12 January 2022."
          document.select("h2").first().select("span").next().select("a").attr("href") shouldBe link
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
          document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn
          document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe checkBtn
        }

      "a user has an unallocated credits from exactly a single credit item" in
        new Setup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(
            Some(-500.00),
            dueDate = Some(LocalDate.of(2022, 1, 12)),
            originalAmount = Some(-1000),
            mainType = "ITSA Overpayment Relief"
          )
        ),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(12.00)
            )
          ),
          creditAndRefundType =  Some(UnallocatedCreditFromSingleCreditItem)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").first().text() shouldBe subHeadingWithUnallocatedCreditsSingleCredit
          document.select("h2").first().select("span").next().select("a").text() shouldBe s"$creditAndRefundFromHMRCTitlePart2."
          document.select("h2").first().select("span").next().select("a").attr("href") shouldBe linkCreditsSummaryPage
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn
          document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe checkBtn
        }

      "a user has an unallocated credits from exactly a single credit item as a cut over credit" in
        new Setup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(
            Some(-500.00),
            dueDate = Some(LocalDate.of(2022, 1, 12)),
            originalAmount = Some(-1000),
            mainType = "ITSA Cutover Credits"
          )
        ),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(12.00)
            )
          ),
          creditAndRefundType =  Some(UnallocatedCreditFromSingleCreditItem)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("h2").first().select("span").first().text() shouldBe subHeadingWithUnallocatedCreditsSingleCredit
          document.select("h2").first().select("span").next().select("a").text() shouldBe s"$creditAndRefundPaymentFromEarlierYearLinkText."
          document.select("h2").first().select("span").next().select("a").attr("href") shouldBe linkCreditsSummaryPage
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn
          document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe checkBtn
        }
    }
  }

  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new Setup(isAgent = true) {

        document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"
        document.select("p").get(2).select("a").attr("href") shouldBe linkPaymentMadeToHmrc

        document.getElementsByClass("govuk-button").first().text() shouldBe claimBtn
        document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe checkBtn
        document.getElementsByClass("govuk-heading-s").first().text() should include(creditAndRefundAgentHasCredits2)
        document.select("p").get(7).text() shouldBe (creditAndRefundAgentHasCreditBullet1Prt1 + " " + creditAndRefundAgentHasCreditBullet1Link + " " + creditAndRefundAgentHasCreditBullet1Prt2)
      }
      "custom account has no credit" in new Setup(isAgent = true, balance = None) {

        document.getElementsByClass("govuk-body").first().text() shouldBe creditAndRefundAgentNoCredit
      }
    }
  }
}
