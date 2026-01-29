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

enum TransactionName(val name: String):
  case AllowancesDeductionsDetailsResponse extends TransactionName("allowances-deductions-details-response")
  case ChargeSummary extends TransactionName("charge-summary")
  case ClaimARefund extends TransactionName("claim-a-refund")
  case ForecastTaxCalculation extends TransactionName("forecast-tax-calculation")
  case ForecastIncome extends TransactionName("forecast-income")
  case ItsaHomePage extends TransactionName("itsa-home-page")
  case IncomeSourceDetailsResponse extends TransactionName("income-source-details-response")
  case InitiatePayNow extends TransactionName("initiate-pay-now")
  case LowConfidenceLevelIvOutcomeFail extends TransactionName("IV-uplift-failure-outcome")
  case LowConfidenceLevelIvOutcomeSuccess extends TransactionName("IV-uplift-success-outcome")
  case LowConfidenceLevelIvHandoff extends TransactionName("low-confidence-level-IV-handoff")
  case ObligationsPageView extends TransactionName("ITVCObligations")
  case ViewObligationsResponse extends TransactionName("view-obligations-response")
  case PaymentAllocations extends TransactionName("payment-allocations-response")
  case PaymentHistoryResponse extends TransactionName("payment-history-response")
  case TaxCalculationDetailsResponse extends TransactionName("tax-calculation-response")
  case TaxYearOverviewResponse extends TransactionName("tax-year-overview-response")
  case ViewInYearTaxEstimate extends TransactionName("ViewInYearTaxEstimate")
  case WhatYouOweResponse extends TransactionName("what-you-owe-response")
  case RefundToTaxPayer extends TransactionName("refund-to-taxpayer")
  case CreditsSummary extends TransactionName("credits-summary")
  case CeaseIncomeSource extends TransactionName("cease-income-source")
  case UpdateIncomeSource extends TransactionName("update-income-source")
  case CreateIncomeSource extends TransactionName("create-income-source")
  case EnterClientUTR extends TransactionName("agent-login-utr-submitted")
  case ClientDetailsConfirmed extends TransactionName("client-details-confirmed")
  case AdjustPaymentsOnAccount extends TransactionName("adjust-payments-on-account")
  case OptOutQuarterlyReportingRequest extends TransactionName("opt-out-quarterly-reporting-request")
  case OptInQuarterlyReportingRequest extends TransactionName("opt-in-quarterly-reporting-request")
  case AccessDeniedForSupportingAgent extends TransactionName("access-denied-for-supporting-agent")
  case SignUpTaxYearsPage extends TransactionName("sign-up-tax-years-page")
  case OptOutTaxYearsPage extends TransactionName("opt-out-tax-years-page")
  case ReportingObligationsPage extends TransactionName("reporting-obligations-page")
  
object TransactionName:
  given Conversion[TransactionName, String] = transactionName => transactionName.name