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

package utils

import enums.GatewayPage.{NoMatch, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import testUtils.TestSupport

class FallBackBackLinkSpec extends TestSupport with FallBackBackLinks {

  "FallBackBacklinks trait" when {

    "getPaymentAllocationBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(Some(PaymentHistoryPage), None, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/payment-refund-history"
      }
      "return Agent PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(Some(PaymentHistoryPage), None, None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payments/history"
      }

      "return Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), Some(2018), None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/tax-year-summary/2018#payments"
      }

      "return homepage link when no tax year available" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), None, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }

      "return Agent Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), Some(2018), None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2018#payments"
      }

      "return What You Owe link" in {
        val url = getPaymentAllocationBackUrl(Some(WhatYouOwePage), None, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "return Agent What You Owe link" in {
        val url = getPaymentAllocationBackUrl(Some(WhatYouOwePage), None, None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
      }

      "return homepage link if NoMatchPage" in {
        val url = getPaymentAllocationBackUrl(Some(NoMatch), None, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }

      "return homepage link if no gateway page found" in {
        val url = getPaymentAllocationBackUrl(None, None, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
    }

    "getChargeSummaryBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(Some(PaymentHistoryPage), 2018, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/payment-refund-history"
      }
      "return Agent PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(Some(PaymentHistoryPage), 2018, None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payments/history"
      }

      "return Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(Some(TaxYearSummaryPage), 2018, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/tax-year-summary/2018#payments"
      }

      "return Agent Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(Some(TaxYearSummaryPage), 2018, None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2018#payments"
      }

      "return What You Owe link" in {
        val url = getChargeSummaryBackUrl(Some(WhatYouOwePage), 2018, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }

      "return Agent What You Owe link" in {
        val url = getChargeSummaryBackUrl(Some(WhatYouOwePage), 2018, None, true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"
      }

      "return homepage link if NoMatchPage" in {
        val url = getChargeSummaryBackUrl(Some(NoMatch), 2018, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }

      "return homepage link if no page found" in {
        val url = getChargeSummaryBackUrl(None, 2018, None, false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
    }
  }
}
