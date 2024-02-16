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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.creditsandrefunds.CreditAndRefundViewModel
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
import testConstants.BaseTestConstants.testTaxYearTo

import java.time.LocalDate


class CreditAndRefundsViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val creditAndRefundView: CreditAndRefunds = app.injector.instanceOf[CreditAndRefunds]
  val creditAndRefundHeading: String = messages("credit-and-refund.heading")
  val subHeadingWithCreditsPart1: String = messages("credit-and-refund.subHeading.has-credits-1")
  val subHeadingWithCreditsPart3: String = messages("credit-and-refund.subHeading.has-credits-2")
  val subHeadingWithCreditsPart2: String = messages("credit-and-refund.subHeading.has-credits-3")
  val subHeadingWithCreditsPart2Agent: String = messages("credit-and-refund.agent.subHeading.has-credits-2")
  val subHeadingWithCreditsPart3Agent: String = messages("credit-and-refund.agent.subHeading.has-credits-3")
  val subHeadingWithUnallocatedCreditsOnePayment: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-one-payment-1")} £500.00 ${messages("credit-and-refund.subHeading.unallocated-credits-one-payment-2")}"
  val subHeadingWithUnallocatedCreditsSingleCredit: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-single-credit-1")} £500.00 ${messages("credit-and-refund.subHeading.unallocated-credits-single-credit-2")}"
  val subHeadingWithUnallocatedCreditsOnePaymentAgent: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-one-payment-1")} £500.00 ${messages("credit-and-refund.agent.unallocated-credits-one-payment-2")}"
  val subHeadingWithUnallocatedCreditsSingleCreditAgent: String = s"${messages("credit-and-refund.subHeading.unallocated-credits-single-credit-1")} £500.00 ${messages("credit-and-refund.agent.unallocated-credits-single-credit-2")}"
  val paymentText: String = messages("credit-and-refund.payment")
  val claimBtn: String = messages("credit-and-refund.claim-refund-btn")
  val creditAndRefundHeadingWithTitleServiceNameGovUk: String = messages("htmlTitle", creditAndRefundHeading)
  val creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent: String = messages("htmlTitle.agent", creditAndRefundHeading)
  val creditAndRefundFromHMRCTitlePart1: String = messages("credit-and-refund.credit-from-adjustment-prt-1")
  val creditAndRefundFromHMRCTitlePart2: String = messages("credit-and-refund.credit-from-hmrc-title")
  val creditAndRefundFromBalancingChargePart1: String = messages("credit-and-refund.credit-from-balancing-charge-prt-1")
  val creditAndRefundFromBalancingChargePart2: String = messages("credit-and-refund.credit-from-balancing-charge-prt-2")
  val creditAndRefundPaymentFromEarlierYearLinkText: String = messages("credit-and-refund.credit-from-earlier-tax-year")
  val creditAndRefundAgentNoCredit: String = messages("credit-and-refund.agent.no-credit")
  val creditAndRefundAgentHasCreditBullet1Prt1: String = messages("credit-and-refund.agent.bullet-one-prt-1")
  val creditAndRefundAgentHasCreditBullet1Link: String = messages("credit-and-refund.agent.bullet-one-link")
  val creditAndRefundAgentHasCreditBullet1Prt2: String = messages("credit-and-refund.bullet-one-prt-2")
  val creditAndRefundAgentHasCreditBullet2Prt1: String = messages("credit-and-refund.bullet-two-prt-1")
  val creditAndRefundsPaymentToHMRC: String = messages("credit-and-refund.payment")
  val creditAndRefundFromAdjustment: String = messages("credit-and-refund.credit-from-adjustment-prt-1")

  val linkPaymentMadeToHmrc = "/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=1040000123"
  val linkCreditsSummaryPage = "/report-quarterly/income-and-expenses/view/credits-from-hmrc/2018"
  val linkCreditsSummaryPageMFAPreviousYear = "/report-quarterly/income-and-expenses/view/credits-from-hmrc/2017"
  val linkAgentCreditsSummaryPage = "/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"
  val linkAgentPaymentMadeToHmrc = "/report-quarterly/income-and-expenses/view/agents/payment-made-to-hmrc?documentNumber=1040000123"

  class TestSetup(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)] = List(documentDetailWithDueDateFinancialDetailListModel()),
              balance: Option[BalanceDetails] = Some(balanceDetailsModel()),
              creditAndRefundType: Option[UnallocatedCreditType] = None,
              isAgent: Boolean = false,
              backUrl: String = "testString",
              isMFACreditsAndDebitsEnabled: Boolean = false,
              isCutOverCreditsEnabled: Boolean = false) {
    lazy val page: HtmlFormat.Appendable =
      creditAndRefundView(
        creditCharges,
        CreditAndRefundViewModel(creditCharges),
        balance,
        creditAndRefundType,
        isAgent = isAgent,
        backUrl,
        isMFACreditsAndDebitsEnabled = isMFACreditsAndDebitsEnabled,
        isCutOverCreditsEnabled = isCutOverCreditsEnabled
      )(FakeRequest(), implicitly, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "displaying individual credit and refund page" should {
    "display the page" when {
      "a user has requested a refund" in new TestSetup(isCutOverCreditsEnabled = true) {

        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.select("h2").first().text() shouldBe s"$subHeadingWithCreditsPart1 £7.00 $subHeadingWithCreditsPart3"
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"
        document.select("p").eachText().contains("Total") shouldBe false

        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
      }

      "a user has not requested a refund" in new TestSetup(balance = Some(balanceDetailsModel(None, None)), isCutOverCreditsEnabled = true) {

        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.select("h2").first().text() shouldBe s"$subHeadingWithCreditsPart1 £7.00 $subHeadingWithCreditsPart2"
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"
        document.select("p").eachText().contains("Total") shouldBe false

        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn

      }

      "a user has a Refund claimed for full amount and claim is in a pending state" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(Some(-6.00))),
          balance = Some(balanceDetailsModel(availableCredit = Some(0))),
          isCutOverCreditsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("h2").first().text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart3) shouldBe false
          document.select("p").get(2).text() shouldBe s"£6.00 $paymentText 15 May 2019"
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has no available credit or current pending refunds" in
        new TestSetup(balance = None) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("p").last.text() shouldBe messages("credit-and-refund.no-credit")

        }

      "a user has a Credit from HMRC adjustment" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA()),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("h2").first().text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart3) shouldBe false
          document.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $creditAndRefundFromHMRCTitlePart1" + " " + testTaxYearTo
          document.select("p").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has a Credit from HMRC adjustment for the previous taxYear" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA(2017)),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("h2").first().text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart3) shouldBe false
          document.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $creditAndRefundFromHMRCTitlePart1" + " " + testTaxYearTo
          document.select("p").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has a Multiple Credit from HMRC adjustment sorted in descending of credit and tax year" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA(),
          documentDetailWithDueDateFinancialDetailListModelMFA(outstandingAmount = Some(-1000.0))),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          Console.println(document)
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("h2").first().text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart2) shouldBe false
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            s"£1,400.00 $creditAndRefundFromHMRCTitlePart1" + " " + testTaxYearTo
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            s"£1,000.00 $creditAndRefundFromHMRCTitlePart1" + " " + testTaxYearTo
          document.select("p").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has a multiple Refund claimed for full amount show sorted in descending of amount" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(),
          documentDetailWithDueDateFinancialDetailListModel(Some(-1000.0))),
          balance = Some(balanceDetailsModel(availableCredit = Some(0))),
          isCutOverCreditsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("h2").first().text().contains(subHeadingWithCreditsPart1 + subHeadingWithCreditsPart3) shouldBe false
          document.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $paymentText 15 May 2019"
          document.select("p").get(3).select("p:nth-child(1)").first().text() shouldBe
            s"£1,000.00 $paymentText 15 May 2019"
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has an unallocated credits from exactly one payment" in
        new TestSetup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(Some(-500.00), dueDate = Some(LocalDate.of(2022, 1, 12)), originalAmount = Some(-1000))),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(500.00)
            )
          ),
          creditAndRefundType = Some(UnallocatedCreditFromOnePayment)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-payment").text() shouldBe "£500.00 "  + creditAndRefundsPaymentToHMRC + " 12 January 2022"
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }

      "a user has an unallocated credits from exactly a single credit item (MFA Credit)" in
        new TestSetup(creditCharges = List(
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00 "  + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }

      "a user has an unallocated credits from exactly a single credit item (cut over credit)" in
        new TestSetup(creditCharges = List(
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00" + " " + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }
      "a user has an unallocated credits from exactly a single credit item (Balancing Charge Credit)" in
        new TestSetup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(
            Some(-500.00),
            dueDate = Some(LocalDate.of(2022, 1, 12)),
            originalAmount = Some(-1000),
            mainType = "SA Balancing Charge Credit"
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00" + " " + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }
    }
  }

  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new TestSetup(isAgent = true, isCutOverCreditsEnabled = true) {

        document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        document.select("p").get(2).text() shouldBe s"£1,400.00 $paymentText 15 May 2019"

        document.select("#main-content .govuk-button").isEmpty shouldBe true
        document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true
        layoutContent.select("h2").first().text() shouldBe s"$subHeadingWithCreditsPart1 £7.00 $subHeadingWithCreditsPart2Agent"
        document.select("p").get(7).text() shouldBe (creditAndRefundAgentHasCreditBullet1Prt1 + " " + creditAndRefundAgentHasCreditBullet1Link + " " + creditAndRefundAgentHasCreditBullet1Prt2)
      }
      "custom account has no credit" in new TestSetup(isAgent = true, balance = None) {

        document.getElementsByClass("govuk-body").first().text() shouldBe creditAndRefundAgentNoCredit
      }

      "a client has an unallocated credits from exactly one payment" in
        new TestSetup(isAgent = true, creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(Some(-500.00), dueDate = Some(LocalDate.of(2022, 1, 12)), originalAmount = Some(-1000))),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(500.00)
            )
          ),
          creditAndRefundType = Some(UnallocatedCreditFromOnePayment)
        ) {
          document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-payment").text() shouldBe "£500.00 " + creditAndRefundsPaymentToHMRC + " 12 January 2022"
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
          document.select("#main-content .govuk-button").isEmpty shouldBe true
          document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true
        }

      "a client has an unallocated credits from exactly a single credit item (MFA Credit)" in
        new TestSetup(isAgent = true, creditCharges = List(
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {
          document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00 " + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").isEmpty shouldBe true
          document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true
        }

      "a client has an unallocated credits from a single credit item (cut over credit)" in
        new TestSetup(isAgent = true, creditCharges = List(
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {
          document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00 " + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").isEmpty shouldBe true
          document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true
        }
      "a client has an unallocated credits from a single credit item (Balancing charge credit)" in
        new TestSetup(isAgent = true, creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(
            Some(-500.00),
            dueDate = Some(LocalDate.of(2022, 1, 12)),
            originalAmount = Some(-1000),
            mainType = "SA Balancing Charge Credit"
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
          creditAndRefundType = Some(UnallocatedCreditFromSingleCreditItem)
        ) {
          document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectById("unallocated-single-credit").text() shouldBe "£500.00 " + creditAndRefundFromAdjustment + " " + testTaxYearTo
          document.select("dt").eachText().contains("Total") shouldBe false
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

          document.select("#main-content .govuk-button").isEmpty shouldBe true
          document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true
        }
    }
  }
}
