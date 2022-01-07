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
import implicits.ImplicitCurrencyFormatter._
import implicits.ImplicitDateFormatter
import models.financialDetails.Payment
import testUtils.ViewSpec
import views.html.PaymentHistory
import java.time.LocalDate
import org.jsoup.nodes.Element
import play.api.test.FakeRequest


class PaymentHistoryViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentHistoryView: PaymentHistory = app.injector.instanceOf[PaymentHistory]

  object PaymentHistoryMessages {
    val title = "Payment history - Business Tax account - GOV.UK"
    val titleWhenAgentView = "Payment history - Your client’s Income Tax details - GOV.UK"

    val heading = "Payment history"
    val info = "If you cannot see all your previous payments here, you can find them in your classic Self Assessment service."

    def button(year: Int): String = s"$year payments"

    val paymentToHmrc = "Payment made to HMRC"
    val CardRef = "Payment made by debit card ref:"

    val paymentHeadingDate = "Date"
    val paymentHeadingDescription = "Description"
    val paymentHeadingAmount = "Amount"
    val partialH2Heading = "payments"
    val saLink = "Self Assessment service"
  }

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), Some("Payment"), Some("lot"), Some("lotitem"), Some("2019-12-25"), Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), Some("tnemyap"), Some("lot"), Some("lotitem"), Some("2007-03-23"), Some("DOCID02"))
  )

  class PaymentHistorySetup(testPayments: List[Payment], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(testPayments, "testBackURL", saUtr, isAgent = false)(FakeRequest(),implicitly)
  )

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.title}'" in new PaymentHistorySetup(testPayments) {
        document.title() shouldBe PaymentHistoryMessages.title
      }

      s"have the h1 heading '${PaymentHistoryMessages.heading}'" in new PaymentHistorySetup(testPayments) {
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.heading
      }

      s"has the h2 heading '${PaymentHistoryMessages.heading}'" in new PaymentHistorySetup(testPayments) {
        layoutContent.selectHead("h2").text.contains(PaymentHistoryMessages.partialH2Heading)
      }

      s"has a table of payment history" which {
        s"has the table caption" in new PaymentHistorySetup(testPayments)  {
          layoutContent.selectHead("div").selectNth("div", 2).selectHead("table")
            .selectHead("caption").text.contains(PaymentHistoryMessages.partialH2Heading)
        }
        s"has table headings for each table column" in new PaymentHistorySetup(testPayments) {
          val row: Element = layoutContent.selectHead("div").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
          row.selectNth("th", 1).text shouldBe PaymentHistoryMessages.paymentHeadingDate
          row.selectNth("th", 2).text shouldBe PaymentHistoryMessages.paymentHeadingDescription
          row.selectNth("th", 3).text shouldBe PaymentHistoryMessages.paymentHeadingAmount
        }
      }

      s"have the information  ${PaymentHistoryMessages.info}" in new PaymentHistorySetup(testPayments) {
        layoutContent.select(Selectors.p).text shouldBe PaymentHistoryMessages.info
        layoutContent.selectFirst(Selectors.p).hasCorrectLink(PaymentHistoryMessages.saLink, "http://localhost:8930/self-assessment/ind/1234567890/account")
      }

      s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(testPayments, None) {
        layoutContent.select("#payment-history-info").text should not be PaymentHistoryMessages.info
      }

      "display payment history by year" in new PaymentHistorySetup(testPayments) {
        val orderedPayments: Map[Int, List[Payment]] = testPayments.groupBy { payment => LocalDate.parse(payment.date.get).getYear }
        for(((year, payments), index) <- orderedPayments.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
        val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.date.get).toLongDate
              row.selectNth("td", 2).text shouldBe PaymentHistoryMessages.paymentToHmrc + s" ${payment.transactionId.get}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe s"/report-quarterly/income-and-expenses/view/charges/payments-made?documentNumber=${payment.transactionId.get}"
              row.selectNth("td", 3).text shouldBe payment.amount.get.toCurrencyString
          }
        }
      }
    }
  }

  class PaymentHistorySetupWhenAgentView(testPayments: List[Payment], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(testPayments, "testBackURL", saUtr, isAgent = true)(FakeRequest(),implicitly)
  )

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.titleWhenAgentView}'" in new PaymentHistorySetupWhenAgentView(testPayments) {
        document.title() shouldBe PaymentHistoryMessages.titleWhenAgentView
      }
    }
  }
}
