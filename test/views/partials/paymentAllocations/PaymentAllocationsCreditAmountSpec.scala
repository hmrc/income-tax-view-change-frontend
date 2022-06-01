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

package views.partials.paymentAllocations

import models.paymentAllocationCharges.PaymentAllocationViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testMtdItUser
import testConstants.PaymentAllocationsTestConstants.{paymentAllocationChargesModel, paymentAllocationChargesModelWithCredit, paymentAllocationViewModel}
import testUtils.TestSupport
import views.html.partials.paymentAllocations.PaymentAllocationsCreditAmount


class PaymentAllocationsCreditAmountSpec extends TestSupport {

  lazy val paymentAllocationsCreditAmount = app.injector.instanceOf[PaymentAllocationsCreditAmount]

  class Setup(isAgent: Boolean = false, creditsRefundsFSEnabled: Boolean = true) {
    val paymentAllocations = PaymentAllocationViewModel(paymentAllocationChargesModelWithCredit, Seq())
    val html: HtmlFormat.Appendable = paymentAllocationsCreditAmount(paymentAllocations, creditsRefundsFSEnabled, isAgent)
    val pageDocument: Document = Jsoup.parse("<table>" + contentAsString(html) + "</table>")
  }

  "Payment Allocations Credit Amount Partial" should {

    "display the money in account row" in new Setup() {
      pageDocument.select("a#money-on-account-link").size() shouldBe 1
      pageDocument.select("a#money-on-account-link").text() shouldBe messages("paymentAllocation.moneyOnAccount")
      pageDocument.select("a#money-on-account-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/credit-and-refunds"
    }

    "display the money in account row for AGENT" in new Setup(true) {
      pageDocument.select("a#money-on-account-link").size() shouldBe 1
      pageDocument.select("a#money-on-account-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/credit-and-refunds"
    }

    "not display link when credits and refunds page feature switch is disabled" in new Setup(false, false) {
      pageDocument.select("a#money-on-account-link").size() shouldBe 0
      pageDocument.select("tr#money-on-account > td:first-child").text() shouldBe messages("paymentAllocation.moneyOnAccount")
    }
  }
}
