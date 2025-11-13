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

package enums.TransactionName

import scala.language.implicitConversions

sealed trait TransactionName {
  val name: String

  implicit def transactionNameToString(transactionName: TransactionName): String = {
    transactionName.name
  }
}

case object AllowancesDeductionsDetailsResponse extends TransactionName {
  val name = "allowances-deductions-details-response"
}

case object ChargeSummary extends TransactionName {
  val name = "charge-summary"
}

case object ClaimARefund extends TransactionName {
  val name = "claim-a-refund"
}

case object ForecastTaxCalculation extends TransactionName {
  val name = "forecast-tax-calculation"
}

case object ForecastIncome extends TransactionName {
  val name = "forecast-income"
}

case object ItsaHomePage extends TransactionName {
  val name = "itsa-home-page"
}

case object IncomeSourceDetailsResponse extends TransactionName {
  val name = "income-source-details-response"
}

case object InitiatePayNow extends TransactionName {
  val name = "initiate-pay-now"
}

case object LowConfidenceLevelIvOutcomeFail extends TransactionName {
  val name = "IV-uplift-failure-outcome"
}

case object LowConfidenceLevelIvOutcomeSuccess extends TransactionName {
  val name = "IV-uplift-success-outcome"
}

case object LowConfidenceLevelIvHandoff extends TransactionName {
  val name = "low-confidence-level-IV-handoff"
}

case object ObligationsPageView extends TransactionName {
  val name = "ITVCObligations"
}

case object ViewObligationsResponse extends TransactionName {
  val name = "view-obligations-response"
}

case object PaymentAllocations extends TransactionName {
  val name = "payment-allocations-response"
}

case object PaymentHistoryResponse extends TransactionName {
  val name = "payment-history-response"
}

case object TaxCalculationDetailsResponse extends TransactionName {
  val name = "tax-calculation-response"
}

case object TaxYearOverviewResponse extends TransactionName {
  val name = "tax-year-overview-response"
}

case object ViewInYearTaxEstimate extends TransactionName {
  val name = "ViewInYearTaxEstimate"
}

case object WhatYouOweResponse extends TransactionName {
  val name = "what-you-owe-response"
}

case object RefundToTaxPayer extends TransactionName {
  val name = "refund-to-taxpayer"
}

case object CreditsSummary extends TransactionName {
  val name = "credits-summary"
}

case object CeaseIncomeSource extends TransactionName {
  val name = "cease-income-source"
}

case object UpdateIncomeSource extends TransactionName {
  val name = "update-income-source"
}

case object CreateIncomeSource extends TransactionName {
  val name = "create-income-source"
}

case object EnterClientUTR extends TransactionName {
  val name = "agent-login-utr-submitted"
}

case object ClientDetailsConfirmed extends TransactionName {
  val name = "client-details-confirmed"
}

case object AdjustPaymentsOnAccount extends TransactionName {
  val name = "adjust-payments-on-account"
}

case object OptOutQuarterlyReportingRequest extends TransactionName {
  val name = "opt-out-quarterly-reporting-request"
}

case object OptInQuarterlyReportingRequest extends TransactionName {
  val name = "opt-in-quarterly-reporting-request"
}

case object AccessDeniedForSupportingAgent extends  TransactionName {
  val name = "access-denied-for-supporting-agent"
}

case object SignUpTaxYearsPage extends TransactionName {
  val name = "sign-up-tax-years-page"
}

case object OptOutTaxYearsPage extends TransactionName {
  val name = "opt-out-tax-years-page"
}
