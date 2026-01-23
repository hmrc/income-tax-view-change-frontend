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

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import implicits.ImplicitDateParser
import models.liabilitycalculation.EndOfYearEstimate
import play.api.libs.json.{JsValue, Json}
import utils.Utilities._


case class ForecastTaxCalculationAuditModel(user: MtdItUser[_], endOfYearEstimate: EndOfYearEstimate)
  extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = enums.TransactionName.ForecastTaxCalculation
  override val auditType: String = enums.AuditType.ForecastTaxCalculation

  private val totalEstimatedIncome: Option[Int] = endOfYearEstimate.totalEstimatedIncome
  private val totalTaxableIncome: Option[Int] = endOfYearEstimate.totalTaxableIncome
  private val totalAllowancesAndDeductions: Option[BigDecimal] = endOfYearEstimate.totalAllowancesAndDeductions
  private val totalIncomeTax: Option[BigDecimal] = endOfYearEstimate.incomeTaxAmount
  private val class4NationalInsurance: Option[BigDecimal] = endOfYearEstimate.nic2
  private val class2NationalInsurance: Option[BigDecimal] = endOfYearEstimate.nic4
  private val totalNationalInsuranceContributions: Option[BigDecimal] = endOfYearEstimate.totalNicAmount
  private val totalTaxDeductedBeforeBalancingPayment: Option[BigDecimal] = endOfYearEstimate.totalTaxDeductedBeforeCodingOut
  private val balancingPaymentCollectedThroughPAYE: Option[BigDecimal] = endOfYearEstimate.saUnderpaymentsCodedOut
  private val studentLoanRepayments: Option[BigDecimal] = endOfYearEstimate.totalStudentLoansRepaymentAmount
  private val taxDueOnAnnuityPayments: Option[BigDecimal] = endOfYearEstimate.totalAnnuityPaymentsTaxCharged
  private val taxDueOnRoyaltyPayments: Option[BigDecimal] = endOfYearEstimate.totalRoyaltyPaymentsTaxCharged
  private val totalTaxDeducted: Option[BigDecimal] = endOfYearEstimate.totalTaxDeducted
  private val incomeTaxAndNationalInsuranceContributionsDue: Option[BigDecimal] = endOfYearEstimate.incomeTaxNicAmount
  private val capitalGainsTax: Option[BigDecimal] = endOfYearEstimate.cgtAmount
  private val forecastSelfAssessmentTaxAmount: Option[BigDecimal] = endOfYearEstimate.incomeTaxNicAndCgtAmount


  override val detail: JsValue = {
    userAuditDetails(user) ++
      Json.obj() ++
     Json.obj("totalEstimatedIncome"-> totalEstimatedIncome) ++
      Json.obj("totalAllowancesAndDeductions"-> totalAllowancesAndDeductions) ++
      Json.obj("totalTaxableIncome"-> totalTaxableIncome) ++
      Json.obj("totalIncomeTax"-> totalIncomeTax) ++
      Json.obj("class4NationalInsurance"-> class4NationalInsurance) ++
      Json.obj("class2NationalInsurance"-> class2NationalInsurance) ++
      Json.obj("totalNationalInsuranceContributions"-> totalNationalInsuranceContributions) ++
      Json.obj("totalTaxDeductedBeforeBalancingPayment"-> totalTaxDeductedBeforeBalancingPayment) ++
      Json.obj("balancingPaymentCollectedThroughPAYE"-> balancingPaymentCollectedThroughPAYE) ++
      Json.obj("studentLoanRepayments"-> studentLoanRepayments) ++
      Json.obj("taxDueOnAnnuityPayments"-> taxDueOnAnnuityPayments) ++
      Json.obj("taxDueOnRoyaltyPayments"-> taxDueOnRoyaltyPayments) ++
      Json.obj("totalTaxDeducted"-> totalTaxDeducted) ++
      Json.obj("incomeTaxAndNationalInsuranceContributionsDue"-> incomeTaxAndNationalInsuranceContributionsDue) ++
      Json.obj("capitalGainsTax"-> capitalGainsTax) ++
      Json.obj("forecastSelfAssessmentTaxAmount"-> forecastSelfAssessmentTaxAmount)
  }
}
