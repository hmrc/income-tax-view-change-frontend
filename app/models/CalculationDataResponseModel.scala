/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import play.api.libs.json.{OFormat, Json}

sealed trait CalculationDataResponseModel

case class CalcDisplayModel(calcTimestamp: String,
                            calcAmount: BigDecimal,
                            calcDataModel: Option[CalculationDataModel]){
  val breakdownNonEmpty: Boolean = calcDataModel.nonEmpty

  val hasBRTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtBRT > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtBRT > 0
    else false
  }
  val hasHRTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtHRT > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtHRT > 0
    else false
  }
  val hasARTSection: Boolean = {
    if (calcDataModel.nonEmpty)
      calcDataModel.get.payPensionsProfitAtART > 0 || calcDataModel.get.incomeTaxOnPayPensionsProfitAtART > 0
    else false
  }
  val hasNISection: Boolean = {
    if(calcDataModel.nonEmpty)
      calcDataModel.get.nicTotal > 0
    else false
  }
}

case class CalculationDataModel(
                                 incomeTaxYTD: BigDecimal,
                                 incomeTaxThisPeriod: BigDecimal,
//                                 payFromAllEmployments: Option[BigDecimal] = None,
//                                 benefitsAndExpensesReceived: Option[BigDecimal] = None,
//                                 allowableExpenses: Option[BigDecimal] = None,
//                                 payFromAllEmploymentsAfterExpenses: Option[BigDecimal] = None,
//                                 shareSchemes: Option[BigDecimal] = None,
                                 profitFromSelfEmployment: BigDecimal,
//                                 profitFromPartnerships: Option[BigDecimal] = None,
                                 profitFromUkLandAndProperty: BigDecimal,
//                                 dividendsFromForeignCompanies: Option[BigDecimal] = None,
//                                 foreignIncome: Option[BigDecimal] = None,
//                                 trustsAndEstates: Option[BigDecimal] = None,
//                                 interestReceivedFromUkBanksAndBuildingSocieties: Option[BigDecimal] = None,
//                                 dividendsFromUkCompanies: Option[BigDecimal] = None,
//                                 ukPensionsAndStateBenefits: Option[BigDecimal] = None,
//                                 gainsOnLifeInsurance: Option[BigDecimal] = None,
//                                 otherIncome: Option[BigDecimal] = None,
                                 totalIncomeReceived: BigDecimal,
//                                 paymentsIntoARetirementAnnuity: Option[BigDecimal] = None,
//                                 foreignTaxOnEstates: Option[BigDecimal] = None,
//                                 incomeTaxRelief: Option[BigDecimal] = None,
//                                 incomeTaxReliefReducedToMaximumAllowable: Option[BigDecimal] = None,
//                                 annuities: Option[BigDecimal] = None,
//                                 giftOfInvestmentsAndPropertyToCharity: Option[BigDecimal] = None,
                                 personalAllowance: BigDecimal,
//                                 marriageAllowanceTransfer: Option[BigDecimal] = None,
//                                 blindPersonAllowance: Option[BigDecimal] = None,
//                                 blindPersonSurplusAllowanceFromSpouse: Option[BigDecimal] = None,
//                                 incomeExcluded: Option[BigDecimal] = None,
//                                 totalIncomeAllowancesUsed: Option[BigDecimal] = None,
                                 totalIncomeOnWhichTaxIsDue: BigDecimal,
//                                 payPensionsExtender: Option[BigDecimal] = None,
//                                 giftExtender: Option[BigDecimal] = None,
//                                 extendedBR: Option[BigDecimal] = None,
                                 payPensionsProfitAtBRT: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtBRT: BigDecimal,
                                 payPensionsProfitAtHRT: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtHRT: BigDecimal,
                                 payPensionsProfitAtART: BigDecimal,
                                 incomeTaxOnPayPensionsProfitAtART: BigDecimal,
//                                 netPropertyFinanceCosts: Option[BigDecimal] = None,
//                                 interestReceivedAtStartingRate: Option[BigDecimal] = None,
//                                 incomeTaxOnInterestReceivedAtStartingRate: Option[BigDecimal] = None,
//                                 interestReceivedAtZeroRate: Option[BigDecimal] = None,
//                                 incomeTaxOnInterestReceivedAtZeroRate: Option[BigDecimal] = None,
//                                 interestReceivedAtBRT: Option[BigDecimal] = None,
//                                 incomeTaxOnInterestReceivedAtBRT: Option[BigDecimal] = None,
//                                 interestReceivedAtHRT: Option[BigDecimal] = None,
//                                 incomeTaxOnInterestReceivedAtHRT: Option[BigDecimal] = None,
//                                 interestReceivedAtART: Option[BigDecimal] = None,
//                                 incomeTaxOnInterestReceivedAtART: Option[BigDecimal] = None,
//                                 dividendsAtZeroRate: Option[BigDecimal] = None,
//                                 incomeTaxOnDividendsAtZeroRate: Option[BigDecimal] = None,
//                                 dividendsAtBRT: Option[BigDecimal] = None,
//                                 incomeTaxOnDividendsAtBRT: Option[BigDecimal] = None,
//                                 dividendsAtHRT: Option[BigDecimal] = None,
//                                 incomeTaxOnDividendsAtHRT: Option[BigDecimal] = None,
//                                 dividendsAtART: Option[BigDecimal] = None,
//                                 incomeTaxOnDividendsAtART: Option[BigDecimal] = None,
//                                 totalIncomeOnWhichTaxHasBeenCharged: Option[BigDecimal] = None,
//                                 taxOnOtherIncome: Option[BigDecimal] = None,
                                 incomeTaxDue: BigDecimal,
//                                 incomeTaxCharged: Option[BigDecimal] = None,
//                                 deficiencyRelief: Option[BigDecimal] = None,
//                                 topSlicingRelief: Option[BigDecimal] = None,
//                                 ventureCapitalTrustRelief: Option[BigDecimal] = None,
//                                 enterpriseInvestmentSchemeRelief: Option[BigDecimal] = None,
//                                 seedEnterpriseInvestmentSchemeRelief: Option[BigDecimal] = None,
//                                 communityInvestmentTaxRelief: Option[BigDecimal] = None,
//                                 socialInvestmentTaxRelief: Option[BigDecimal] = None,
//                                 maintenanceAndAlimonyPaid: Option[BigDecimal] = None,
//                                 marriedCouplesAllowance: Option[BigDecimal] = None,
//                                 marriedCouplesAllowanceRelief: Option[BigDecimal] = None,
//                                 surplusMarriedCouplesAllowance: Option[BigDecimal] = None,
//                                 surplusMarriedCouplesAllowanceRelief: Option[BigDecimal] = None,
//                                 notionalTaxFromLifePolicies: Option[BigDecimal] = None,
//                                 notionalTaxFromDividendsAndOtherIncome: Option[BigDecimal] = None,
//                                 foreignTaxCreditRelief: Option[BigDecimal] = None,
//                                 incomeTaxDueAfterAllowancesAndReliefs: Option[BigDecimal] = None,
//                                 giftAidPaymentsAmount: Option[BigDecimal] = None,
//                                 giftAidTaxDue: Option[BigDecimal] = None,
//                                 capitalGainsTaxDue: Option[BigDecimal] = None,
//                                 remittanceForNonDomiciles: Option[BigDecimal] = None,
//                                 highIncomeChildBenefitCharge: Option[BigDecimal] = None,
//                                 totalGiftAidTaxReduced: Option[BigDecimal] = None,
//                                 incomeTaxDueAfterGiftAidReduction: Option[BigDecimal] = None,
//                                 annuityAmount: Option[BigDecimal] = None,
//                                 taxDueOnAnnuity: Option[BigDecimal] = None,
//                                 taxCreditsOnDividendsFromUkCompanies: Option[BigDecimal] = None,
//                                 incomeTaxDueAfterDividendTaxCredits: Option[BigDecimal] = None,
//                                 nationalInsuranceContributionAmount: Option[BigDecimal] = None,
//                                 nationalInsuranceContributionCharge: Option[BigDecimal] = None,
//                                 nationalInsuranceContributionSupAmount: Option[BigDecimal] = None,
//                                 nationalInsuranceContributionSupCharge: Option[BigDecimal] = None,
//                                 totalClass4Charge: Option[BigDecimal] = None,
//                                 nationalInsuranceClass1Amount: Option[BigDecimal] = None,
//                                 nationalInsuranceClass2Amount: Option[BigDecimal] = None,
                                 nicTotal: BigDecimal,
//                                 underpaidTaxForPreviousYears: Option[BigDecimal] = None,
//                                 studentLoanRepayments: Option[BigDecimal] = None,
//                                 pensionChargesGross: Option[BigDecimal] = None,
//                                 pensionChargesTaxPaid: Option[BigDecimal] = None,
//                                 totalPensionSavingCharges: Option[BigDecimal] = None,
//                                 pensionLumpSumAmount: Option[BigDecimal] = None,
//                                 pensionLumpSumRate: Option[BigDecimal] = None,
//                                 statePensionLumpSumAmount: Option[BigDecimal] = None,
//                                 remittanceBasisChargeForNonDomiciles: Option[BigDecimal] = None,
//                                 additionalTaxDueOnPensions: Option[BigDecimal] = None,
//                                 additionalTaxReliefDueOnPensions: Option[BigDecimal] = None,
//                                 incomeTaxDueAfterPensionDeductions: Option[BigDecimal] = None,
//                                 employmentsPensionsAndBenefits: Option[BigDecimal] = None,
//                                 outstandingDebtCollectedThroughPaye: Option[BigDecimal] = None,
//                                 payeTaxBalance: Option[BigDecimal] = None,
//                                 cisAndTradingIncome: Option[BigDecimal] = None,
//                                 partnerships: Option[BigDecimal] = None,
//                                 ukLandAndPropertyTaxPaid: Option[BigDecimal] = None,
//                                 foreignIncomeTaxPaid: Option[BigDecimal] = None,
//                                 trustAndEstatesTaxPaid: Option[BigDecimal] = None,
//                                 overseasIncomeTaxPaid: Option[BigDecimal] = None,
//                                 interestReceivedTaxPaid: Option[BigDecimal] = None,
//                                 voidISAs: Option[BigDecimal] = None,
//                                 otherIncomeTaxPaid: Option[BigDecimal] = None,
//                                 underpaidTaxForPriorYear: Option[BigDecimal] = None,
//                                 totalTaxDeducted: Option[BigDecimal] = None,
//                                 incomeTaxOverpaid: Option[BigDecimal] = None,
//                                 incomeTaxDueAfterDeductions: Option[BigDecimal] = None,
//                                 propertyFinanceTaxDeduction: Option[BigDecimal] = None,
//                                 taxableCapitalGains: Option[BigDecimal] = None,
//                                 capitalGainAtEntrepreneurRate: Option[BigDecimal] = None,
//                                 incomeTaxOnCapitalGainAtEntrepreneurRate: Option[BigDecimal] = None,
//                                 capitalGrainsAtLowerRate: Option[BigDecimal] = None,
//                                 incomeTaxOnCapitalGainAtLowerRate: Option[BigDecimal] = None,
//                                 capitalGainAtHigherRate: Option[BigDecimal] = None,
//                                 incomeTaxOnCapitalGainAtHigherTax: Option[BigDecimal] = None,
//                                 capitalGainsTaxAdjustment: Option[BigDecimal] = None,
//                                 foreignTaxCreditReliefOnCapitalGains: Option[BigDecimal] = None,
//                                 liabilityFromOffShoreTrusts: Option[BigDecimal] = None,
//                                 taxOnGainsAlreadyCharged: Option[BigDecimal] = None,
//                                 totalCapitalGainsTax: Option[BigDecimal] = None,
//                                 incomeAndCapitalGainsTaxDue: Option[BigDecimal] = None,
//                                 taxRefundedInYear: Option[BigDecimal] = None,
//                                 unpaidTaxCalculatedForEarlierYears: Option[BigDecimal] = None,
//                                 marriageAllowanceTransferAmount: Option[BigDecimal] = None,
//                                 marriageAllowanceTransferRelief: Option[BigDecimal] = None,
//                                 marriageAllowanceTransferMaximumAllowable: Option[BigDecimal] = None,
//                                 nationalRegime: Option[String] = None,
//                                 allowance: Option[BigDecimal] = None,
//                                 limitBRT: Option[BigDecimal] = None,
//                                 limitHRT: Option[BigDecimal] = None,
                                 rateBRT: BigDecimal,
                                 rateHRT: BigDecimal,
                                 rateART: BigDecimal
//                                 limitAIA: Option[BigDecimal] = None,
//                                 allowanceBRT: Option[BigDecimal] = None,
//                                 interestAllowanceHRT: Option[BigDecimal] = None,
//                                 interestAllowanceBRT: Option[BigDecimal] = None,
//                                 dividendAllowance: Option[BigDecimal] = None,
//                                 dividendBRT: Option[BigDecimal] = None,
//                                 dividendHRT: Option[BigDecimal] = None,
//                                 dividendART: Option[BigDecimal] = None,
//                                 class2NICsLimit: Option[BigDecimal] = None,
//                                 class2NICsPerWeek: Option[BigDecimal] = None,
//                                 class4NICsLimitBR: Option[BigDecimal] = None,
//                                 class4NICsLimitHR: Option[BigDecimal] = None,
//                                 class4NICsBRT: Option[BigDecimal] = None,
//                                 class4NICsHRT: Option[BigDecimal] = None,
//                                 proportionAllowance: Option[BigDecimal] = None,
//                                 proportionLimitBRT: Option[BigDecimal] = None,
//                                 proportionLimitHRT: Option[BigDecimal] = None,
//                                 proportionalTaxDue: Option[BigDecimal] = None,
//                                 proportionInterestAllowanceBRT: Option[BigDecimal] = None,
//                                 proportionInterestAllowanceHRT: Option[BigDecimal] = None,
//                                 proportionDividendAllowance: Option[BigDecimal] = None,
//                                 proportionPayPensionsProfitAtART: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnPayPensionsProfitAtART: Option[BigDecimal] = None,
//                                 proportionPayPensionsProfitAtBRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnPayPensionsProfitAtBRT: Option[BigDecimal] = None,
//                                 proportionPayPensionsProfitAtHRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnPayPensionsProfitAtHRT: Option[BigDecimal] = None,
//                                 proportionInterestReceivedAtZeroRate: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnInterestReceivedAtZeroRate: Option[BigDecimal] = None,
//                                 proportionInterestReceivedAtBRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnInterestReceivedAtBRT: Option[BigDecimal] = None,
//                                 proportionInterestReceivedAtHRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnInterestReceivedAtHRT: Option[BigDecimal] = None,
//                                 proportionInterestReceivedAtART: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnInterestReceivedAtART: Option[BigDecimal] = None,
//                                 proportionDividendsAtZeroRate: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnDividendsAtZeroRate: Option[BigDecimal] = None,
//                                 proportionDividendsAtBRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnDividendsAtBRT: Option[BigDecimal] = None,
//                                 proportionDividendsAtHRT: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnDividendsAtHRT: Option[BigDecimal] = None,
//                                 proportionDividendsAtART: Option[BigDecimal] = None,
//                                 proportionIncomeTaxOnDividendsAtART: Option[BigDecimal] = None,
//                                 proportionClass2NICsLimit: Option[BigDecimal] = None,
//                                 proportionClass4NICsLimitBR: Option[BigDecimal] = None,
//                                 proportionClass4NICsLimitHR: Option[BigDecimal] = None,
//                                 proportionReducedAllowanceLimit: Option[BigDecimal] = None
                                 ) extends CalculationDataResponseModel

case class CalculationDataErrorModel(code: Int, message: String) extends CalculationDataResponseModel

object CalculationDataModel {
  implicit val format: OFormat[CalculationDataModel] = Json.format[CalculationDataModel]
}
object CalcDisplayModel {
  implicit val format: OFormat[CalcDisplayModel] = Json.format[CalcDisplayModel]
}

object CalculationDataErrorModel {
  implicit val format: OFormat[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}