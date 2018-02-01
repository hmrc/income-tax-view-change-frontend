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

package helpers

import java.time.LocalDate

import models._
import play.api.libs.json.{JsArray, JsValue, Json}
import utils.ImplicitDateFormatter

object IntegrationTestConstants extends ImplicitDateFormatter {

  val testDate = "2018-05-05".toLocalDate

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"
  val testUserName = "Albert Einstein"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"
  val testCalcId2 = "01234568"

  val testYear = "2018"
  val testYearPlusOne = "2019"
  val testYearInt = 2018
  val testYearPlusOneInt = 2019

  val testCalcType = "it"

  val testSelfEmploymentId = "ABC123456789"
  val otherTestSelfEmploymentId = "ABC123456780"

  object GetCalculationData {

    val calculationDataSuccessWithEoYModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 90500.00,
      totalTaxableIncome = 198500.00,
      personalAllowance = 11500.00,
      taxReliefs = 1000,
      additionalAllowances = 505.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 200000.00,
        ukProperty = 10000.00,
        bankBuildingSocietyInterest = 2000.00,
        ukDividends = 11000.00
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 20000.00,
          taxRate = 20.0,
          taxAmount = 4000.00
        ),
        higherBand = BandModel(
          taxableIncome = 100000.00,
          taxRate = 40.0,
          taxAmount = 40000.00
        ),
        additionalBand = BandModel(
          taxableIncome = 50000.00,
          taxRate = 45.0,
          taxAmount = 22500.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 1.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 20.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 500.0,
          taxRate = 20.0,
          taxAmount = 100.0
        ),
        higherBand = BandModel(
          taxableIncome = 1000.0,
          taxRate = 40.0,
          taxAmount = 400.0
        ),
        additionalBand = BandModel(
          taxableIncome = 479.0,
          taxRate = 45.0,
          taxAmount = 215.55
        )
      ),
      dividends = DividendsModel(
        allowance = 5000.0,
        basicBand = BandModel(
          taxableIncome = 1000.0,
          taxRate = 7.5,
          taxAmount = 75.0
        ),
        higherBand = BandModel(
          taxableIncome = 2000.0,
          taxRate = 37.5,
          taxAmount = 750.0
        ),
        additionalBand = BandModel(
          taxableIncome = 3000.0,
          taxRate = 38.1,
          taxAmount = 1143.0
        )
      ),
      nic = NicModel(
        class2 = 10000.00,
        class4 = 14000.00
      ),
      eoyEstimate = Some(EoyEstimate(25000.00))
    )

    val calculationDataSuccessModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 90500.00,
      totalTaxableIncome = 198500.00,
      personalAllowance = 11500.00,
      taxReliefs = 1000,
      additionalAllowances = 505.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 200000.00,
        ukProperty = 10000.00,
        bankBuildingSocietyInterest = 2000.00,
        ukDividends = 11000.00
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 20000.00,
          taxRate = 20.0,
          taxAmount = 4000.00
        ),
        higherBand = BandModel(
          taxableIncome = 100000.00,
          taxRate = 40.0,
          taxAmount = 40000.00
        ),
        additionalBand = BandModel(
          taxableIncome = 50000.00,
          taxRate = 45.0,
          taxAmount = 22500.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 1.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 20.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 500.0,
          taxRate = 20.0,
          taxAmount = 100.0
        ),
        higherBand = BandModel(
          taxableIncome = 1000.0,
          taxRate = 40.0,
          taxAmount = 400.0
        ),
        additionalBand = BandModel(
          taxableIncome = 479.0,
          taxRate = 45.0,
          taxAmount = 215.55
        )
      ),
      dividends = DividendsModel(
        allowance = 5000.0,
        basicBand = BandModel(
          taxableIncome = 1000.0,
          taxRate = 7.5,
          taxAmount = 75.0
        ),
        higherBand = BandModel(
          taxableIncome = 2000.0,
          taxRate = 37.5,
          taxAmount = 750.0
        ),
        additionalBand = BandModel(
          taxableIncome = 3000.0,
          taxRate = 38.1,
          taxAmount = 1143.0
        )
      ),
      nic = NicModel(
        class2 = 10000.00,
        class4 = 14000.00
      )
    )

    val calculationDataErrorModel = CalculationDataErrorModel(code = 500, message = "Calculation Error Model Response")

    val calculationDataSuccessWithEoyString =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "payFromAllEmployments": 0,
        | "benefitsAndExpensesReceived": 0,
        | "totalAllowancesAndDeductions": 505,
        | "allowableExpenses": 0,
        | "payFromAllEmploymentsAfterExpenses": 0,
        | "shareSchemes": 0,
        | "profitFromSelfEmployment": 200000,
        | "profitFromPartnerships": 0,
        | "profitFromUkLandAndProperty": 10000,
        | "dividendsFromForeignCompanies": 0,
        | "foreignIncome": 0,
        | "trustsAndEstates": 0,
        | "interestReceivedFromUkBanksAndBuildingSocieties": 2000,
        | "dividendsFromUkCompanies": 11000,
        | "ukPensionsAndStateBenefits": 0,
        | "gainsOnLifeInsurance": 0,
        | "otherIncome": 0,
        | "totalIncomeReceived": 230000,
        | "paymentsIntoARetirementAnnuity": 0,
        | "foreignTaxOnEstates": 0,
        | "incomeTaxRelief": 0,
        | "incomeTaxReliefReducedToMaximumAllowable": 0,
        | "annuities": 0,
        | "giftOfInvestmentsAndPropertyToCharity": 0,
        | "personalAllowance": 11500,
        | "marriageAllowanceTransfer": 0,
        | "blindPersonAllowance": 0,
        | "blindPersonSurplusAllowanceFromSpouse": 0,
        | "incomeExcluded": 0,
        | "totalIncomeAllowancesUsed": 0,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsExtender": 0,
        | "giftExtender": 0,
        | "extendedBR": 0,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "netPropertyFinanceCosts": 0,
        | "interestReceivedAtStartingRate": 1,
        | "incomeTaxOnInterestReceivedAtStartingRate": 0,
        | "interestReceivedAtZeroRate": 20,
        | "incomeTaxOnInterestReceivedAtZeroRate": 0,
        | "interestReceivedAtBRT": 500,
        | "incomeTaxOnInterestReceivedAtBRT": 100,
        | "interestReceivedAtHRT": 1000,
        | "incomeTaxOnInterestReceivedAtHRT": 400,
        | "interestReceivedAtART": 479,
        | "incomeTaxOnInterestReceivedAtART": 215.55,
        | "dividendsAtZeroRate": 0,
        | "incomeTaxOnDividendsAtZeroRate": 0,
        | "dividendsAtBRT": 1000,
        | "incomeTaxOnDividendsAtBRT": 75,
        | "dividendsAtHRT": 2000,
        | "incomeTaxOnDividendsAtHRT": 750,
        | "dividendsAtART": 3000,
        | "incomeTaxOnDividendsAtART": 1143,
        | "totalIncomeOnWhichTaxHasBeenCharged": 0,
        | "taxOnOtherIncome": 0,
        | "incomeTaxDue": 66500,
        | "incomeTaxCharged": 0,
        | "deficiencyRelief": 0,
        | "topSlicingRelief": 0,
        | "ventureCapitalTrustRelief": 0,
        | "enterpriseInvestmentSchemeRelief": 0,
        | "seedEnterpriseInvestmentSchemeRelief": 0,
        | "communityInvestmentTaxRelief": 0,
        | "socialInvestmentTaxRelief": 0,
        | "maintenanceAndAlimonyPaid": 0,
        | "marriedCouplesAllowance": 0,
        | "marriedCouplesAllowanceRelief": 0,
        | "surplusMarriedCouplesAllowance": 0,
        | "surplusMarriedCouplesAllowanceRelief": 0,
        | "notionalTaxFromLifePolicies": 0,
        | "notionalTaxFromDividendsAndOtherIncome": 0,
        | "foreignTaxCreditRelief": 0,
        | "incomeTaxDueAfterAllowancesAndReliefs": 0,
        | "giftAidPaymentsAmount": 0,
        | "giftAidTaxDue": 0,
        | "capitalGainsTaxDue": 0,
        | "remittanceForNonDomiciles": 0,
        | "highIncomeChildBenefitCharge": 0,
        | "totalGiftAidTaxReduced": 0,
        | "incomeTaxDueAfterGiftAidReduction": 0,
        | "annuityAmount": 0,
        | "taxDueOnAnnuity": 0,
        | "taxCreditsOnDividendsFromUkCompanies": 0,
        | "incomeTaxDueAfterDividendTaxCredits": 0,
        | "nationalInsuranceContributionAmount": 0,
        | "nationalInsuranceContributionCharge": 0,
        | "nationalInsuranceContributionSupAmount": 0,
        | "nationalInsuranceContributionSupCharge": 0,
        | "totalClass4Charge": 14000,
        | "nationalInsuranceClass1Amount": 0,
        | "nationalInsuranceClass2Amount": 10000,
        | "nicTotal": 24000,
        | "underpaidTaxForPreviousYears": 0,
        | "studentLoanRepayments": 0,
        | "pensionChargesGross": 0,
        | "pensionChargesTaxPaid": 0,
        | "totalPensionSavingCharges": 0,
        | "pensionLumpSumAmount": 0,
        | "pensionLumpSumRate": 0,
        | "statePensionLumpSumAmount": 0,
        | "remittanceBasisChargeForNonDomiciles": 0,
        | "additionalTaxDueOnPensions": 0,
        | "additionalTaxReliefDueOnPensions": 0,
        | "incomeTaxDueAfterPensionDeductions": 0,
        | "employmentsPensionsAndBenefits": 0,
        | "outstandingDebtCollectedThroughPaye": 0,
        | "payeTaxBalance": 0,
        | "cisAndTradingIncome": 0,
        | "partnerships": 0,
        | "ukLandAndPropertyTaxPaid": 0,
        | "foreignIncomeTaxPaid": 0,
        | "trustAndEstatesTaxPaid": 0,
        | "overseasIncomeTaxPaid": 0,
        | "interestReceivedTaxPaid": 0,
        | "voidISAs": 0,
        | "otherIncomeTaxPaid": 0,
        | "underpaidTaxForPriorYear": 0,
        | "totalTaxDeducted": 0,
        | "incomeTaxOverpaid": 0,
        | "incomeTaxDueAfterDeductions": 0,
        | "propertyFinanceTaxDeduction": 0,
        | "taxableCapitalGains": 0,
        | "capitalGainAtEntrepreneurRate": 0,
        | "incomeTaxOnCapitalGainAtEntrepreneurRate": 0,
        | "capitalGrainsAtLowerRate": 0,
        | "incomeTaxOnCapitalGainAtLowerRate": 0,
        | "capitalGainAtHigherRate": 0,
        | "incomeTaxOnCapitalGainAtHigherTax": 0,
        | "capitalGainsTaxAdjustment": 0,
        | "foreignTaxCreditReliefOnCapitalGains": 0,
        | "liabilityFromOffShoreTrusts": 0,
        | "taxOnGainsAlreadyCharged": 0,
        | "totalCapitalGainsTax": 0,
        | "incomeAndCapitalGainsTaxDue": 0,
        | "taxRefundedInYear": 0,
        | "unpaidTaxCalculatedForEarlierYears": 0,
        | "marriageAllowanceTransferAmount": 0,
        | "marriageAllowanceTransferRelief": 0,
        | "marriageAllowanceTransferMaximumAllowable": 0,
        | "nationalRegime": "0",
        | "allowance": 0,
        | "limitBRT": 0,
        | "limitHRT": 0,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
        | "limitAIA": 0,
        | "limitAIA": 0,
        | "allowanceBRT": 0,
        | "interestAllowanceHRT": 0,
        | "interestAllowanceBRT": 0,
        | "dividendAllowance": 5000,
        | "dividendBRT": 7.5,
        | "dividendHRT": 37.5,
        | "dividendART": 38.1,
        | "class2NICsLimit": 0,
        | "class2NICsPerWeek": 0,
        | "class4NICsLimitBR": 0,
        | "class4NICsLimitHR": 0,
        | "class4NICsBRT": 0,
        | "class4NICsHRT": 0,
        | "proportionAllowance": 11500,
        | "proportionLimitBRT": 0,
        | "proportionLimitHRT": 0,
        | "proportionalTaxDue": 0,
        | "proportionInterestAllowanceBRT": 0,
        | "proportionInterestAllowanceHRT": 0,
        | "proportionDividendAllowance": 0,
        | "proportionPayPensionsProfitAtART": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtART": 0,
        | "proportionPayPensionsProfitAtBRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtBRT": 0,
        | "proportionPayPensionsProfitAtHRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtHRT": 0,
        | "proportionInterestReceivedAtZeroRate": 0,
        | "proportionIncomeTaxOnInterestReceivedAtZeroRate": 0,
        | "proportionInterestReceivedAtBRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtBRT": 0,
        | "proportionInterestReceivedAtHRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtHRT": 0,
        | "proportionInterestReceivedAtART": 0,
        | "proportionIncomeTaxOnInterestReceivedAtART": 0,
        | "proportionDividendsAtZeroRate": 0,
        | "proportionIncomeTaxOnDividendsAtZeroRate": 0,
        | "proportionDividendsAtBRT": 0,
        | "proportionIncomeTaxOnDividendsAtBRT": 0,
        | "proportionDividendsAtHRT": 0,
        | "proportionIncomeTaxOnDividendsAtHRT": 0,
        | "proportionDividendsAtART": 0,
        | "proportionIncomeTaxOnDividendsAtART": 0,
        | "proportionClass2NICsLimit": 0,
        | "proportionClass4NICsLimitBR": 0,
        | "proportionClass4NICsLimitHR": 0,
        | "proportionReducedAllowanceLimit": 0,
        | "eoyEstimate": {
        |        "selfEmployment": [
        |            {
        |                "id": "selfEmploymentId1",
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            },
        |            {
        |                "id": "selfEmploymentId2",
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            }
        |        ],
        |        "ukProperty": [
        |            {
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            }
        |        ],
        |        "totalTaxableIncome": 89999999.99,
        |        "incomeTaxAmount": 89999999.99,
        |        "nic2": 89999999.99,
        |        "nic4": 89999999.99,
        |        "totalNicAmount": 9999999.99,
        |        "incomeTaxNicAmount": 66000.00
        |    }
        |}
      """.stripMargin

    val calculationDataSuccessString =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "payFromAllEmployments": 0,
        | "totalAllowancesAndDeductions": 505,
        | "benefitsAndExpensesReceived": 0,
        | "allowableExpenses": 0,
        | "payFromAllEmploymentsAfterExpenses": 0,
        | "shareSchemes": 0,
        | "profitFromSelfEmployment": 200000,
        | "profitFromPartnerships": 0,
        | "profitFromUkLandAndProperty": 10000,
        | "dividendsFromForeignCompanies": 0,
        | "foreignIncome": 0,
        | "trustsAndEstates": 0,
        | "interestReceivedFromUkBanksAndBuildingSocieties": 2000,
        | "dividendsFromUkCompanies": 11000,
        | "ukPensionsAndStateBenefits": 0,
        | "gainsOnLifeInsurance": 0,
        | "otherIncome": 0,
        | "totalIncomeReceived": 230000,
        | "paymentsIntoARetirementAnnuity": 0,
        | "foreignTaxOnEstates": 0,
        | "incomeTaxRelief": 0,
        | "incomeTaxReliefReducedToMaximumAllowable": 0,
        | "annuities": 0,
        | "giftOfInvestmentsAndPropertyToCharity": 0,
        | "personalAllowance": 11500,
        | "marriageAllowanceTransfer": 0,
        | "blindPersonAllowance": 0,
        | "blindPersonSurplusAllowanceFromSpouse": 0,
        | "incomeExcluded": 0,
        | "totalIncomeAllowancesUsed": 0,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsExtender": 0,
        | "giftExtender": 0,
        | "extendedBR": 0,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "netPropertyFinanceCosts": 0,
        | "interestReceivedAtStartingRate": 1,
        | "incomeTaxOnInterestReceivedAtStartingRate": 0,
        | "interestReceivedAtZeroRate": 20,
        | "incomeTaxOnInterestReceivedAtZeroRate": 0,
        | "interestReceivedAtBRT": 500,
        | "incomeTaxOnInterestReceivedAtBRT": 100,
        | "interestReceivedAtHRT": 1000,
        | "incomeTaxOnInterestReceivedAtHRT": 400,
        | "interestReceivedAtART": 479,
        | "incomeTaxOnInterestReceivedAtART": 215.55,
        | "dividendsAtZeroRate": 0,
        | "incomeTaxOnDividendsAtZeroRate": 0,
        | "dividendsAtBRT": 1000,
        | "incomeTaxOnDividendsAtBRT": 75,
        | "dividendsAtHRT": 2000,
        | "incomeTaxOnDividendsAtHRT": 750,
        | "dividendsAtART": 3000,
        | "incomeTaxOnDividendsAtART": 1143,
        | "totalIncomeOnWhichTaxHasBeenCharged": 0,
        | "taxOnOtherIncome": 0,
        | "incomeTaxDue": 66500,
        | "incomeTaxCharged": 0,
        | "deficiencyRelief": 0,
        | "topSlicingRelief": 0,
        | "ventureCapitalTrustRelief": 0,
        | "enterpriseInvestmentSchemeRelief": 0,
        | "seedEnterpriseInvestmentSchemeRelief": 0,
        | "communityInvestmentTaxRelief": 0,
        | "socialInvestmentTaxRelief": 0,
        | "maintenanceAndAlimonyPaid": 0,
        | "marriedCouplesAllowance": 0,
        | "marriedCouplesAllowanceRelief": 0,
        | "surplusMarriedCouplesAllowance": 0,
        | "surplusMarriedCouplesAllowanceRelief": 0,
        | "notionalTaxFromLifePolicies": 0,
        | "notionalTaxFromDividendsAndOtherIncome": 0,
        | "foreignTaxCreditRelief": 0,
        | "incomeTaxDueAfterAllowancesAndReliefs": 0,
        | "giftAidPaymentsAmount": 0,
        | "giftAidTaxDue": 0,
        | "capitalGainsTaxDue": 0,
        | "remittanceForNonDomiciles": 0,
        | "highIncomeChildBenefitCharge": 0,
        | "totalGiftAidTaxReduced": 0,
        | "incomeTaxDueAfterGiftAidReduction": 0,
        | "annuityAmount": 0,
        | "taxDueOnAnnuity": 0,
        | "taxCreditsOnDividendsFromUkCompanies": 0,
        | "incomeTaxDueAfterDividendTaxCredits": 0,
        | "nationalInsuranceContributionAmount": 0,
        | "nationalInsuranceContributionCharge": 0,
        | "nationalInsuranceContributionSupAmount": 0,
        | "nationalInsuranceContributionSupCharge": 0,
        | "totalClass4Charge": 14000,
        | "nationalInsuranceClass1Amount": 0,
        | "nationalInsuranceClass2Amount": 10000,
        | "nicTotal": 24000,
        | "underpaidTaxForPreviousYears": 0,
        | "studentLoanRepayments": 0,
        | "pensionChargesGross": 0,
        | "pensionChargesTaxPaid": 0,
        | "totalPensionSavingCharges": 0,
        | "pensionLumpSumAmount": 0,
        | "pensionLumpSumRate": 0,
        | "statePensionLumpSumAmount": 0,
        | "remittanceBasisChargeForNonDomiciles": 0,
        | "additionalTaxDueOnPensions": 0,
        | "additionalTaxReliefDueOnPensions": 0,
        | "incomeTaxDueAfterPensionDeductions": 0,
        | "employmentsPensionsAndBenefits": 0,
        | "outstandingDebtCollectedThroughPaye": 0,
        | "payeTaxBalance": 0,
        | "cisAndTradingIncome": 0,
        | "partnerships": 0,
        | "ukLandAndPropertyTaxPaid": 0,
        | "foreignIncomeTaxPaid": 0,
        | "trustAndEstatesTaxPaid": 0,
        | "overseasIncomeTaxPaid": 0,
        | "interestReceivedTaxPaid": 0,
        | "voidISAs": 0,
        | "otherIncomeTaxPaid": 0,
        | "underpaidTaxForPriorYear": 0,
        | "totalTaxDeducted": 0,
        | "incomeTaxOverpaid": 0,
        | "incomeTaxDueAfterDeductions": 0,
        | "propertyFinanceTaxDeduction": 0,
        | "taxableCapitalGains": 0,
        | "capitalGainAtEntrepreneurRate": 0,
        | "incomeTaxOnCapitalGainAtEntrepreneurRate": 0,
        | "capitalGrainsAtLowerRate": 0,
        | "incomeTaxOnCapitalGainAtLowerRate": 0,
        | "capitalGainAtHigherRate": 0,
        | "incomeTaxOnCapitalGainAtHigherTax": 0,
        | "capitalGainsTaxAdjustment": 0,
        | "foreignTaxCreditReliefOnCapitalGains": 0,
        | "liabilityFromOffShoreTrusts": 0,
        | "taxOnGainsAlreadyCharged": 0,
        | "totalCapitalGainsTax": 0,
        | "incomeAndCapitalGainsTaxDue": 0,
        | "taxRefundedInYear": 0,
        | "unpaidTaxCalculatedForEarlierYears": 0,
        | "marriageAllowanceTransferAmount": 0,
        | "marriageAllowanceTransferRelief": 0,
        | "marriageAllowanceTransferMaximumAllowable": 0,
        | "nationalRegime": "0",
        | "allowance": 0,
        | "limitBRT": 0,
        | "limitHRT": 0,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
        | "limitAIA": 0,
        | "limitAIA": 0,
        | "allowanceBRT": 0,
        | "interestAllowanceHRT": 0,
        | "interestAllowanceBRT": 0,
        | "dividendAllowance": 5000,
        | "dividendBRT": 7.5,
        | "dividendHRT": 37.5,
        | "dividendART": 38.1,
        | "class2NICsLimit": 0,
        | "class2NICsPerWeek": 0,
        | "class4NICsLimitBR": 0,
        | "class4NICsLimitHR": 0,
        | "class4NICsBRT": 0,
        | "class4NICsHRT": 0,
        | "proportionAllowance": 11500,
        | "proportionLimitBRT": 0,
        | "proportionLimitHRT": 0,
        | "proportionalTaxDue": 0,
        | "proportionInterestAllowanceBRT": 0,
        | "proportionInterestAllowanceHRT": 0,
        | "proportionDividendAllowance": 0,
        | "proportionPayPensionsProfitAtART": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtART": 0,
        | "proportionPayPensionsProfitAtBRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtBRT": 0,
        | "proportionPayPensionsProfitAtHRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtHRT": 0,
        | "proportionInterestReceivedAtZeroRate": 0,
        | "proportionIncomeTaxOnInterestReceivedAtZeroRate": 0,
        | "proportionInterestReceivedAtBRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtBRT": 0,
        | "proportionInterestReceivedAtHRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtHRT": 0,
        | "proportionInterestReceivedAtART": 0,
        | "proportionIncomeTaxOnInterestReceivedAtART": 0,
        | "proportionDividendsAtZeroRate": 0,
        | "proportionIncomeTaxOnDividendsAtZeroRate": 0,
        | "proportionDividendsAtBRT": 0,
        | "proportionIncomeTaxOnDividendsAtBRT": 0,
        | "proportionDividendsAtHRT": 0,
        | "proportionIncomeTaxOnDividendsAtHRT": 0,
        | "proportionDividendsAtART": 0,
        | "proportionIncomeTaxOnDividendsAtART": 0,
        | "proportionClass2NICsLimit": 0,
        | "proportionClass4NICsLimitBR": 0,
        | "proportionClass4NICsLimitHR": 0,
        | "proportionReducedAllowanceLimit": 0
        |}
      """.stripMargin

  }

  object GetBusinessDetails {
    def successResponse(selfEmploymentId: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$selfEmploymentId",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
          ]
          """.stripMargin)

    def otherSuccessResponse(selfEmploymentId: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$selfEmploymentId",
              "accountingPeriod":{"start":"2018-01-01","end":"2018-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2018-01-01",
              "cessationDate":"2018-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
          ]
          """.stripMargin)

    def multipleSuccessResponse(id1: String, id2: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$id1",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-06",
              "cessationDate":"2017-12-31",
              "tradingName":"firstBusiness",
              "businessDescription":"a first business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            },
            {
              "id": "$id2",
              "accountingPeriod":{"start":"2018-01-01","end":"2018-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"secondBusiness",
              "businessDescription":"a second business",
              "businessAddressLineOne":"742 Evergreen Terrace",
              "businessAddressLineTwo":"Springfield",
              "businessAddressLineThree":"Oregon",
              "businessAddressLineFour":"USA",
              "businessPostcode":"51MP 50N5"
            }
          ]
          """.stripMargin)

    def emptyBusinessDetailsResponse(): JsValue = JsArray()

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(s"""
                    |{
                    |   "code": "$code",
                    |   "reason":"$reason"
                    |}
      """.stripMargin)
  }

  object GetPropertyDetails {
    def successResponse(): JsValue =
      Json.parse(
        s"""{}"""
      )
  }

  object GetReportDeadlinesData {
    def successResponse(obligationsModel: ReportDeadlinesModel): JsValue = {
      Json.toJson(obligationsModel)
    }

    def emptyResponse(): JsValue = {
      Json.parse(
        """[]"""
      )
    }

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(
        s"""
           |{
           |  "code": $code,
           |  "reason": $reason
           |}
         """.stripMargin)

    val multipleReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      ), ReportDeadlineModel(
        start = "2017-07-06",
        end = "2017-10-05",
        due = LocalDate.now().plusDays(1),
        met = false
      ), ReportDeadlineModel(
        start = "2017-10-06",
        end = "2018-01-05",
        due = LocalDate.now().minusDays(1),
        met = false
      ))
    )

    val multipleReceivedOpenReportDeadlinesModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2016-04-01",
        end = "2016-06-30",
        due = "2016-07-31",
        met = true
      ), ReportDeadlineModel(
        start = "2016-07-01",
        end = "2016-09-30",
        due = LocalDate.now().minusDays(309),
        met = true
      ), ReportDeadlineModel(
        start = "2016-10-01",
        end = "2016-12-31",
        due = LocalDate.now().minusDays(217),
        met = true
      ), ReportDeadlineModel(
        start = "2017-01-01",
        end = "2017-03-31",
        due = LocalDate.now().minusDays(128),
        met = false
      ), ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = LocalDate.now().minusDays(36),
        met = false
      ), ReportDeadlineModel(
        start = "2017-07-01",
        end = "2017-09-30",
        due = LocalDate.now().plusDays(30),
        met = false
      ),ReportDeadlineModel(
        start = "2017-10-01",
        end = "2018-01-31",
        due = LocalDate.now().plusDays(146),
        met = false),
      ReportDeadlineModel(
        start = "2017-11-01",
        end = "2018-02-01",
        due = LocalDate.now().plusDays(174),
        met = false)
    ))

    val singleReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      )
    ))

    val otherReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().minusDays(1),
        met = true
      )
    ))

    val singleObligationOverdueModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().minusDays(1),
        met = false
      )
    ))

    val singleObligationPlusYearOpenModel = ReportDeadlinesModel(List(ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().plusYears(1),
        met = false
      )
    ))



    val emptyModel = ReportDeadlinesModel(List())
  }

  object GetStatementsData {
//    def successResponse(statementModel: TransactionModelWithYear): JsValue = {
//      Json.toJson(statementModel)
//    }

    def emptyResponse(): JsValue = {
      Json.parse(
        """[]"""
      )
    }

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(
        s"""
           |{
           |  "code": $code,
           |  "reason": $reason
           |}
         """.stripMargin)

    val charge2018: SubItemModel = SubItemModel(
      subItem = Some("000"),
      dueDate = Some("2019-1-31".toLocalDate),
      amount = Some(1000.00),
      clearingDate = None,
      clearingReason = None,
      outgoingPaymentMethod = None,
      paymentLock = None,
      clearingLock = None,
      interestLock = None,
      dunningLock = None,
      returnFlag = None,
      paymentReference = None,
      paymentAmount = None,
      paymentMethod = None,
      paymentLot = None,
      paymentLotItem = None,
      clearingSAPDocument = None,
      statisticalDocument = None,
      returnReason = None,
      promiseToPay = None
    )

    val charge2019: SubItemModel = SubItemModel(
      subItem = Some("000"),
      dueDate = Some("2020-1-31".toLocalDate),
      amount = Some(7500.00),
      clearingDate = None,
      clearingReason = None,
      outgoingPaymentMethod = None,
      paymentLock = None,
      clearingLock = None,
      interestLock = None,
      dunningLock = None,
      returnFlag = None,
      paymentReference = None,
      paymentAmount = None,
      paymentMethod = None,
      paymentLot = None,
      paymentLotItem = None,
      clearingSAPDocument = None,
      statisticalDocument = None,
      returnReason = None,
      promiseToPay = None
    )

    val payment2019: SubItemModel = SubItemModel(
      subItem = Some("001"),
      dueDate = None,
      amount = None,
      clearingDate = Some("2019-8-6".toLocalDate),
      clearingReason = None,
      outgoingPaymentMethod = None,
      paymentLock = None,
      clearingLock = None,
      interestLock = None,
      dunningLock = None,
      returnFlag = None,
      paymentReference = Some("aPaymentReference"),
      paymentAmount = Some(500.00),
      paymentMethod = None,
      paymentLot = None,
      paymentLotItem = None,
      clearingSAPDocument = None,
      statisticalDocument = None,
      returnReason = None,
      promiseToPay = None
    )

    val otherPayment2019: SubItemModel = SubItemModel(
      subItem = Some("002"),
      dueDate = None,
      amount = None,
      clearingDate = Some("2019-8-7".toLocalDate),
      clearingReason = None,
      outgoingPaymentMethod = None,
      paymentLock = None,
      clearingLock = None,
      interestLock = None,
      dunningLock = None,
      returnFlag = None,
      paymentReference = Some("aPaymentReference"),
      paymentAmount = Some(250.00),
      paymentMethod = None,
      paymentLot = None,
      paymentLotItem = None,
      clearingSAPDocument = None,
      statisticalDocument = None,
      returnReason = None,
      promiseToPay = None
    )

    val singleChargeTransactionModel = TransactionModel(
      chargeType = Some(""),
      mainType = Some(""),
      periodKey = Some(""),
      periodKeyDescription = Some(""),
      taxPeriodFrom = Some("2017-4-6".toLocalDate),
      taxPeriodTo = Some("2018-4-5".toLocalDate),
      businessPartner = Some(""),
      contractAccountCategory = Some(""),
      contractAccount = Some(""),
      contractObjectType = Some(""),
      contractObject = Some(""),
      sapDocumentNumber = Some(""),
      sapDocumentNumberItem = Some(""),
      chargeReference = Some(""),
      mainTransaction = Some(""),
      subTransaction = Some(""),
      originalAmount = Some(1000.00),
      outstandingAmount = Some(1000.00),
      clearedAmount = Some(0.00),
      accruedInterest = Some(0.00),
      items = Seq(charge2018)
    )

    val singleCharge2PaymentsTransactionModel = TransactionModel(
      chargeType = Some(""),
      mainType = Some(""),
      periodKey = Some(""),
      periodKeyDescription = Some(""),
      taxPeriodFrom = Some("2018-4-6".toLocalDate),
      taxPeriodTo = Some("2019-4-5".toLocalDate),
      businessPartner = Some(""),
      contractAccountCategory = Some(""),
      contractAccount = Some(""),
      contractObjectType = Some(""),
      contractObject = Some(""),
      sapDocumentNumber = Some(""),
      sapDocumentNumberItem = Some(""),
      chargeReference = Some(""),
      mainTransaction = Some(""),
      subTransaction = Some(""),
      originalAmount = Some(7500.00),
      outstandingAmount = Some(6750.00),
      clearedAmount = Some(750.00),
      accruedInterest = Some(0.00),
      items = Seq(charge2019, payment2019, otherPayment2019)
    )

    val singleFinancialTransactionsModel = FinancialTransactionsModel(
      idType = "",
      idNumber = "",
      regimeType = "",
      processingDate = "2017-03-07T09:30:00.000Z".toZonedDateTime,
      financialTransactions = Seq(singleChargeTransactionModel)
    )

    val singleFTModel1charge2payments = FinancialTransactionsModel(
      idType = "",
      idNumber = "",
      regimeType = "",
      processingDate = "2017-03-07T09:30:00.000Z".toZonedDateTime,
      financialTransactions = Seq(singleChargeTransactionModel, singleCharge2PaymentsTransactionModel)
    )
  }
}
