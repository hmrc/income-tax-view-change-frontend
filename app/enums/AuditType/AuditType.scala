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

package enums.AuditType

import scala.language.implicitConversions

enum AuditType:
  case AllowancesDeductionsDetailsResponse, ChargeSummary, ClaimARefundResponse, ForecastTaxCalculation, ForecastIncome,
  ItsaHomePage, IncomeSourceDetailsResponse, InitiatePayNow, LowConfidenceLevelIvOutcomeFail, 
  LowConfidenceLevelIvOutcomeSuccess, LowConfidenceLevelIvHandoff, ObligationsPageView, ViewObligationsResponse,
  PaymentAllocations, PaymentHistoryResponse, TaxCalculationDetailsResponse, TaxYearOverviewResponse,
  ViewInYearTaxEstimate, WhatYouOweResponse, RefundToTaxPayerResponse, CreditsSummaryResponse, CeaseIncomeSource,
  UpdateIncomeSource, CreateIncomeSource, EnterClientUTR, ClientDetailsConfirmed, AdjustPaymentsOnAccount,
  OptOutQuarterlyReportingRequest, OptInQuarterlyReportingRequest, AccessDeniedForSupportingAgent,
  SignUpTaxYearsPage, OptOutTaxYearsPage, ReportingObligationsPage 

object AuditType:
  given Conversion[AuditType, String] = auditType => auditType.toString