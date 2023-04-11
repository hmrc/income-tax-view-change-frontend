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
import play.api.libs.json.JsValue
import utils.Utilities._


case class ForecastTaxCalculationAuditModel(user: MtdItUser[_], endOfYearEstimate: EndOfYearEstimate)
  extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = enums.TransactionName.ForecastTaxCalculation
  override val auditType: String = enums.AuditType.TaxYearOverviewResponse

  val totalEstimatedIncome: Option[Int] = endOfYearEstimate.totalEstimatedIncome
  val totalTaxableIncome: Option[Int] = endOfYearEstimate.totalTaxableIncome
  val totalAllowancesAndDeductions: Option[BigDecimal] = endOfYearEstimate.totalAllowancesAndDeductions
  val totalIncomeTax: Option[BigDecimal] = endOfYearEstimate.incomeTaxAmount
  val class4NationalInsurance: Option[BigDecimal] = endOfYearEstimate.nic2
  val class2NationalInsurance: Option[BigDecimal] = endOfYearEstimate.nic4
  val totalNationalInsuranceContributions: Option[BigDecimal] = endOfYearEstimate.totalNicAmount
  val totalTaxDeductedBeforeBalancingPayment: Option[BigDecimal] = endOfYearEstimate.totalTaxDeductedBeforeCodingOut
  val balancingPaymentCollectedThroughPAYE: Option[BigDecimal] = endOfYearEstimate.saUnderpaymentsCodedOut
  val studentLoanRepayments: Option[BigDecimal] = endOfYearEstimate.totalStudentLoansRepaymentAmount
  val taxDueOnAnnuityPayments: Option[BigDecimal] = endOfYearEstimate.totalAnnuityPaymentsTaxCharged
  val totalTaxDeducted: Option[BigDecimal] = endOfYearEstimate.totalTaxDeducted
  val incomeTaxAndNationalInsuranceContributionsDue: Option[BigDecimal] = endOfYearEstimate.incomeTaxNicAmount
  val capitalGainsTax: Option[BigDecimal] = endOfYearEstimate.cgtAmount
  val forecastSelfAssessmentTaxAmount: Option[BigDecimal] = endOfYearEstimate.incomeTaxNicAndCgtAmount


  override val detail: JsValue = {
    userAuditDetails(user) ++
      ("totalEstimatedIncome", totalEstimatedIncome) ++
      ("totalTaxableIncome", totalTaxableIncome) ++
      ("totalAllowancesAndDeductions", totalAllowancesAndDeductions) ++
      ("totalIncomeTax", totalIncomeTax) ++
      ("class4NationalInsurance", class4NationalInsurance) ++
      ("class2NationalInsurance", class2NationalInsurance) ++
      ("totalNationalInsuranceContributions", totalNationalInsuranceContributions) ++
      ("totalTaxDeductedBeforeBalancingPayment", totalTaxDeductedBeforeBalancingPayment) ++
      ("balancingPaymentCollectedThroughPAYE", balancingPaymentCollectedThroughPAYE) ++
      ("studentLoanRepayments", studentLoanRepayments) ++
      ("taxDueOnAnnuityPayments", taxDueOnAnnuityPayments) ++
      ("totalTaxDeducted", totalTaxDeducted) ++
      ("incomeTaxAndNationalInsuranceContributionsDue", incomeTaxAndNationalInsuranceContributionsDue) ++
      ("capitalGainsTax", capitalGainsTax) ++
      ("forecastSelfAssessmentTaxAmount", forecastSelfAssessmentTaxAmount)
  }
}








