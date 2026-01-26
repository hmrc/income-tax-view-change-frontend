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
import models.creditsandrefunds.{MoneyInYourAccountViewModel, CreditsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.ANewCreditAndRefundModel
import testUtils.{TestSupport, ViewSpec}
import views.html.CreditAndRefundsView

import java.time.LocalDate


class CreditAndRefundsViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val creditAndRefundView: CreditAndRefundsView = app.injector.instanceOf[CreditAndRefundsView]
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

  class TestSetup(
                   creditAndRefundModel: CreditsModel,
                   isAgent: Boolean = false,
                   backUrl: String = "testString",
                   welshLang: Boolean  = false) {

    val testMessages: Messages = if(welshLang) {
      app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
    } else { messages }

    val viewModel: MoneyInYourAccountViewModel = MoneyInYourAccountViewModel.fromCreditsModel(creditAndRefundModel)

    lazy val page: HtmlFormat.Appendable =
      creditAndRefundView(
        viewModel,
        isAgent = isAgent,
        backUrl)(FakeRequest(), implicitly, testMessages)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "displaying individual credit and refund page" should {

    "display bulleted list" when {

      "there are multiple credits or payments" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(4000.0)
          .withMfaCredit(LocalDate.of(2019, 5, 15), -1400.0)
          .withPayment(LocalDate.of(2019, 5, 15), -1400.0)
          .withPoaOneReconciliationCredit(LocalDate.of(2019, 5, 15), -1100)
          .withPoaTwoReconciliationCredit(LocalDate.of(2019, 5, 15), -1100)
          .get()) {
        document.select("ul#credits-list").isDefined shouldBe true
      }

      "there are multiple refund requests" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(700.0)
          .withFirstRefund(700.0)
          .withSecondRefund(700.0)
          .get()) {
        document.select("ul#credits-list").isDefined shouldBe true
      }

      "there is a single credit and single refund request" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(700.0)
          .withPayment(LocalDate.of(2019, 5, 15), 1400.0)
          .withFirstRefund(700.0)
          .get()) {
        document.select("ul#credits-list").isDefined shouldBe true
      }
    }

    "not display bulleted list" when {

      "there is a single credit" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(2800.0)
          .withMfaCredit(LocalDate.of(2019, 5, 15), 1400.0)
          .get()) {
        document.select("ul#credits-list").isEmpty shouldBe true
      }

      "there is a single payment" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(700.0)
          .withPayment(LocalDate.of(2019, 5, 15), 1400.0)
          .get()) {
        document.select("ul#credits-list").isEmpty shouldBe true
      }

      "there is a single refund request" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(700.0)
          .withFirstRefund(700.0)
          .get()) {
        document.select("ul#credits-list").isEmpty shouldBe true
      }
    }

    "display the page" when {

      "there is a refund for full amount" in
        new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel()
            .withFirstRefund(6.0)
            .withPayment(LocalDate.of(2019, 5, 15), 6.0)
            .withAvailableCredit(0.0)
            .get()
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£0.00 is in your account"
          layoutContent.select("p").get(1).text() shouldBe "£6.00 from a payment you made to HMRC on 15 May 2019"
          layoutContent.select("p").get(2).text() shouldBe "£6.00 is a refund currently in progress"
          layoutContent.select("#credits-list p").size() shouldBe 2
        }

      "there is no available credit or refunds" in
        new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel().get(),
        ) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("p").last.text() shouldBe "You have no money in your account."
        }

      "there is available credit but no allocated credit" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(500.0)
          .withTotalCredit(500.0)
          .get()) {
        document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.selectFirst("p").text shouldBe "£500.00 is in your account"
        layoutContent.select("p").get(1).text shouldBe
          "The most you can claim back is £500.00. This amount does not include any refunds that may already be in progress."
        layoutContent.select("p").get(2).text shouldBe
          "Money that you do not claim back can be used automatically by HMRC to cover your future tax bills when they become due."
        document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
      }

        "there is allocated credit" which {

          "is more than available credit" in new TestSetup(
            creditAndRefundModel = ANewCreditAndRefundModel()
              .withAvailableCredit(500.0)
              .withTotalCredit(500.0)
              .withAllocatedFutureCredit(600.0)
              .get()
          ) {
            document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
            layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
            layoutContent.selectFirst("p").text shouldBe "£500.00 is in your account"
            layoutContent.select("p").get(1).text shouldBe
              "HMRC has reserved £600.00 of this to cover your upcoming tax bill. Check what you owe for further information."
            layoutContent.selectFirst(".govuk-inset-text").text shouldBe
              "If you claim any of this money, you will need to pay it back to HMRC to settle your upcoming tax bill."
            document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
          }

          "is less than available credit" in new TestSetup(
            creditAndRefundModel = ANewCreditAndRefundModel()
              .withAvailableCredit(500.0)
              .withTotalCredit(500.0)
              .withAllocatedFutureCredit(100.0)
              .withUnallocatedCredit(400.0)
              .get()) {
            document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
            layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
            layoutContent.selectFirst("p").text shouldBe "£500.00 is in your account"
            layoutContent.select("p").get(1).text shouldBe
              "HMRC has reserved £100.00 of this to cover your upcoming tax bill. Check what you owe for further information."
            layoutContent.selectFirst(".govuk-inset-text").text shouldBe
              "If you claim back more than £400.00, you will need to make another payment to HMRC to settle your upcoming tax bill."
            document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
          }

          "is equal to available credit" in new TestSetup(
            creditAndRefundModel = ANewCreditAndRefundModel()
              .withAvailableCredit(500.0)
              .withTotalCredit(500.0)
              .withAllocatedFutureCredit(500.0)
              .get()
          ) {
            document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
            layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
            layoutContent.selectFirst("p").text shouldBe "£500.00 is in your account"
            layoutContent.select("p").get(1).text shouldBe
              "HMRC has reserved all £500.00 to cover your upcoming tax bill. Check what you owe for further information."
            layoutContent.selectFirst(".govuk-inset-text").text shouldBe
              "If you claim any of this money, you will need to pay it back to HMRC to settle your upcoming tax bill."
            document.select("#main-content .govuk-button").first().text() shouldBe claimBtn
          }
        }

      "there are multiple MFA credits sorted by tax year" in
        new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel()
            .withFirstRefund(4.50)
            .withMfaCredit(dueDate = LocalDate.of(2018, 1, 1), 1400.0)
            .withMfaCredit(dueDate = LocalDate.of(2018, 2, 1), 1000.0)
            .withAvailableCredit(0.0)
            .get()
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            "£1,400.00 credit from HMRC adjustment - 2017 to 2018 tax year"
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            "£1,000.00 credit from HMRC adjustment - 2017 to 2018 tax year"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
        }

      "there are multiple refunds claimed for full amount" in new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel()
            .withFirstRefund(4.50)
            .withPayment(dueDate = LocalDate.of(2019, 5, 15), 1400.0)
            .withPayment(dueDate = LocalDate.of(2019, 5, 15), 1000.0)
            .withAvailableCredit(0.0)
            .get()
        ) {

          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          layoutContent.selectFirst("p").text shouldBe "£0.00 is in your account"
          layoutContent.select("p").get(1).select("p:nth-child(1)").first().text() shouldBe
            s"£1,400.00 $paymentText 15 May 2019"
          layoutContent.select("p").get(2).select("p:nth-child(1)").first().text() shouldBe
            s"£1,000.00 $paymentText 15 May 2019"
          document.select("govuk-list govuk-list--bullet").isEmpty shouldBe true
        }
    }

    "display correct credit labels" when {

      "language is English" in
        new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel()
            .withAvailableCredit(3700.0)
            .withAllocatedFutureCredit(10.0)
            .withFirstRefund(20.0)
            .withSecondRefund(40.0)
            .withCutoverCredit(LocalDate.of(2023, 1, 1), 100.0)
            .withBalancingChargeCredit(LocalDate.of(2022, 1, 1), 200.0)
            .withRepaymentInterest(LocalDate.of(2020, 1, 1), 400.0)
            .withMfaCredit(LocalDate.of(2019, 1, 1), 500.0)
            .withPoaOneReconciliationCredit(LocalDate.of(2019, 5, 15), 1100)
            .withPoaTwoReconciliationCredit(LocalDate.of(2019, 5, 15), 1100)
            .withITSAReturnAmendmentCredit(LocalDate.of(2019, 5, 15), 300)
            .get()
        ) {
          document.title() shouldBe creditAndRefundHeadingWithTitleServiceNameGovUk
          layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            "£100.00 credit from an earlier tax year - 2022 to 2023 tax year"
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            "£200.00 credit from overpaid tax - 2021 to 2022 tax year"
          document.select("ul#credits-list li:nth-child(3)").text() shouldBe
            "£400.00 credit from repayment interest - 2019 to 2020 tax year"
          document.select("ul#credits-list li:nth-child(4)").text() shouldBe
            "£1,100.00 credit from first payment on account - 2019 to 2020 tax year"
          document.select("ul#credits-list li:nth-child(5)").text() shouldBe
            "£1,100.00 credit from second payment on account - 2019 to 2020 tax year"
          document.select("ul#credits-list li:nth-child(6)").text() shouldBe
            "£300.00 credit from amended tax return - 2019 to 2020 tax year"
          document.select("ul#credits-list li:nth-child(7)").text() shouldBe
            "£500.00 credit from HMRC adjustment - 2018 to 2019 tax year"
          document.select("ul#credits-list li:nth-child(8)").text() shouldBe "£40.00 is a refund currently in progress"
          document.select("ul#credits-list li:nth-child(9)").text() shouldBe "£20.00 is a refund currently in progress"
        }

      "language is Welsh" in
        new TestSetup(
          creditAndRefundModel = ANewCreditAndRefundModel()
            .withAvailableCredit(1200.0)
            .withCutoverCredit(LocalDate.of(2023, 1, 1), 100.0)
            .withBalancingChargeCredit(LocalDate.of(2022, 1, 1), 200.0)
            .withRepaymentInterest(LocalDate.of(2020, 1, 1), 400.0)
            .withMfaCredit(LocalDate.of(2019, 1, 1), 500.0)
            .withPoaOneReconciliationCredit(LocalDate.of(2019, 5, 15), 1100)
            .withPoaTwoReconciliationCredit(LocalDate.of(2019, 5, 15), 1100)
            .withITSAReturnAmendmentCredit(LocalDate.of(2019, 5, 15), 300)
            .get(),
          welshLang = true
        ) {
          document.title() shouldBe "Hawlio ad-daliad - Rheoli’ch Hunanasesiad - GOV.UK"
          layoutContent.selectHead("h1").text shouldBe "Hawlio ad-daliad"
          document.select("ul#credits-list li:nth-child(1)").text() shouldBe
            "Credyd o £100.00 o flwyddyn dreth gynharach - blwyddyn dreth 2022 i 2023"
          document.select("ul#credits-list li:nth-child(2)").text() shouldBe
            "Credyd o £200.00 o ordaliad treth - blwyddyn dreth 2021 i 2022"
          document.select("ul#credits-list li:nth-child(3)").text() shouldBe
            "Credyd o £400.00 o log ar ad-daliadau - blwyddyn dreth 2019 i 2020"
          document.select("ul#credits-list li:nth-child(4)").text() shouldBe
            "£1,100.00 o gredyd o’r taliad cyntaf ar gyfrif - blwyddyn dreth blwyddyn dreth 2019 i 2020"
          document.select("ul#credits-list li:nth-child(5)").text() shouldBe
            "£1,100.00 o gredyd o’r ail daliad ar gyfrif - blwyddyn dreth blwyddyn dreth 2019 i 2020"
          document.select("ul#credits-list li:nth-child(6)").text() shouldBe
            "£300.00 o gredyd o Ffurflen Dreth ddiwygiedig - blwyddyn dreth blwyddyn dreth 2019 i 2020"
          document.select("ul#credits-list li:nth-child(7)").text() shouldBe
            "Credyd o £500.00 o ganlyniad i addasiad gan CThEF - blwyddyn dreth 2018 i 2019"
        }
    }
  }

  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new TestSetup(
        isAgent = true,
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withTotalCredit(500.0)
          .withAvailableCredit(400.0)
          .get()
      ) {
        document.title() shouldBe creditAndRefundHeadingAgentWithTitleServiceNameGovUkAgent
        layoutContent.selectHead("h1").text shouldBe creditAndRefundHeading
        layoutContent.selectFirst("p").text shouldBe "£500.00 is in your account"
        layoutContent.select("p").get(1).text shouldBe
          "The most you can claim back is £400.00. This amount does not include any refunds that may already be in progress."
        layoutContent.select("p").get(2).text shouldBe
          "Money that you do not claim back can be used automatically by HMRC to cover your future tax bills when they become due."
        document.select("#main-content .govuk-button").isEmpty shouldBe true
      }
    }
  }
}
