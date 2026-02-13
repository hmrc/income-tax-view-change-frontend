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
import java.time.LocalDate
import implicits.ImplicitDateFormatter
import models.creditsandrefunds.{CreditsModel, MoneyInYourAccountViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testConstants.ANewCreditAndRefundModel
import testUtils.{TestSupport, ViewSpec}
import views.html.MoneyInYourAccountView


class MoneyInYourAccountViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val moneyInYourAccountView: MoneyInYourAccountView = app.injector.instanceOf[MoneyInYourAccountView]
  val moneyInYourAccountHeading: String = messages("money-in-your-account.heading")

  val individualTitle: String = messages("htmlTitle", moneyInYourAccountHeading)
  val agentTitle: String = messages("htmlTitle.agent", moneyInYourAccountHeading)

  private def dateInYear(year: Int) = LocalDate.of(year, 1, 1)

  class TestSetup(
                   creditAndRefundModel: CreditsModel,
                   isAgent: Boolean = false,
                   backUrl: String = "testString",
                   welshLang: Boolean = false) {

    val testMessages: Messages = if (welshLang) {
      app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
    } else {
      messages
    }

    val testUrl = "testUrl"

    val viewModel: MoneyInYourAccountViewModel = MoneyInYourAccountViewModel.fromCreditsModel(creditAndRefundModel, testUrl)

    lazy val page: HtmlFormat.Appendable =
      moneyInYourAccountView(
        viewModel,
        isAgent = isAgent,
        backUrl)(FakeRequest(), implicitly, testMessages)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "displaying individual credit and refund page" should {

    "display the page" when {

      "the user has no credit or refunds" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel().get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation").text() shouldBe "You do not have any money in your account at the moment."
      }
      "the user has no credit and one refund" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel().firstRefundAmountRequested(100).get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation").text() shouldBe "You do not have any money in your account at the moment, but you have a refund in progress."
        document.selectById("refunds-link").attribute("href").toString.contains("/report-quarterly/income-and-expenses/view/refund-status")
      }
      "the user has no credit and more than one refund" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel().firstRefundAmountRequested(100).secondRefundAmountRequested(200).get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation").text() shouldBe "You do not have any money in your account at the moment, but you have refunds in progress."
        document.selectById("refunds-link").attribute("href").toString.contains("/report-quarterly/income-and-expenses/view/refund-status")
      }

      "the user has credit which has not been allocated" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(100)
          .withUnallocatedCredit(100)
          .withPayment(dateInYear(2024), 50)
          .withPayment(dateInYear(2024), 50)
          .get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation").text() shouldBe "This amount has not been set aside for any charges yet."
        document.hasTableWithCaption("Where the money came from")
        document.hasTableWithCorrectSize(1, 4)
        document.hasTableWithCorrectHeadings(List("Date", "Description", "Tax year", "Amount"))
        document.selectById("claim-a-refund-button").text() shouldBe "Claim a refund"
      }
      "the user has credit which has all been allocated" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(100)
          .withAllocatedFutureCredit(100)
          .withBalancingChargeCredit(dateInYear(2024), 50)
          .withPoaOneReconciliationCredit(dateInYear(2023), 50)
          .get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation").text() shouldBe "This amount has been set aside to pay for upcoming charges. You can still claim it back, but it may be easier to leave it in your account to avoid missing any payment deadlines."
        document.hasTableWithCaption("Where the money came from")
        document.hasTableWithCorrectSize(1, 4)
        document.hasTableWithCorrectHeadings(List("Date", "Description", "Tax year", "Amount"))
        document.selectById("claim-a-refund-button").text() shouldBe "Claim a refund"
      }
      "the user has credit which has been partially allocated" in new TestSetup(
        creditAndRefundModel = ANewCreditAndRefundModel()
          .withAvailableCredit(100)
          .withPayment(dateInYear(2024), 100)
          .withUnallocatedCredit(100)
          .withAllocatedFutureCredit(100).get()
      ) {
        document.title() shouldBe individualTitle
        layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        document.selectById("credit-explanation-1").text() shouldBe "£100.00 has been set aside to pay for upcoming charges. You can claim this money back, but it may be easier to leave it in your account to avoid missing any payment deadlines."
        document.selectById("credit-explanation-2").text() shouldBe "If you claim more than £100.00, you’ll need to make another payment to cover your upcoming charges before the deadline."
        document.hasTableWithCaption("Where the money came from")
        document.hasTableWithCorrectSize(1, 3)
        document.hasTableWithCorrectHeadings(List("Date", "Description", "Tax year", "Amount"))
        document.selectById("claim-a-refund-button").text() shouldBe "Claim a refund"
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
          document.title() shouldBe agentTitle
          layoutContent.selectHead("h1").text shouldBe moneyInYourAccountHeading
        }
      }
    }
  }
}
