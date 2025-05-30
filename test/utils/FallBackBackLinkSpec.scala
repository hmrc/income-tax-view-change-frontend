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

package utils

import enums.GatewayPage.{NoMatch, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage, YourSelfAssessmentChargeSummaryPage}
import testUtils.TestSupport

class FallBackBackLinkSpec extends TestSupport with FallBackBackLinks {

  private val testTaxYear = 2018

  "FallBackBacklinks trait" when {

    "getPaymentAllocationBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(Some(PaymentHistoryPage), None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/payment-refund-history"
      }
      "return Agent PaymentHistory link" in {
        val url = getPaymentAllocationBackUrl(Some(PaymentHistoryPage), None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payment-refund-history"
      }

      "return Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), Some(testTaxYear), None, isAgent = false)
        url shouldBe s"/report-quarterly/income-and-expenses/view/tax-year-summary/$testTaxYear#payments"
      }
      "return Agent Tax Year Summary link" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), Some(testTaxYear), None, isAgent = true)
        url shouldBe s"/report-quarterly/income-and-expenses/view/agents/tax-year-summary/$testTaxYear#payments"
      }

      "return homepage link when no tax year available" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
      "return Agent homepage link when no tax year available" in {
        val url = getPaymentAllocationBackUrl(Some(TaxYearSummaryPage), None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
      }

      "return What You Owe link" in {
        val url = getPaymentAllocationBackUrl(Some(WhatYouOwePage), None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }
      "return Agent What You Owe link" in {
        val url = getPaymentAllocationBackUrl(Some(WhatYouOwePage), None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes"
      }

      "return Your Self Assessment charges link" in {
        val url = getPaymentAllocationBackUrl(Some(YourSelfAssessmentChargeSummaryPage), None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
      }
      "return Agent Your Self Assessment charges link" in {
        val url = getPaymentAllocationBackUrl(Some(YourSelfAssessmentChargeSummaryPage), None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges"
      }

      "return homepage link if NoMatchPage" in {
        val url = getPaymentAllocationBackUrl(Some(NoMatch), None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
      "return Agent homepage link if NoMatchPage" in {
        val url = getPaymentAllocationBackUrl(Some(NoMatch), None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
      }

      "return homepage link if no gateway page found" in {
        val url = getPaymentAllocationBackUrl(None, None, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
      "return Agent homepage link if no gateway page found" in {
        val url = getPaymentAllocationBackUrl(None, None, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
      }
    }

    "getChargeSummaryBackUrl method" should {
      "return PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(Some(PaymentHistoryPage), testTaxYear, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/payment-refund-history"
      }
      "return Agent PaymentHistory link" in {
        val url = getChargeSummaryBackUrl(Some(PaymentHistoryPage), testTaxYear, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/payment-refund-history"
      }

      "return Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(Some(TaxYearSummaryPage), testTaxYear, None, isAgent = false)
        url shouldBe s"/report-quarterly/income-and-expenses/view/tax-year-summary/$testTaxYear#payments"
      }
      "return Agent Tax Year Summary link" in {
        val url = getChargeSummaryBackUrl(Some(TaxYearSummaryPage), testTaxYear, None, isAgent = true)
        url shouldBe s"/report-quarterly/income-and-expenses/view/agents/tax-year-summary/$testTaxYear#payments"
      }

      "return What You Owe link" in {
        val url = getChargeSummaryBackUrl(Some(WhatYouOwePage), testTaxYear, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/what-you-owe"
      }
      "return Agent What You Owe link" in {
        val url = getChargeSummaryBackUrl(Some(WhatYouOwePage), testTaxYear, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes"
      }

      "return Your Self Assessment charges link" in {
        val url = getChargeSummaryBackUrl(Some(YourSelfAssessmentChargeSummaryPage), testTaxYear, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
      }
      "return Agent Your Self Assessmnet charges link" in {
        val url = getChargeSummaryBackUrl(Some(YourSelfAssessmentChargeSummaryPage), testTaxYear, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/your-self-assessment-charges"
      }

      "return homepage link if NoMatchPage" in {
        val url = getChargeSummaryBackUrl(Some(NoMatch), testTaxYear, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
      "return Agent homepage link if NoMatchPage" in {
        val url = getChargeSummaryBackUrl(Some(NoMatch), testTaxYear, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
      }

      "return homepage link if no page found" in {
        val url = getChargeSummaryBackUrl(None, testTaxYear, None, isAgent = false)
        url shouldBe "/report-quarterly/income-and-expenses/view"
      }
      "return Agent homepage link if no page found" in {
        val url = getChargeSummaryBackUrl(None, testTaxYear, None, isAgent = true)
        url shouldBe "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
      }
    }
  }
}
