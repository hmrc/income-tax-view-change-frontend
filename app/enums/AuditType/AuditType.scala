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

sealed trait AuditType {
  val name: String

  implicit def auditTypeToString(auditType: AuditType): String = {
    auditType.name
  }
}

case object AllowancesDeductionsDetailsResponse extends AuditType {
  val name = "AllowancesDeductionsDetailsResponse"
}

case object ChargeSummary extends AuditType {
  val name = "ChargeSummary"
}

case object ClaimARefundResponse extends AuditType {
  val name = "ClaimARefundResponse"
}

case object ForecastTaxCalculation extends AuditType {
  val name = "ForecastTaxCalculation"
}

case object ForecastIncome extends AuditType {
  val name = "ForecastIncome"
}

case object ItsaHomePage extends AuditType {
  val name = "ItsaHomePage"
}

case object IncomeSourceDetailsResponse extends AuditType {
  val name = "incomeSourceDetailsResponse"
}

case object InitiatePayNow extends AuditType {
  val name = "InitiatePayNow"
}

case object LowConfidenceLevelIvOutcomeFail extends AuditType {
  val name = "LowConfidenceLevelIvOutcomeFail"
}

case object LowConfidenceLevelIvOutcomeSuccess extends AuditType {
  val name = "LowConfidenceLevelIvOutcomeSuccess"
}

case object LowConfidenceLevelIvHandoff extends AuditType {
  val name = "LowConfidenceLevelIvHandoff"
}

case object ObligationsPageView extends AuditType {
  val name = "obligationsPageView"
}

case object ViewObligationsResponse extends AuditType {
  val name = "ViewObligationsResponse"
}

case object PaymentAllocations extends AuditType {
  val name = "PaymentAllocations"
}

case object PaymentHistoryResponse extends AuditType {
  val name = "PaymentHistoryResponse"
}

case object TaxCalculationDetailsResponse extends AuditType {
  val name = "TaxCalculationDetailsResponse"
}

case object TaxYearOverviewResponse extends AuditType {
  val name = "TaxYearOverviewResponse"
}

case object ViewInYearTaxEstimate extends AuditType {
  val name = "ViewInYearTaxEstimate"
}

case object WhatYouOweResponse extends AuditType {
  val name = "WhatYouOweResponse"
}

case object RefundToTaxPayerResponse extends AuditType {
  override val name: String = "RefundToTaxpayerResponse"
}

case object CreditsSummaryResponse extends AuditType {
  override val name: String = "CreditsSummaryResponse"
}

case object CeaseIncomeSource extends AuditType {
  override val name: String = "CeaseIncomeSource"
}

case object UpdateIncomeSource extends AuditType {
  override val name: String = "UpdateIncomeSource"
}

case object CreateIncomeSource extends AuditType {
  override val name: String = "CreateIncomeSource"
}

case object EnterClientUTR extends AuditType {
  override val name: String = "AgentLoginUTRSubmitted"
}

case object ClientDetailsConfirmed extends AuditType {
  override val name: String = "ClientDetailsConfirmed"
}

case object AdjustPaymentsOnAccount extends AuditType {
  override val name: String = "AdjustPaymentsOnAccount"
}

case object OptOutQuarterlyReportingRequest extends AuditType {
  override val name: String = "OptOutQuarterlyReportingRequest"
}

case object OptInQuarterlyReportingRequest extends AuditType {
  override val name: String = "OptInQuarterlyReportingRequest"
}

case object AccessDeniedForSupportingAgent extends AuditType {
  val name = "AccessDeniedForSupportingAgent"
}

case object SignUpTaxYearsPage extends AuditType {
  val name = "SignUpTaxYearsPage"
}