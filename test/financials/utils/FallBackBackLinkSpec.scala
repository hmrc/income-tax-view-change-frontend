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

package financials.utils

import common.enums.GatewayPage.{NoMatch, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import common.testUtils.TestSupport

class FallBackBackLinkSpec extends TestSupport with FallBackBackLinks {

  val returnsFrontendEnabled: Boolean = true

  private val testTaxYear = 2018

  "FallBackBacklinks trait" when {
    
    "getPaymentAllocationBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, Some(PaymentHistoryPage), None, None)
        url should include("/payment-refund-history")
      }
      "return Agent PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, Some(PaymentHistoryPage), None, None)
        url should include("/agents/payment-refund-history")
      }

      "return Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, Some(TaxYearSummaryPage), Some(testTaxYear), None)
        url should include(s"/tax-year-summary/$testTaxYear#payments")
      }
      "return Agent Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, Some(TaxYearSummaryPage), Some(testTaxYear), None)
        url should include(s"/agents/tax-year-summary/$testTaxYear#payments")
      }

      "return homepage link when no tax year available" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, Some(TaxYearSummaryPage), None, None)
        url should include("/report-quarterly/income-and-expenses/view")
      }
      "return Agent homepage link when no tax year available" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, Some(TaxYearSummaryPage), None, None)
        url should include("/report-quarterly/income-and-expenses/view/agents")
      }

      "return What You Owe link" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, Some(WhatYouOwePage), None, None)
        url should include("/what-you-owe")
      }
      "return Agent What You Owe link" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, Some(WhatYouOwePage), None, None)
        url should include("/agents/what-your-client-owes")
      }

      "return homepage link if NoMatchPage" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, Some(NoMatch), None, None)
        url should include("/report-quarterly/income-and-expenses/view")
      }
      "return Agent homepage link if NoMatchPage" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, Some(NoMatch), None, None)
        url should include("/report-quarterly/income-and-expenses/view/agents")
      }

      "return homepage link if no gateway page found" in {
        val url = getPaymentAllocationBackUrl(isAgent = false, None, None, None)
        url should include("/report-quarterly/income-and-expenses/view")
      }
      "return Agent homepage link if no gateway page found" in {
        val url = getPaymentAllocationBackUrl(isAgent = true, None, None, None)
        url should include("/report-quarterly/income-and-expenses/view/agents")
      }
    }

    "getChargeSummaryBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(isAgent = false, Some(PaymentHistoryPage), testTaxYear, None)
        url should include("/payment-refund-history")
      }
      "return Agent PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(isAgent = true, Some(PaymentHistoryPage), testTaxYear, None)
        url should include("/agents/payment-refund-history")
      }

      "return Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(isAgent = false, Some(TaxYearSummaryPage), testTaxYear, None)
        url should include(s"/tax-year-summary/$testTaxYear#payments")
      }
      "return Agent Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(isAgent = true, Some(TaxYearSummaryPage), testTaxYear, None)
        url should include(s"/agents/tax-year-summary/$testTaxYear#payments")
      }

      "return What You Owe link" in {
        val url = getChargeSummaryBackUrl(isAgent = false, Some(WhatYouOwePage), testTaxYear, None)
        url should include("/what-you-owe")
      }
      "return Agent What You Owe link" in {
        val url = getChargeSummaryBackUrl(isAgent = true, Some(WhatYouOwePage), testTaxYear, None)  
        url should include("/agents/what-your-client-owes")
      }

      "return homepage link if NoMatchPage" in {
        val url = getChargeSummaryBackUrl(isAgent = false, Some(NoMatch), testTaxYear, None)
        url should include("/report-quarterly/income-and-expenses/view")
      }
      "return Agent homepage link if NoMatchPage" in {
        val url = getChargeSummaryBackUrl(isAgent = true, Some(NoMatch), testTaxYear, None)
        url should include("/report-quarterly/income-and-expenses/view/agents")
      }

      "return homepage link if no page found" in {
        val url = getChargeSummaryBackUrl(isAgent = false, None, testTaxYear, None)
        url should include("/report-quarterly/income-and-expenses/view")
      }
      "return Agent homepage link if no page found" in {
        val url = getChargeSummaryBackUrl(isAgent = true, None, testTaxYear, None)
        url should include("/report-quarterly/income-and-expenses/view/agents")
      }
    }
  }
}
