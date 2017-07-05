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

package assets

import auth.MtdItUser
import models._
import play.api.http.Status
import play.api.libs.json.Json
import utils.ImplicitDateFormatter

object TestConstants extends ImplicitDateFormatter {

  val testMtditid = "XAIT0000123456"
  val testNino = "AB123456C"
  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino)
  val testSelfEmploymentId = "XA00001234"
  val testTaxCalculationId = "CALCID"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorMessage = "Dummy Error Message"

  object BusinessDetails {

    val testBusinessAccountingPeriod = AccountingPeriodModel(start = "2017-6-1", end = "2018-5-30")
    val testTradeName = "business"

    val business1 = BusinessModel(
      id = testSelfEmploymentId,
      accountingPeriod = testBusinessAccountingPeriod,
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = Some("2017-12-31"),
      tradingName = testTradeName,
      businessDescription = Some("a business"),
      businessAddressLineOne = Some("64 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )
    val business2 = BusinessModel(
      id = "5678",
      accountingPeriod = AccountingPeriodModel(start = "2017-1-1", end = "2017-12-31"),
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = Some("2017-12-31"),
      tradingName = "otherBusiness",
      businessDescription = Some("some business"),
      businessAddressLineOne = Some("65 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )

    val businessesSuccessResponse = List(business1, business2)
    val businessesSuccessModel = BusinessDetailsModel(businessesSuccessResponse)
    val businessSuccessEmptyModel = BusinessDetailsModel(List.empty)
    val businessSuccessString: String =
      s"""
          {
             "business":[
                {
                   "id":"$testSelfEmploymentId",
                   "accountingPeriod":{
                     "start":"${testBusinessAccountingPeriod.start}",
                     "end":"${testBusinessAccountingPeriod.end}"
                   },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "cessationDate":"2017-12-31",
                   "tradingName":"$testTradeName",
                   "businessDescription":"a business",
                   "businessAddressLineOne":"64 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                },
                {
                   "id":"5678",
                   "accountingPeriod":{
                      "start":"2017-01-01",
                      "end":"2017-12-31"
                   },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "cessationDate":"2017-12-31",
                   "tradingName":"otherBusiness",
                   "businessDescription":"some business",
                   "businessAddressLineOne":"65 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                }
             ]
          }
      """.stripMargin
    val businessSuccessJson = Json.parse(businessSuccessString)


    val businessErrorModel = BusinessDetailsErrorModel(testErrorStatus, testErrorMessage)
    val businessErrorString =
      s"""
        |{
        |  "code":$testErrorStatus,
        |  "message":"$testErrorMessage"
        |}
      """.stripMargin
    val businessListErrorJson = Json.parse(businessErrorString)

    val businessIncomeModel = BusinessIncomeModel(testSelfEmploymentId, testBusinessAccountingPeriod, testTradeName)
    val businessIncomeModelAlignedTaxYear =
      BusinessIncomeModel(testSelfEmploymentId, AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5"), testTradeName)
  }

  object PropertyIncome {
    val propertyIncomeModel = PropertyIncomeModel(
      accountingPeriod = AccountingPeriodModel("2017-04-06", "2018-04-05")
    )
  }

  object PropertyDetails {
    val testPropertyAccountingPeriod = AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5")
    val propertySuccessModel = PropertyDetailsModel(testPropertyAccountingPeriod)
    val propertyErrorModel = PropertyDetailsErrorModel(testErrorStatus, testErrorMessage)
  }

  object Estimates {

    val testYear = 2018
    val testCalcType = "it"

    val lastTaxCalcSuccess = LastTaxCalculation(
      calcID = testTaxCalculationId,
      calcTimestamp = "2017-07-06T12:34:56.789Z",
      calcAmount = 543.21
    )

    val lastTaxCalcError = LastTaxCalculationError(testErrorStatus, testErrorMessage)
  }

  object Obligations {

    def fakeObligationsModel(m: ObligationModel): ObligationModel = new ObligationModel(m.start,m.end,m.due,m.met) {
      override def currentTime() = "2017-10-31"
    }

    val receivedObligation = fakeObligationsModel(ObligationModel(
      start = "2017-04-01",
      end = "2017-6-30",
      due = "2017-7-31",
      met = true
    ))

    val overdueObligation = fakeObligationsModel(ObligationModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-30",
      met = false
    ))

    val openObligation = fakeObligationsModel(ObligationModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-31",
      met = false
    ))

    val obligationsDataSuccessModel = ObligationsModel(List(receivedObligation, overdueObligation, openObligation))
    val obligationsDataSuccessString =
      """
        |{
        |  "obligations": [
        |    {
        |      "start": "2017-04-01",
        |      "end": "2017-06-30",
        |      "due": "2017-07-31",
        |      "met": true
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-30",
        |      "met": false
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-31",
        |      "met": false
        |    }
        |  ]
        |}
      """.stripMargin
    val obligationsDataSuccessJson = Json.parse(obligationsDataSuccessString)

    val obligationsDataErrorModel = ObligationsErrorModel(testErrorStatus, testErrorMessage)
    val obligationsDataErrorString =
      s"""
        |{
        |  "code":$testErrorStatus,
        |  "message":"$testErrorMessage"
        |}
      """.stripMargin
    val obligationsDataErrorJson = Json.parse(obligationsDataErrorString)

  }

  object IncomeSourceDetails {

    //Outputs
    val bothIncomeSourceSuccess = IncomeSourcesModel(Some(BusinessDetails.businessIncomeModel), Some(PropertyIncome.propertyIncomeModel))
    val businessIncomeSourceSuccess = IncomeSourcesModel(Some(BusinessDetails.businessIncomeModel), None)
    val propertyIncomeSourceSuccess = IncomeSourcesModel(None, Some(PropertyIncome.propertyIncomeModel))
    val noIncomeSourceSuccess = IncomeSourcesModel(None, None)

  }

  object CalcBreakdown {
    val calculationDataSuccessModel = CalculationDataModel(incomeTaxYTD = Some(90500),
      incomeTaxThisPeriod = Some(2000),
      profitFromSelfEmployment = Some(200000),
      profitFromUkLandAndProperty = Some(10000),
      totalIncomeReceived = Some(230000),
      personalAllowance = Some(11500),
      totalIncomeOnWhichTaxIsDue = Some(198500),
      payPensionsProfitAtBRT = Some(20000),
      incomeTaxOnPayPensionsProfitAtBRT = Some(4000),
      payPensionsProfitAtHRT = Some(100000),
      incomeTaxOnPayPensionsProfitAtHRT = Some(40000),
      payPensionsProfitAtART = Some(50000),
      incomeTaxOnPayPensionsProfitAtART = Some(22500),
      incomeTaxDue = Some(66500),
      nicTotal = Some(24000),
      rateBRT = Some(20),
      rateHRT = Some(40),
      rateART = Some(45)
    )


    val calculationDataSuccessString =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "profitFromSelfEmployment": 200000,
        | "profitFromUkLandAndProperty": 10000,
        | "totalIncomeReceived": 230000,
        | "personalAllowance": 11500,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "incomeTaxDue": 66500,
        | "nicTotal": 24000,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45
        |}
      """.stripMargin


    val calculationDataFullString =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "payFromAllEmployments": 888,
        | "benefitsAndExpensesReceived": 888,
        | "allowableExpenses": 888,
        | "payFromAllEmploymentsAfterExpenses": 888,
        | "shareSchemes": 888,
        | "profitFromSelfEmployment": 200000,
        | "profitFromPartnerships": 888,
        | "profitFromUkLandAndProperty": 10000,
        | "dividendsFromForeignCompanies": 888,
        | "foreignIncome": 888,
        | "trustsAndEstates": 888,
        | "interestReceivedFromUkBanksAndBuildingSocieties": 888,
        | "dividendsFromUkCompanies": 888,
        | "ukPensionsAndStateBenefits": 888,
        | "gainsOnLifeInsurance": 888,
        | "otherIncome": 888,
        | "totalIncomeReceived": 230000,
        | "paymentsIntoARetirementAnnuity": 888,
        | "foreignTaxOnEstates": 888,
        | "incomeTaxRelief": 888,
        | "incomeTaxReliefReducedToMaximumAllowable": 888,
        | "annuities": 888,
        | "giftOfInvestmentsAndPropertyToCharity": 888,
        | "personalAllowance": 11500,
        | "marriageAllowanceTransfer": 888,
        | "blindPersonAllowance": 888,
        | "blindPersonSurplusAllowanceFromSpouse": 888,
        | "incomeExcluded": 888,
        | "totalIncomeAllowancesUsed": 888,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsExtender": 888,
        | "giftExtender": 888,
        | "extendedBR": 888,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "netPropertyFinanceCosts": 888,
        | "interestReceivedAtStartingRate": 888,
        | "incomeTaxOnInterestReceivedAtStartingRate": 888,
        | "interestReceivedAtZeroRate": 888,
        | "incomeTaxOnInterestReceivedAtZeroRate": 888,
        | "interestReceivedAtBRT": 888,
        | "incomeTaxOnInterestReceivedAtBRT": 888,
        | "interestReceivedAtHRT": 888,
        | "incomeTaxOnInterestReceivedAtHRT": 888,
        | "interestReceivedAtART": 888,
        | "incomeTaxOnInterestReceivedAtART": 888,
        | "dividendsAtZeroRate": 888,
        | "incomeTaxOnDividendsAtZeroRate": 888,
        | "dividendsAtBRT": 888,
        | "incomeTaxOnDividendsAtBRT": 888,
        | "dividendsAtHRT": 888,
        | "incomeTaxOnDividendsAtHRT": 888,
        | "dividendsAtART": 888,
        | "incomeTaxOnDividendsAtART": 888,
        | "totalIncomeOnWhichTaxHasBeenCharged": 888,
        | "taxOnOtherIncome": 888,
        | "incomeTaxDue": 66500,
        | "incomeTaxCharged": 888,
        | "deficiencyRelief": 888,
        | "topSlicingRelief": 888,
        | "ventureCapitalTrustRelief": 888,
        | "enterpriseInvestmentSchemeRelief": 888,
        | "seedEnterpriseInvestmentSchemeRelief": 888,
        | "communityInvestmentTaxRelief": 888,
        | "socialInvestmentTaxRelief": 888,
        | "maintenanceAndAlimonyPaid": 888,
        | "marriedCouplesAllowance": 888,
        | "marriedCouplesAllowanceRelief": 888,
        | "surplusMarriedCouplesAllowance": 888,
        | "surplusMarriedCouplesAllowanceRelief": 888,
        | "notionalTaxFromLifePolicies": 888,
        | "notionalTaxFromDividendsAndOtherIncome": 888,
        | "foreignTaxCreditRelief": 888,
        | "incomeTaxDueAfterAllowancesAndReliefs": 888,
        | "giftAidPaymentsAmount": 888,
        | "giftAidTaxDue": 888,
        | "capitalGainsTaxDue": 888,
        | "remittanceForNonDomiciles": 888,
        | "highIncomeChildBenefitCharge": 888,
        | "totalGiftAidTaxReduced": 888,
        | "incomeTaxDueAfterGiftAidReduction": 888,
        | "annuityAmount": 888,
        | "taxDueOnAnnuity": 888,
        | "taxCreditsOnDividendsFromUkCompanies": 888,
        | "incomeTaxDueAfterDividendTaxCredits": 888,
        | "nationalInsuranceContributionAmount": 888,
        | "nationalInsuranceContributionCharge": 888,
        | "nationalInsuranceContributionSupAmount": 888,
        | "nationalInsuranceContributionSupCharge": 888,
        | "totalClass4Charge": 888,
        | "nationalInsuranceClass1Amount": 888,
        | "nationalInsuranceClass2Amount": 888,
        | "nicTotal": 24000,
        | "underpaidTaxForPreviousYears": 888,
        | "studentLoanRepayments": 888,
        | "pensionChargesGross": 888,
        | "pensionChargesTaxPaid": 888,
        | "totalPensionSavingCharges": 888,
        | "pensionLumpSumAmount": 888,
        | "pensionLumpSumRate": 888,
        | "statePensionLumpSumAmount": 888,
        | "remittanceBasisChargeForNonDomiciles": 888,
        | "additionalTaxDueOnPensions": 888,
        | "additionalTaxReliefDueOnPensions": 888,
        | "incomeTaxDueAfterPensionDeductions": 888,
        | "employmentsPensionsAndBenefits": 888,
        | "outstandingDebtCollectedThroughPaye": 888,
        | "payeTaxBalance": 888,
        | "cisAndTradingIncome": 888,
        | "partnerships": 888,
        | "ukLandAndPropertyTaxPaid": 888,
        | "foreignIncomeTaxPaid": 888,
        | "trustAndEstatesTaxPaid": 888,
        | "overseasIncomeTaxPaid": 888,
        | "interestReceivedTaxPaid": 888,
        | "voidISAs": 888,
        | "otherIncomeTaxPaid": 888,
        | "underpaidTaxForPriorYear": 888,
        | "totalTaxDeducted": 888,
        | "incomeTaxOverpaid": 888,
        | "incomeTaxDueAfterDeductions": 888,
        | "propertyFinanceTaxDeduction": 888,
        | "taxableCapitalGains": 888,
        | "capitalGainAtEntrepreneurRate": 888,
        | "incomeTaxOnCapitalGainAtEntrepreneurRate": 888,
        | "capitalGrainsAtLowerRate": 888,
        | "incomeTaxOnCapitalGainAtLowerRate": 888,
        | "capitalGainAtHigherRate": 888,
        | "incomeTaxOnCapitalGainAtHigherTax": 888,
        | "capitalGainsTaxAdjustment": 888,
        | "foreignTaxCreditReliefOnCapitalGains": 888,
        | "liabilityFromOffShoreTrusts": 888,
        | "taxOnGainsAlreadyCharged": 888,
        | "totalCapitalGainsTax": 888,
        | "incomeAndCapitalGainsTaxDue": 888,
        | "taxRefundedInYear": 888,
        | "unpaidTaxCalculatedForEarlierYears": 888,
        | "marriageAllowanceTransferAmount": 888,
        | "marriageAllowanceTransferRelief": 888,
        | "marriageAllowanceTransferMaximumAllowable": 888,
        | "nationalRegime": "888",
        | "allowance": 888,
        | "limitBRT": 888,
        | "limitHRT": 888,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
        | "limitAIA": 888,
        | "limitAIA": 888,
        | "allowanceBRT": 888,
        | "interestAllowanceHRT": 888,
        | "interestAllowanceBRT": 888,
        | "dividendAllowance": 888,
        | "dividendBRT": 888,
        | "dividendHRT": 888,
        | "dividendART": 888,
        | "class2NICsLimit": 888,
        | "class2NICsPerWeek": 888,
        | "class4NICsLimitBR": 888,
        | "class4NICsLimitHR": 888,
        | "class4NICsBRT": 888,
        | "class4NICsHRT": 888,
        | "proportionAllowance": 888,
        | "proportionLimitBRT": 888,
        | "proportionLimitHRT": 888,
        | "proportionalTaxDue": 888,
        | "proportionInterestAllowanceBRT": 888,
        | "proportionInterestAllowanceHRT": 888,
        | "proportionDividendAllowance": 888,
        | "proportionPayPensionsProfitAtART": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtART": 888,
        | "proportionPayPensionsProfitAtBRT": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtBRT": 888,
        | "proportionPayPensionsProfitAtHRT": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtHRT": 888,
        | "proportionInterestReceivedAtZeroRate": 888,
        | "proportionIncomeTaxOnInterestReceivedAtZeroRate": 888,
        | "proportionInterestReceivedAtBRT": 888,
        | "proportionIncomeTaxOnInterestReceivedAtBRT": 888,
        | "proportionInterestReceivedAtHRT": 888,
        | "proportionIncomeTaxOnInterestReceivedAtHRT": 888,
        | "proportionInterestReceivedAtART": 888,
        | "proportionIncomeTaxOnInterestReceivedAtART": 888,
        | "proportionDividendsAtZeroRate": 888,
        | "proportionIncomeTaxOnDividendsAtZeroRate": 888,
        | "proportionDividendsAtBRT": 888,
        | "proportionIncomeTaxOnDividendsAtBRT": 888,
        | "proportionDividendsAtHRT": 888,
        | "proportionIncomeTaxOnDividendsAtHRT": 888,
        | "proportionDividendsAtART": 888,
        | "proportionIncomeTaxOnDividendsAtART": 888,
        | "proportionClass2NICsLimit": 888,
        | "proportionClass4NICsLimitBR": 888,
        | "proportionClass4NICsLimitHR": 888,
        | "proportionReducedAllowanceLimit": 888
        |}
      """.stripMargin

    val calculationDataSuccessJson = Json.parse(calculationDataSuccessString)

    val calculationDataErrorModel = CalculationDataErrorModel(testErrorStatus, testErrorMessage)
    val calculationDataErrorString =
      s"""
         |{
         |  "code":$testErrorStatus,
         |  "message":"$testErrorMessage"
         |}
       """.stripMargin
    val calculationDataErrorJson = Json.parse(calculationDataErrorString)

    val financialDataSuccessModel = CalcDisplayModel(
      calcTimestamp = Estimates.lastTaxCalcSuccess.calcTimestamp,
      calcAmount = Estimates.lastTaxCalcSuccess.calcAmount,
      calcDataModel = Some(calculationDataSuccessModel)
    )

    val financialDataNoBreakdownModel = CalcDisplayModel(
      calcTimestamp = Estimates.lastTaxCalcSuccess.calcTimestamp,
      calcAmount = Estimates.lastTaxCalcSuccess.calcAmount,
      calcDataModel = None
    )
  }
}
