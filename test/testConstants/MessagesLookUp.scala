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

  object Core {
    val welsh = "Cymraeg"
  }

  object CalculationBreakdown {
    def estimateSubHeading(taxAmount: String) = s"How your estimate of $taxAmount was calculated"

    val billSubHeading = "How your tax was calculated"

    def nationalRegime(regime: String) = s"(National Regime: $regime)"

    val incomeHeading = "About your income"
    val incomeSubheading = "Summary of the income you reported and applicable deductions"
    val incomeBusinessProfit = "Income from Business Profit"
    val incomeProperty = "Income from Property"
    val incomeDividends = "Income from Dividends"
    val incomeSavings = "Income from Savings"
    val incomePersonalAllowance = "Deduction - Personal Allowance"
    val incomeDividendsAllowance = "Deduction - Dividends Allowance"
    val incomeSavingsAllowance = "Deduction - Savings Allowance"
    val incomeGiftInvestmentPropertyToCharity = "Deduction - gifts of investments and property to charity"
    val incomeEstimatedTotalTaxableIncome = "Your estimated total taxable income"
    val incomeTotalTaxableIncome = "Your total taxable income"
    val calculationHeading = "Calculation of tax"

    def calculationSubheading(income: String): String = s"Your tax calculation breakdown based on your taxable income of $income"

    def calculationIncomeTax(income: String, rate: String) = s"Income Tax ($income at $rate%)"

    def calculationDividend(income: String, rate: String) = s"Dividend Tax ($income at $rate%)"

    def calculationSavings(income: String, rate: String) = s"Savings Tax ($income at $rate%)"

    val calculationClassTwoNI = "Class 2 National Insurance"
    val calculationClassFourNI = "Class 4 National Insurance"
    val calculationTaxRelief = "Deduction - your tax reliefs"
    val calculationPaymentsToDate = "Deduction - your payments to date"
    val calculationYourTotalEstimate = "Your total estimated tax"
    val calculationTotalOutstanding = "Total outstanding tax"

    def calculationDueDate(year: String) = s"due 31 January $year"
  }

  object Base {
    val backToHome = "Back to Income Tax home"
    val back = "Back"
  }

  object RecruitmentBanner {
    val text = "Help improve this service"
    val link = "Join our research panel by answering a few questions."
    val dismiss = "No, thankyou."
  }

  object CurrentObligationsHelper {
    val subHeading = "Updates due"
    val subHeadingPara = "You must submit these returns on your record-keeping software (opens in a new tab) by the dates listed."
    val annualDropdownListOne = "In the annual update you must declare that the 4 quarterly updates you submitted are correct or amend any errors."
    val annualDropdownListTwo = "Using your record-keeping software (opens in a new tab), you need to submit one annual update for each source of income at the end of its accounting period."
    val quarterlyDropdownLine1 = "A quarterly update sets out the income and expenses for each 3-month period."
    val quarterlyDropdownLine2 = "Using your record-keeping software (opens in a new tab), you must submit 4 quarterly updates in a year for each source of income."
    val finalDeclerationDetails = "Your final declaration is to confirm that the annual updates you submitted are correct and that you have submitted every source of income true to your knowledge using your record-keeping software (opens in a new tab)."
    val quarterlyDropDown = "What is a quarterly update?"
    val annualDropDown = "What is an annual update?"
    val finalDeclarationDropDown = "What is a final declaration?"
    val declarationsHeading = "Final declaration"
    val quarterlyHeading = "Quarterly updates"
    val annualHeading = "Annual updates"
    val finalDeclarationHeading = "Final Declaration"
    val propertyIncome = "Property Income"

    def fromToDates(from: String, to: String) = s"$from to $to"

    val dueOn = "Due on:"
    val crystallisedHeading = "Whole tax year (final check)"
  }

  //Tax Years Page Messages
  object TaxYears {
    val title = "Tax years - Business Tax account - GOV.UK"
    val heading = "Tax years"
    val noEstimates = "You don’t have an estimate right now. We’ll show your next Income Tax estimate when you submit a report using software."
    val taxYear: (String, String) => String = (year, yearPlusOne) => s"6 April $year to 5 April $yearPlusOne"
    val overdue = "OVERDUE"
    val updateReturn = "Update return"
    val viewSummary = "View summary"
    val tableHeadingTaxYear = "Tax year"
    val tableHeadingOptions = "Options"
    val saLink = "Self Assessment online account (opens in new tab)"
    val saLinkAgent = "Self Assessment for Agents Account (opens in new tab)"
    val saNote = s"To view your tax years from before you signed up to Making Tax Digital for Income Tax, you need to visit your previous $saLink."
    val saNoteAgent = s"To view your client’s tax years from before they signed up to Making Tax Digital for Income Tax, you need to login into your $saLinkAgent. This will be a different Government Gateway ID and password to your Agent Services account."
  }

  // Home Page Messages
  object HomePage {
    val title = "Income Tax - Business Tax account - GOV.UK"
    val agentTitle = "Your client’s Income Tax - Your client’s Income Tax details - GOV.UK"
    val heading = "Income Tax"
    val agentHeading = "Your client’s Income Tax"

    def taxpayerReference(utr: String): String = s"UTR: $utr"

    val updatesHeading = "Next updates due"
    val paymentsHeading = "Next payments due"
    val updatesLink = "View update deadlines"
    val paymentLink = "Check what you owe"
    val taxYearsHeading = "Returns"
    val taxYearsLink = "View all tax years"
    val viewPaymentslink = "View payment history"
    val yourIncomeTaxReturnHeading = "Your Income Tax return"
    val ManageYourIncomeTaxReturnHeading = "Manage Income Tax"
    val yourIncomeTaxReturnDescription = "Send updates to HMRC and declare income for a tax year."
    val sendUpdatesLink = "Send updates"
    val submitYourReturnsLink = "Update and submit an Income Tax Return"
    val saViewLandPServiceLink = "View your Self Assessment tax years"
    val saViewLandPServiceDescription = "Use this service to view your earlier tax year information before you signed up for Making Tax Digital for Income Tax."
    val changeClientLink = "Change client"
    val backLink = "Back"
    val paymentHistoryHeading = "Payment history"
    val paymentHistoryAndCreditView = "Payment history"
    val creditAndRefundAndCreditView = "Claim a refund"

    def viewPaymentsLinkWithDateRange(taxYear: Int): String = s"View your current ${taxYear - 1} to ${taxYear} return"

    def viewUpdateAndSubmitLinkWithDateRange(taxYear: Int): String = s"Update and submit your ${taxYear - 1} to ${taxYear} return"
  }

  object IncomeBreakdown {
    val title = "Income - Business Tax account - GOV.UK"
    val agentTitle = "Income - Your client’s Income Tax details - GOV.UK"

    def subHeading(taxYear: Int): String = s"6 April ${taxYear - 1} to 5 April $taxYear"

    def heading(taxYear: Int): String = s"${subHeading(taxYear)} Income"

    val guidance = "You can see more details in your record-keeping software."
    val incomeBreakdownHeader = "Income type"
    val incomeBreakdownHeaderAmount = "Amount"
    val employments = "Pay from all employments"
    val benefitsAndExpenses = "Benefits & expenses received"
    val allowableExpenses = "Allowable expenses"
    val selfEmployments = "Profit from all self employments"
    val property = "Profit from UK land and property"
    val bbsi = "Interest from UK banks, building societies and securities"
    val dividends = "Dividends from UK companies"
    val occupationalPensions = "Occupational pensions"
    val stateBenefits = "State benefit income"
    val profitUkFurnished = "Profit from UK furnished holiday lettings"
    val profitFromForeignProperties = "Profit from overseas properties"
    val profitFromEeaHoliday = "Profit from EEA holiday property lettings"
    val foreignDividendsIncome = "Overseas dividends income"
    val foreignSavingsIncome = "Overseas savings income"
    val foreignPensions = "Overseas pensions"
    val incomeReceivedAbroad = "Overseas income received whilst abroad"
    val foreignincomeAndGains = "Overseas income and gains"
    val foreignBenefitsAndGifts = "Overseas benefits and gifts"
    val gainsOnInsurancePolicy = "Gains on life insurance polices"
    val shareSchemes = "Share schemes"
    val total = "Total income received"
  }

  object DeductionBreakdown {
    val title = "Allowances and deductions - Business Tax account - GOV.UK"
    val agentTitle = "Allowances and deductions - Your client’s Income Tax details - GOV.UK"

    def subHeading(taxYear: Int): String = s"6 April ${taxYear - 1} to 5 April $taxYear"

    def heading(taxYear: Int): String = s"${subHeading(taxYear)} Allowances and deductions"

    val guidance = "You can see more details in your record-keeping software."

    val deductionBreakdownHeader = "Allowance or deduction type"
    val deductionBreakdownHeaderAmount = "Amount"
    val personalAllowance = "Personal Allowance"
    val marriageAllowanceTransfer = "Marriage Allowance Transfer"
    val totalPensionContributions = "Pensions contributions"
    val lossesAppliedToGeneralIncome = "Loss relief"
    val giftOfInvestmentsAndPropertyToCharity = "Gift of investments and property to charity"
    val annualPayments = "Annual Payments"
    val loanInterest = "Qualifying loan interest"
    val postCessasationTradeReceipts = "Post cessasation trade reciepts"
    val tradeUnionPayments = "Trade Union payments"
    val total = "Total allowances and deductions"
  }

  object TaxCalcBreakdown {
    val title = "Tax calculation - Business Tax account - GOV.UK"
    val agentTitle = "Tax calculation - Your client’s Income Tax details - GOV.UK"
    val upliftMessage = "1.25 percentage point uplift in National Insurance contribution funds NHS, health and social care"

    def subHeading(taxYear: Int): String = s"6 April ${taxYear - 1} to 5 April $taxYear"

    def heading(taxYear: Int): String = s"${subHeading(taxYear)} Tax calculation"

    val sectionHeadingPPP: String = "Pay, pensions and profit"
    val sectionHeadingSavings: String = "Savings"
    val sectionHeadingDividends: String = "Dividends"
    val sectionHeadingLumpSums: String = "Employment lump sums"
    val sectionHeadingGainsOnLifePolicies: String = "Gains on life policies"
    val sectionHeadingNIC4: String = "Class 4 National Insurance"
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

  // Estimated Tax Liability Page Messages
  class Calculation(taxYear: Int) {
    val heading = s"Tax estimate for ${taxYear - 1} - $taxYear"
    val title = s"Tax estimate for ${taxYear - 1} - $taxYear - Business Tax account - GOV.UK"

    object Crystallised {
      val heading = s"Income tax bill for ${taxYear - 1} to $taxYear"
      val tabTitle = s"$heading - Business Tax account - GOV.UK"
      val subHeading = s"Albert Einstein"
      val utrHeading = "Unique Taxpayer Reference - XAIT0000123456"

      def noBreakdownContent(amount: String): String = s"Your total tax bill amount: $amount"

      val p1 = "This figure is based on the information you provided in your quarterly reports and final report."
      val breakdownHeading = "How this figure was calculated"
      val errors = "If there are any errors, you can make adjustments through your software."
      val changes = s"If you make these changes before 31 January ${taxYear + 1} you will not be penalised."
      val payNow = "Pay now"
      val payDeadline = s"You must pay by 31 January ${taxYear + 1} to avoid penalties."

      def owed(amount: String): String = s"Tax left to pay: $amount"
    }

  }

  // No Estimated Tax Liability Page Messages
  object NoEstimatedTaxLiability {
    val subHeading = "Estimates"
    val heading = "Tax estimate for 2017 - 2018"
    val title = s"$heading - Business Tax account - GOV.UK"
    val p1 = "Once you’ve submitted a report using your accounting software, you can view your tax estimate here."
  }

  // Estimated Tax Liability Error Page Messages
  object EstimatedTaxLiabilityError {
    val subHeading = "Estimates"
    val heading = "Tax estimate for 2017 - 2018"
    val title = s"$heading - Business Tax account - GOV.UK"
    val p1 = "We can’t display your estimated tax amount at the moment."
    val p2 = "Try refreshing the page in a few minutes."
  }

  object NoNextUpdates {
    val title = "Report deadlines - Business Tax account - GOV.UK"
    val heading = "Report deadlines"
    val noUpdates = "You don’t have any reports due right now. Your next deadline will show here on the first Monday of next month."
  }

  // Statements Page Messages
  object Statements {
    val title = "Income Tax Statement - Business Tax account - GOV.UK"
    val pageHeading = "Income Tax Statement"
    val p1 = "A record of your charges and payments."
    val taxYear: Int => String = taxYear => s"Tax year: ${taxYear - 1}-$taxYear"
    val totalCharges = "Total charges"
    val stillToPay: String => String = amount => s"Still to pay: $amount"
    val dueBy: String => String = date => s"This is due by $date."
    val dueByWithLink: String => String = date => s"This is due by $date. You can pay this now."
    val paymentAriaLabel: Int => String = taxYear => s"pay your ${taxYear - 1}-$taxYear bill now"
    val nothingToPay = "Nothing left to pay"
    val paidBill = "You’ve paid this bill."
    val transactions = "Your transactions"
    val charge: String => String = amount => s"You had a charge of $amount."
    val youPaid: (String, String) => String = (amount, date) => s"You paid $amount on $date"
    val earlierTransactions = "For earlier transactions, view your Self Assessment."
    val noTransactions = "You’ve had no transactions since you started reporting through accounting software."

    object Error {
      val pageHeading = "We can’t show your statement right now"
      val p1 = "Try reloading the page or coming back later."
    }

  }

  // Agent Error Messages
  object AgentErrorMessages {
    val heading: String = "You can't use this service yet"
    val title: String = s"$heading - Your client’s Income Tax details - GOV.UK"
    val setupAccountLink = "set up an agent services account (opens in new tab)"
    val notAnAgentNote: String = s"To use this service, you need to $setupAccountLink."
    val signOutButton: String = "Sign out"
  }

  // Timeout Messages
  object Timeout {
    val title = "Your session has timed out - Business Tax account - GOV.UK"
    val heading = "Your session has timed out"
    val signIn = "To view your quarterly reporting details, you’ll have to sign in using your Government Gateway ID."
  }

  object Helpers {
    val overdue = "Overdue"
    val received = "Received"
    val due: String => String = duedate => s"Due by $duedate"
  }

  object ISE {
    val title = "Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK"
  }

  object BtaServiceInfoHeader {
    val btaHome = "Business tax home"
    val btaMessages = "Messages"
    val btaManageAccount = "Manage account"
  }

  object BtaPartial {
    val heading = "Income Tax reporting through software"
    val p1 = "You’ve signed up to report your Income Tax through software. This will replace your Self Assessment tax return."
    val p2 = "You can view your report deadlines, bills and an estimate for the next tax year."
    val button = "View your Income Tax details"
  }

  object NotEnrolled {
    val title = "You can’t view this page - Business Tax account - GOV.UK"
    val heading = "You can’t view this page"
    val signUp = "You need to sign up for quarterly reporting before you can view this page."
  }

  object CustomNotFound {
    val title = "There is a problem - Business Tax account - GOV.UK"
    val AgentTitle = "There is a problem - Your client’s Income Tax details - GOV.UK"
    val heading = "There is a problem"
    val content = "The page you’re trying to view has changed"
    val homepageLinkText = "Return to Making Tax Digital for Income Tax"
  }

  object ExitSurvey {
    val title = "Give feedback - Business Tax account - GOV.UK"
    val heading = "Give feedback"
    val q1 = "Overall, how did you feel about the service you received today?"

    object Options {
      val option1 = "Very satisfied"
      val option2 = "Satisfied"
      val option3 = "Neither satisfied or dissatisfied"
      val option4 = "Dissatisfied"
      val option5 = "Very dissatisfied"
    }

    val q2 = "How could we improve this service?"
    val p1 = "Please don’t include any personal or financial information, for example your National Insurance or credit card numbers."

    object Errors {
      val maxImprovementsError = "You can’t enter more than 1200 characters for your feedback"
    }

  }

  object Thankyou {
    val title = "Thank you - Business Tax account - GOV.UK"
    val heading = "Thank you"
    val line1 = "Your feedback will help us improve this service."
    val signInLink = "Go back to sign in."
  }

  //NextUpdates Page Messages
  object Obligations {
    val title = "Updates - Business Tax account - GOV.UK"
    val nextTitle = "Next updates - Business Tax account - GOV.UK"
    val heading = "Updates"
    val previousObligations = "Previously submitted updates"
    val tabOne = "Updates due"
    //    val tabTwo = "Previously submitted updates"
    val subTitle = "Updates due"
    val annualDropdownListOne = "In the annual update you must declare that the 4 quarterly updates you submitted are correct or amend any errors."
    val annualDropdownListTwo = "Using your accounting software , you need to submit one annual update for each source of income at the end of its accounting period."
    val quarterlyDropdownLine1 = "A quarterly update sets out the income and expenses for each 3-month period."
    val quarterlyDropdownLine2 = "Using your accounting software, you must submit 4 quarterly updates in a year for each source of income."
    val finalDeclerationDetails = "Your final declaration is to confirm that the annual updates you submitted are correct and that you have submitted every source of income true to your knowledge using your accounting software."
    val quarterlyDropDown = "What is a quarterly update?"
    val annualDropDown = "What is an annual update?"
    val finalDeclarationDropDown = "What is a final declaration?"
    val declarationsHeading = "Final declaration"
    val quarterlyHeading = "Quarterly updates"
    val annualHeading = "Annual updates"
    val finalDeclarationHeading = "Final Declaration"
    val propertyIncome = "Property Income"

    def fromToDates(from: String, to: String) = s"$from to $to"

    val dueOn = "Due on:"
    val crystallisedHeading = "Whole tax year (final check)"
  }

  object PreviousObligations {
    val title = "Updates - Business Tax account - GOV.UK"
    val heading = "Updates"
    val subHeading = "Previously submitted updates"
    val noPreviousObligations = "No previously submitted updates"
    val propertyIncomeSource = "Property Income"
    val crystallisationIncomeSource = "Tax year - Final check"
    val quarterly = "Quarterly update"
    val eops = "Annual update"
    val crystallised = "Declaration"

    def dateToDate(from: String, to: String) = s"$from to $to"

    def wasDueOn(date: String) = s"Was due on $date"

    val submittedOn = "Submitted on"
    val updatesLink = "Updates due"
    val previousUpdatesTab = "Previously submitted updates"
  }

  object PaymentDue {
    val title = "Payments due - Business Tax account - GOV.UK"
    val heading = "Payments due"
    val description = "If you have made a payment in the last 7 days, the amounts shown here may not be accurate."
    val due = "Due"

    def taxYearPeriod(from: String, to: String) = s"Tax year $from to $to"

    val noBills = "No payments due."
    val billLink = "View bill"

    def billLinkAria(fromYear: String, toYear: String) = s"View bill for Tax year $fromYear to $toYear"

    val payNow = "Pay now"

    def payNowAria(fromYear: String, toYear: String) = s"Pay now for Tax year $fromYear to $toYear"
  }

  object WhatYouOwe {
    val title = "What you owe - Business Tax account - GOV.UK"
    val heading = "What you owe"
    val noPaymentsDue = "You have no payments due."
    val saLink = "Self Assessment online account (opens in new tab)"
    val saNote = s"You may still have payments due for your earlier Self Assessment tax years. To view these outstanding payments you need to visit your previous $saLink."
    val osChargesNote = "Any payments made will be used for outstanding charges in these tax years first and will not be displayed here."
    val dropDownInfo = "What are the payment types?"
    val paymentDaysNote = "Payments can take up to 7 days to process."
    val creditOnAccount = "If you make a payment more than 30 days before a payment is due, you will see it as credit on account."
    val paymentUnderReviewParaLink = "there is a current appeal against a tax decision (opens in new tab)."
    val paymentUnderReviewPara = s"One or more of your payments are currently under review because $paymentUnderReviewParaLink"

    def preMtdPayments(from: String, to: String) = s"$from to $to"

    val paymentType = "Payment type"
    val taxYearSummary = "Tax year summary"
    val amountDue = "Amount due"

    val paymentprocessingbullet1 = "may take up to 5 working days to process, depending on what payment method (opens in new tab) you use"
    val paymentprocessingbullet2 = "must reach us by the due date - you’ll be charged interest and may have to pay a penalty if your payment is late"
    val paymentsMade = "Any payments you make:"
    val poa1Text = "Payment on account 1 of 2"
    val latePoa1Text = "Late payment interest for payment on account 1 of 2"
    val poa2Text = "Payment on account 2 of 2"
    val latePoa2Text = "Late payment interest for payment on account 2 of 2"
    val remainingBalance = "Balancing payment"
    val preMTDRemainingBalance = "Balancing payment (Pre-Making Tax Digital)"
    val lateRemainingBalance = "Late payment interest for Balancing payment"
    val remainingBalanceLine1 = "The Balancing payment is the tax you still need to pay for a tax year."
    val interestOnRemainingBalance = "Interest on Balancing payment"
    val paymentUnderReview = "Payment under review"

    def interestOnRemainingBalanceYear(from: String, to: String) = s"From $from to $to"

    val poaHeading = "Payment on account"
    val poaLine1 = "This is a payment towards next year’s tax bill. Each payment is half of your previous year’s tax bill."
    val lpiHeading = "Late payment interest"
    val lpiLine1 = "This is interest you owe for a late payment."
    val nic2Heading = "Class 2 National Insurance"
    val nic2Line1 = "This is a payment specifically for your Class 2 National Insurance contributions."
    val cancelledPAYEHeading = "Cancelled PAYE Self Assessment"
    val cancelledPAYELine1 = "You have previously agreed to pay some of your self assessment tax through your PAYE tax code. HMRC has been unable to collect all of these payments from you, so this is the balancing payment you need to pay."
    val overduePayments = "Overdue payments"
    val overduePaymentsDue = "Overdue payments"
    val overdueTag = "OVERDUE"
    val dueInThirtyDays = "Due within 30 days"
    val futurePayments = "Future payments"
    val totalPaymentsDue = "Total payments"

    val poa1WithTaxYear = s"$poa1Text $currentYear"
    val poa1WithTaxYearOverdue = s"$overdueTag $poa1Text $currentYear"
    val poa2WithTaxYear = s"$poa2Text $currentYear"
    val poa2WithTaxYearOverdue = s"$overdueTag $poa2Text $currentYear"
    val poa1WithTaxYearAndUnderReview = s"$poa1Text $currentYear $paymentUnderReview"
    val poa1WithTaxYearOverdueAndUnderReview = s"$overdueTag $poa1Text $currentYear $paymentUnderReview"
    val poa2WithTaxYearAndUnderReview = s"$poa2Text $currentYear $paymentUnderReview"
    val poa2WithTaxYearOverdueAndUnderReview = s"$overdueTag $poa2Text $currentYear $paymentUnderReview"

    def interestFromToDate(from: String, to: String, rate: String) = s"Interest for late payment $from to $to at $rate%"

    val interestRatesLink = "current interest rate for late and early payments (opens in new tab)"
    val interestRatesPara = s"Any overdue payment interest is at the $interestRatesLink. The rate may have changed since the interest was first charged."

    def taxYearForChargesText(from: String, to: String): String = s"Tax year $from to $to"

    def taxYearSummaryText(from: String, to: String): String = s"$from to $to Tax year"


    val dueDate = "Due date"
    val payNow = "Make a payment"

    def payNowAria(fromYear: String, toYear: String) = s"Pay now for Tax year $fromYear to $toYear"
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

  object TaxYearSummary {
    def title(firstYear: Int, secondYear: Int) = s"6 April $firstYear to 5 April $secondYear - Business Tax account - GOV.UK"

    def heading(firstYear: Int, secondYear: Int): String = s"Tax year summary 6 April $firstYear to 5 April $secondYear"

    def status(status: String) = s"Tax year status: $status"

    def calculationDate(date: String) = s"Calculation date: $date"

    val linksSummary = "Click on each link for more detail about each section."
    val noLinksSummary = "This page shows a summary of your tax year."
    val income = "Income"
    val deductions = "Allowances and deductions"
    val taxableIncome = "Total income on which tax is due"
    val taxDue = "Income Tax and Class 4 National Insurance contributions due"
    val totalRemaining = "Total remaining due"
  }

  object StandardErrorView {
    val heading = "Sorry, there is a problem with the service"
    val errorMessage = "Try again later."
  }

  object PaymentAllocation {
    val title = "Payment made to HMRC - Business Tax account - GOV.UK"
    val heading = "Payment made to HMRC"
    val headingEarlier = "Payment from an earlier tax year"
    val saLink = "Self Assessment online account (opens in new tab)"
    val saNote = s"This is money you paid into your account before you signed up for Making Tax Digital for Income Tax. Visit your previous $saLink."
    val backLink = "Back"
    val date = "31 January 2021"
    val amount = "£300.00"
    val info = "Any payments made will automatically be allocated towards penalties and earlier tax years before current and future tax years."
    val paymentAllocationHeading = "Payment allocations"
    val tableHeadings = Seq("Payment allocation", "Date allocated", "Amount")
    val tableDataPaymentAllocation = "Class 4 National Insurance for payment on account 1 of 2 2020 Tax year 2019 to 2020"
    val tableDataPaymentAllocationLpi = "Late payment interest for Balancing payment 2020 Tax year 2019 to 2020"
    val tableDataDateAllocated = "31 Jan 2021"
    val tableDataDateAllocatedLpi = "N/A"
    val tableDataAmount = "£10.10"
    val tableDataAmountLpi = "£300.00"
    val creditOnAccount = "Money in your account"
    val creditOnAccountAmount = "£200.00"
    val moneyOnAccount = "Money in your account"
    val moneyOnAccountDate = "N/A"
    val moneyOnAccountAmount = "£200.00"
    val allocationsTableHeading = "Payment allocations"
    val allocationsTableHeaders = Seq("Payment allocation", "Date allocated", "Amount")
    val allocationsTableCaption = "Payment allocations"
    val allocationsTableHeadersText: String = allocationsTableCaption + " " + allocationsTableHeaders.mkString(" ")

  }

  //Tax Years Page Messages
  object CreditAndRefunds {
    val title = "Claim a refund"
    val subHeadingWithCredits = "in your account."
    val subHeadingWithoutCredits = "has been requested as a refund and is in progress."
    val noAvailableAmount = "You have no money in your account."
    val totalText = "Total"
    val refundText = "Refund - in progress"
    val paymentText = "payment"
    val claimBtn = "Claim a refund"
    val checkBtn = "Check refund status"
  }
}
