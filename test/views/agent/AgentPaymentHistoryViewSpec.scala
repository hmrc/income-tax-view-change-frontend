/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import config.FrontendAppConfig
import implicits.ImplicitCurrencyFormatter._
import implicits.ImplicitDateFormatter
import models.financialDetails.Payment
import play.api.test.FakeRequest
import testUtils.ViewSpec
import views.html.agent.AgentsPaymentHistory

import java.time.LocalDate


class AgentPaymentHistoryViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val agentsPaymentHistory: AgentsPaymentHistory = app.injector.instanceOf[AgentsPaymentHistory]


  object PaymentHistoryMessages {
    val title = "Payment history - Your client’s Income Tax details - GOV.UK"
    val heading = "Payment history"
    val info = "If you cannot see all your previous payments here, you can find them in your classic Self Assessment service."

    def button(year: Int): String = s"$year payments"

    val paymentToHmrc = "Payment made to HMRC"
    val CardRef = "Payment made by debit card ref:"
  }

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), Some("Payment"), Some("lot"), Some("lotitem"), Some("2019-12-25")),
    Payment(Some("BBBBB"), Some(5000), Some("tnemyap"), Some("lot"), Some("lotitem"), Some("2007-03-23"))
  )

  class PaymentHistorySetup(testPayments: List[Payment], saUtr: Option[String] = Some("1234567890")) extends Setup(
    agentsPaymentHistory(testPayments, mockImplicitDateFormatter, "testBackURL", saUtr)(FakeRequest(), implicitly, mockAppConfig)
  )

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.title}'" in new PaymentHistorySetup(testPayments) {
        document.title() shouldBe PaymentHistoryMessages.title
      }

      s"have the heading '${PaymentHistoryMessages.heading}'" in new PaymentHistorySetup(testPayments) {
        content.selectHead("h1").text shouldBe PaymentHistoryMessages.heading
      }

      s"have the information  ${PaymentHistoryMessages.info}" in new PaymentHistorySetup(testPayments) {
        content.selectHead("#payment-history-info").text shouldBe PaymentHistoryMessages.info
        content.selectHead("#payment-history-info a").attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      }

      s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(testPayments, None) {
        content.select("#payment-history-info").text should not be PaymentHistoryMessages.info
      }

      "display payment history by year" in new PaymentHistorySetup(testPayments) {
        testPayments.groupBy { payment => LocalDate.parse(payment.date.get).getYear }.map { case (year, payments) =>

          content.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = content.selectHead(s"#accordion-default-content-$year")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.date.get).toLongDate
              row.selectNth("td", 2).text shouldBe PaymentHistoryMessages.paymentToHmrc
              row.selectNth("td", 3).text shouldBe payment.amount.get.toCurrencyString
          }
        }
      }
    }
  }
}
