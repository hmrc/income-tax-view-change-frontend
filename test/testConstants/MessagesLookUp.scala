/*
 * Copyright 2022 HM Revenue & Customs
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

package testConstants

import testConstants.FinancialDetailsTestConstants.{currentYear, currentYearMinusOne}

object MessagesLookUp { // scalastyle:off number.of.methods

  object TaxCalcBreakdown {
    val title = "Tax calculation - Business Tax account - GOV.UK"
    val upliftMessage = "1.25 percentage point uplift in National Insurance contributions funds NHS, health and social care"

    def subHeading(taxYear: Int): String = s"6 April ${taxYear - 1} to 5 April $taxYear"

    def heading(taxYear: Int): String = s"${subHeading(taxYear)} Tax calculation"

    val sectionHeadingPPP: String = "Pay, pensions and profit"
    val sectionHeadingSavings: String = "Savings"
    val sectionHeadingDividends: String = "Dividends"
    val sectionHeadingLumpSums: String = "Employment lump sums"
    val sectionHeadingGainsOnLifePolicies: String = "Gains on life policies"
    val sectionHeadingAdditionalChar: String = "Additional charges"
    val sectionHeadingNationalInsuranceContributionsChar: String = "National Insurance contributions"
    val sectionHeadingTaxReductions: String = "Tax reductions"
    val marriageAllowanceTransfer: String = "Marriage Allowance Transfer"
    val topSlicingRelief: String = "Top Slicing Relief"
    val deficiencyRelief: String = "Deficiency Relief"
    val vctRelief: String = "Venture Capital Trust relief"
    val eisRelief: String = "Enterprise Investment Scheme relief"
    val seedRelief: String = "Seed Enterprise Investment Scheme relief"
    val citRelief: String = "Community Investment Tax Relief"
    val sitRelief: String = "Social Enterprise Investment Tax Relief"
    val maintenanceRelief: String = "Maintenance and alimony paid"
    val reliefForFinanceCosts: String = "Relief for finance costs"
    val notionalTax: String = "Notional tax from gains on life policies etc."
    val foreignTaxCreditRelief: String = "Foreign Tax Credit Relief"
    val reliefClaimedOnQualifyingDis: String = "Relief claimed on a qualifying distribution"
    val nonDeductibleLoanInterest: String = "Non deductible loan interest"
    val incomeTaxDueAfterTaxReductions: String = "Income Tax due after tax reductions"
    val otherCharges: String = "Other charges"
    val sectionHeadingCapitalGainsTax: String = "Capital Gains Tax"
    val sectionHeadingAdditionalDeduc: String = "Tax deductions"
    val sectionHeadingTotal: String = "Income Tax and National Insurance contributions due"
    val guidance: String = "This calculation is based on your taxable income of £0.00"
    val cgtTypeTableHeader = "Capital Gains Tax type"
    val reductionTableHeader = "Reduction"
    val amountTableHeader = "Amount"
    val ukRateTableHeader = "UK rates for England and Northern Ireland"
    val scotlandRateTableHead = "UK rates for Scotland"
    val rateBandTableHeader = "Rate band"
    val chargeTypeTableHeader = "Charge type"
    val nationalInsuranceTypeTableHeader = "National Insurance type"
    //pay, pension and profit table
    val pPP_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val pPP_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val pPP_ART: String = "Additional rate (£50,000.00 at 45.0%)"
    //pay, pension and profit table for Scotland
    val pPP_Scot_SRT: String = "Starter rate (£20,000.00 at 10.0%)"
    val pPP_Scot_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val pPP_Scot_IRT: String = "Intermediate rate (£20,000.00 at 25.0%)"
    val pPP_Scot_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val pPP_Scot_ART: String = "Top rate (£500,000.00 at 45.0%)"
    //lump sums table for Scotland
    val ls_Scot_SRT: String = "Starter rate (£20,000.00 at 10.0%)"
    val ls_Scot_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val ls_Scot_IRT: String = "Intermediate rate (£20,000.00 at 25.0%)"
    val ls_Scot_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val ls_Scot_ART: String = "Top rate (£500,000.00 at 45.0%)"
    //Gains on life policies for scotland
    val gols_Scot_SRT: String = "Starter rate (£20,000.00 at 10.0%)"
    val gols_Scot_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val gols_Scot_IRT: String = "Intermediate rate (£20,000.00 at 25.0%)"
    val gols_Scot_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val gols_Scot_ART: String = "Top rate (£500,000.00 at 45.0%)"
    //savings table
    val saving_SSR: String = "Starting rate (£1.00 at 0.0%)"
    val saving_BRT: String = "Basic rate (£20.00 at 10.0%)"
    val saving_HRT: String = "Higher rate (£2,000.00 at 40.0%)"
    val saving_ART: String = "Additional rate (£100,000.00 at 50.0%)"
    val saving_ZRTBR: String = "Basic rate band at nil rate (£20.00 at 0.0%)"
    val saving_ZRTHR: String = "Higher rate band at nil rate (£10,000.00 at 0.0%)"
    //dividends table
    val dividend_BRT: String = "Basic rate (£1,000.00 at 7.5%)"
    val dividend_HRT: String = "Higher rate (£2,000.00 at 37.5%)"
    val dividend_ART: String = "Additional rate (£3,000.00 at 38.1%)"
    val dividend_ZRTBR: String = "Basic rate band at nil rate (£1,000.00 at 0%)"
    val dividend_ZRTHR: String = "Higher rate band at nil rate (£2,000.00 at 0%)"
    val dividend_ZRTAR: String = "Additional rate band at nil rate (£3,000.00 at 0%)"
    //lump sums table
    val ls_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val ls_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val ls_ART: String = "Additional rate (£50,000.00 at 45.0%)"
    //gains on life policies table
    val gols_BRT: String = "Basic rate (£20,000.00 at 20.0%)"
    val gols_HRT: String = "Higher rate (£100,000.00 at 40.0%)"
    val gols_ART: String = "Additional rate (£50,000.00 at 45.0%)"
    //Class 4 National Insurance
    val Nic4_ZRT: String = "Zero rate (£2,000.00 at 1%)"
    val Nic4_BRT: String = "Basic rate (£3,000.00 at 2%)"
    val Nic4_HRT: String = "Higher rate (£5,000.00 at 3%)"
    val giftAidTaxCharge: String = "Gift Aid tax charge"
    val totalPensionSavingCharges: String = "Total pension saving charges"
    val statePensionLumpSum: String = "State pension lump sum"
    val totalStudentLoansRepaymentAmount: String = "Student Loan repayments"
    //other charges table
    val payeUnderpaymentsCodedOut: String = "Underpaid for earlier years in your tax code for 2017 to 2018"
    val saUnderpaymentsCodedOut: String = "Underpaid for earlier years in your self assessment for 2017 to 2018"
    //additional charges table
    val Nic2: String = "Class 2 National Insurance"
    val VoluntaryNic2: String = "Voluntary Class 2 National Insurance"
    val GiftAid: String = "Gift Aid tax charge"
    val PensionLumpSum: String = "State pension lump sum"
    val PensionSavings: String = "Total pension saving charges"
    //capital gains tax table
    val taxableCapitalGains: String = "Taxable Capital Gains"
    val assetsDisposalsAndInvestorsRelief: String = "Business Asset Disposal Relief and or Investors' Relief gains (£10,000.00 at 10.0%)"
    val propertyAndInterest_LRT: String = "Residential property and carried interest basic rate (£20,000.00 at 18.0%)"
    val propertyAndInterest_HRT: String = "Residential property and carried interest higher rate (£30,000.00 at 28.0%)"
    val otherGains_LRT: String = "Other gains basic rate (£11,000.00 at 20.0%)"
    val otherGains_HRT: String = "Other gains higher rate (£12,000.00 at 28.0%)"
    val capitalGainsTaxAdj: String = "Capital Gains Tax adjustment"
    val foreignTaxCreditReliefOnCG: String = "Foreign Tax Credit Relief on capital gains"
    val taxOnGainsAlreadyPaid: String = "Tax on gains already paid"
    val capitalGainsTaxDue: String = "Capital Gains Tax due"
    val capitalGainsTaxOverpaid: String = "Capital Gains Tax calculated as overpaid"
    //additional deduction
    val Nic4New_ZRT: String = "Class 4 National Insurance (£2,000.00 at 1%)"
    val Nic4New_BRT: String = s"Class 4 National Insurance (£3,000.00 at 2%)"
    val Nic4New_HRT: String = s"Class 4 National Insurance (£5,000.00 at 3%)"
    val Nic4NewWithUplift_BRT: String = s"$Nic4New_BRT $upliftMessage"
    val Nic4NewWithUplift_HRT: String = s"$Nic4New_HRT $upliftMessage"
    val BBSI: String = "Interest received from UK banks and building societies"
    val employments: String = "All employments"
    val ukPensions: String = "UK pensions"
    val stateBenefits: String = "State benefits"
    val cis: String = "CIS and trading income"
    val ukLandAndProperty: String = "UK land and property"
    val specialWithholdingTax: String = "Special withholding tax"
    val voidISAs: String = "Void ISAs"
    val totalDeductions: String = "Income Tax due after deductions"
    val deductionsTableHeader = "Deduction type"
    val inYearAdjustmentCodedInLaterTaxYear = "Outstanding debt collected through PAYE"
  }

  object NotEnrolled {
    // todo it's not available in messages
    val signUp = "You need to sign up for quarterly reporting before you can view this page."
  }

  object AgentPaymentDue {
    val title = "What you owe - Your client’s Income Tax details - GOV.UK"
    val heading = "What you owe"
    val noPaymentsDue = "You have no payments due."
    val saLink = "Self Assessment online account (opens in new tab)"
    val saNote = s"You may still have payments due for your earlier Self Assessment tax years. To view these outstanding payments you need to visit your previous $saLink."
    val osChargesNote = "Any payments made will be used for outstanding charges in these tax years first and will not be displayed here."
    val dropDownInfo = "What are the payment types?"
    val paymentsMade = "Any payments you make:"
    val paymentDaysNote = "Payments can take up to 3 days to process."
    val creditOnAccount = "If you make a payment more than 30 days before a payment is due, you will see it as credit on account."
    val paymentUnderReviewParaLink = "there is a current appeal against a tax decision (opens in new tab)."
    val paymentUnderReviewPara = s"One or more of your payments are currently under review because $paymentUnderReviewParaLink"

    def preMtdPayments(from: String, to: String) = s"Tax year $from to $to pre-Making Tax Digital payments"

    val paymentType = "Payment type"
    val taxYearSummary = "Tax year summary"
    val amountDue = "Amount due"
    val poa1Text = "Payment on account 1 of 2"
    val poa2Text = "Payment on account 2 of 2"
    val latePoa1Text = "Late payment interest for payment on account 1 of 2"
    val latePoa2Text = "Late payment interest for payment on account 2 of 2"
    val paymentprocessingbullet1 = "may take up to 5 working days to process, depending on what payment method (opens in new tab) you use"
    val paymentprocessingbullet2 = "must reach us by the due date - you’ll be charged interest and may have to pay a penalty if your payment is late"
    val remainingBalance = "Balancing payment"
    val remainingBalanceLine1 = "The Balancing payment is the tax you still need to pay for a tax year."
    val interestOnRemainingBalance = "Interest on Balancing payment"
    val paymentUnderReview = "Payment under review"

    def interestOnRemainingBalanceYear(from: String, to: String) = s"From $from to $to"

    val poaHeading = "Payment on account"
    val poaLine1 = "This is a payment towards next year’s tax bill. Each payment is half of your previous year’s tax bill."
    val lpiHeading = "Late payment interest"
    val lpiLine1 = "This is interest you owe for a late payment."
    val overduePayments = "Overdue payments"
    val paymentsDue = "Payments Due"
    val overduePaymentsDue = "Overdue payments"
    val overdueTag = "OVERDUE"
    val dueInThirtyDays = "Due within 30 days"
    val futurePayments = "Future payments"
    val totalPaymentsDue = "Total payments"

    val poa1WithTaxYear = s"$poa1Text ${taxYearForChargesText(currentYearMinusOne, currentYear)}"
    val poa1WithTaxYearOverdue = s"$overdueTag $poa1Text ${taxYearForChargesText(currentYearMinusOne, currentYear)}"
    val poa2WithTaxYear = s"$poa2Text $currentYear ${taxYearForChargesText(currentYearMinusOne, currentYear)}"
    val poa2WithTaxYearOverdue = s"$overdueTag $poa2Text ${taxYearForChargesText(currentYearMinusOne, currentYear)}"
    val poa1WithTaxYearAndUnderReview = s"$poa1Text $currentYear ${taxYearForChargesText(currentYearMinusOne, currentYear)} $paymentUnderReview"
    val poa1WithTaxYearOverdueAndUnderReview = s"$overdueTag $poa1Text $currentYear ${taxYearForChargesText(currentYearMinusOne, currentYear)} $paymentUnderReview"
    val poa2WithTaxYearAndUnderReview = s"$poa2Text $currentYear ${taxYearForChargesText(currentYearMinusOne, currentYear)} $paymentUnderReview"
    val poa2WithTaxYearOverdueAndUnderReview = s"$overdueTag $poa2Text ${taxYearForChargesText(currentYearMinusOne, currentYear)} $paymentUnderReview"

    def taxYearForChargesText(from: String, to: String): String = s"Tax year $from to $to"

    def taxYearSummaryText(from: String, to: String): String = s"$from to $to Tax year"

    val dueDate = "Due date"
  }
}
