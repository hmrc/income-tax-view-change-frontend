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
import testConstants.BaseTestConstants.testTaxYearTo
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

  val paymentText: String = messages("credit-and-refund.payment")
  val claimBtn: String = messages("credit-and-refund.claim-refund-btn")
  val creditAndRefundHeadingWithTitleServiceNameGovUk: String = messages("htmlTitle", creditAndRefundHeading)
  val creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent: String = messages("htmlTitle.agent", creditAndRefundHeading)
  val creditAndRefundFromHMRCTitlePart1: String = messages("credit-and-refund.credit-from-adjustment-prt-1")
  val creditAndRefundFromHMRCTitlePart2: String = messages("credit-and-refund.credit-from-hmrc-title")
  val creditAndRefundPaymentFromEarlierYearLinkText: String = messages("credit-and-refund.credit-from-earlie r-tax-year")
  val creditAndRefundAgentNoCredit: String = messages("credit-and-refund.agent.no-credit")
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
        layoutContent.selectFirst("p").text shouldBe "£7.00 available to claim"
        layoutContent.select("p").get(1).text() shouldBe "£1,400.00 from a payment you made to HMRC on 15 May 2019"
        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
      }

      "a user has not requested a refund" in new TestSetup(balance = Some(balanceDetailsModel(None, None)), isCutOverCreditsEnabled = true) {
        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.selectFirst("p").text shouldBe "£7.00 available to claim"
        layoutContent.select("p").get(1).text() shouldBe "£1,400.00 from a payment you made to HMRC on 15 May 2019"
        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
      }

      "a user has a Refund claimed for full amount and claim is in a pending state" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(Some(-6.00))),
          balance = Some(balanceDetailsModel(
            availableCredit = Some(0),
            firstPendingAmountRequested = Some(6.00),
            secondPendingAmountRequested = None)),
          isCutOverCreditsEnabled = true
        ) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£0.00 available to claim"
          layoutContent.select("p").get(1).text() shouldBe "£6.00 from a payment you made to HMRC on 15 May 2019"
          layoutContent.select("p").get(2).text() shouldBe "£6.00 is a refund currently in progress"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
        }

      "a user has no available credit or current pending refunds" in
        new TestSetup(balance = None) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("p").last.text() shouldBe "You have no money in your account."
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
          layoutContent.select("p").get(1).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 credit from HMRC adjustment - 2017 to 2018 tax year"
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
          layoutContent.select("p").get(1).select("p:nth-child(1)").first().text() shouldBe
            "£1,400.00 credit from HMRC adjustment - 2017 to 2018 tax year"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
        }

      "a user has a Multiple Credit from HMRC adjustment sorted in tax year" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModelMFA(),
          documentDetailWithDueDateFinancialDetailListModelMFA(outstandingAmount = Some(-1000.0))),
          balance = Some(balanceDetailsModel(
            firstPendingAmountRequested = Some(4.50),
            secondPendingAmountRequested = None,
            availableCredit = Some(0))),
          isMFACreditsAndDebitsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            "£1,000.00 credit from HMRC adjustment - 2017 to 2018 tax year"
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            "£1,400.00 credit from HMRC adjustment - 2017 to 2018 tax year"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
        }

      "a user has a multiple Refund claimed for full amount" in
        new TestSetup(creditCharges = List(documentDetailWithDueDateFinancialDetailListModel(),
          documentDetailWithDueDateFinancialDetailListModel(Some(-1000.0))),
          balance = Some(balanceDetailsModel(availableCredit = Some(0))),
          isCutOverCreditsEnabled = true
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.select("p").get(1).select("p:nth-child(1)").first().text() shouldBe
            s"£1,000.00 $paymentText 15 May 2019"
          layoutContent.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $paymentText 15 May 2019"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true

        }

      "a user has an unallocated credits from exactly one payment" in
        new TestSetup(creditCharges = List(
          documentDetailWithDueDateFinancialDetailListModel(Some(-500.00), dueDate = Some(LocalDate.of(2022, 1, 12)), originalAmount = Some(-1000))),
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None
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

      "there is no allocated credit" in new TestSetup(
        balance = Some(
          balanceDetailsModel(
            availableCredit = Some(500.00),
            allocatedCredit = Some(0),
            firstPendingAmountRequested = None,
            secondPendingAmountRequested = None,
          )
        )) {
        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.selectFirst("p").text shouldBe "£500.00 available to claim"
        layoutContent.select("p").get(2).text shouldBe
          "The most you can claim back is £500.00. This amount does not include any refunds that may already be in progress."
        layoutContent.select("p").get(3).text shouldBe
          "Money that you do not claim back can be used automatically by HMRC to cover your future tax bills when they become due."
        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
      }

      "there is allocated credit" which {

        "is more than available credit" in new TestSetup(
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              allocatedCredit = Some(600.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
            )
          )) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£500.00 available to claim"
          layoutContent.select("p").get(2).text shouldBe
            "HMRC has reserved £600.00 of this to cover your upcoming tax bill. Check what you owe for further information."
          layoutContent.selectFirst(".govuk-inset-text").text shouldBe
            "If you claim any of this money, you will need to pay it back to HMRC to settle your upcoming tax bill."
          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }


        "is less than available credit" in new TestSetup(
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              allocatedCredit = Some(100.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
            )
          )) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£500.00 available to claim"
          layoutContent.select("p").get(2).text shouldBe
            "HMRC has reserved £100.00 of this to cover your upcoming tax bill. Check what you owe for further information."
          layoutContent.selectFirst(".govuk-inset-text").text shouldBe
           "If you claim back more than £400.00, you will need to make another payment to HMRC to settle your upcoming tax bill."
          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }

        "is equal to available credit" in new TestSetup(
          balance = Some(
            balanceDetailsModel(
              availableCredit = Some(500.00),
              allocatedCredit = Some(500.00),
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
            )
          )) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£500.00 available to claim"
          layoutContent.select("p").get(2).text shouldBe
            "HMRC has reserved all £500.00 to cover your upcoming tax bill. Check what you owe for further information."
          layoutContent.selectFirst(".govuk-inset-text").text shouldBe
            "If you claim any of this money, you will need to pay it back to HMRC to settle your upcoming tax bill."
          document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
        }
      }
    }
  }

  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new TestSetup(isAgent = true, isCutOverCreditsEnabled = true, balance = Some(
        balanceDetailsModel(
          availableCredit = Some(500.00),
          allocatedCredit = Some(0),
          firstPendingAmountRequested = None,
          secondPendingAmountRequested = None
        )
      )) {
        document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.selectFirst("p").text shouldBe "£500.00 available to claim"
        layoutContent.select("p").get(2).text shouldBe
          "The most you can claim back is £500.00. This amount does not include any refunds that may already be in progress."
        layoutContent.select("p").get(3).text shouldBe
          "Money that you do not claim back can be used automatically by HMRC to cover your future tax bills when they become due."
        document.select("#main-content .govuk-button").isEmpty shouldBe true
      }
    }
  }
}
