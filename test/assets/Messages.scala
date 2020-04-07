/*
 * Copyright 2020 HM Revenue & Customs
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

object Messages {

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

  //Tax Years Page Messages
  object TaxYears {
    val title = "My tax years"
    val heading = "My tax years"
    val noEstimates = "You don’t have an estimate right now. We’ll show your next Income Tax estimate when you submit a report using software."
    val p1 = "Below is a list of tax years you have submitted updates for. Click on the tax years to view a summary."
    val taxYearLink: (String, String) => String = (year, yearPlusOne) => s"$year to $yearPlusOne"
    val ongoing = "ONGOING"
    val complete = "COMPLETE"
    val overdue = "OVERDUE"
  }

  // Home Page Messages
  object HomePage {
    val title = "Your Income Tax"
    val heading = "Income Tax"
    def taxpayerReference(mtditid: String): String = s"Unique Taxpayer Reference - $mtditid"
    val updatesHeading = "Next update due"
    val updatesLink = "View updates"
    val taxYearsHeading = "My tax years"
    val taxYearsDescription = "Check the status of each tax year."
    val taxYearsLink = "View tax years"
  }

  object IncomeBreakdown {
    val title = "Income"
    def subHeading(taxYear: Int): String = s"6 April ${taxYear - 1} to 5 April $taxYear"
    def heading(taxYear: Int): String = s"${subHeading(taxYear)} $title"
    def guidance(taxYear: Int): String = s"This is a summary of your income for the tax year ${taxYear - 1}-$taxYear. " +
      s"You can see more details in your record-keeping software (opens in a new tab)."
    val guidanceLink = "record-keeping software (opens in a new tab)"
    val selfEmployments = "Profit from all self employments"
    val property = "Profit from UK land and property"
    val bbsi = "Interest from UK banks, building societies and securities"
    val dividends = "Dividends from UK companies"
    val total = "Total income"
  }

  // Estimated Tax Liability Page Messages
  class Calculation(taxYear: Int) {
    val heading = s"Tax estimate for ${taxYear-1} - $taxYear"
    val title = heading
    object Crystallised {
      val heading = s"Income tax bill for ${taxYear-1} to $taxYear"
      val tabTitle = heading
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
    object Bills {
      val billsTitle = "Bills"
      val billsHeading = "Bills"
      val viewBills = "View your finalised bills:"
      val billLink = s"${taxYear-1} to $taxYear tax year"
      val billsLinkAriaLabel = s"view bill for $billLink"
      val noBills = "You’ve had no bills since you started reporting through software."
      val earlierBills = "For earlier bills, view your Self Assessment calculations (opens in a new tab)."
    }
  }

  // No Estimated Tax Liability Page Messages
  object NoEstimatedTaxLiability {
    val subHeading = "Estimates"
    val heading = "Tax estimate for 2017 - 2018"
    val title = heading
    val p1 = "Once you’ve submitted a report using your accounting software, you can view your tax estimate here."
  }

  // Estimated Tax Liability Error Page Messages
  object EstimatedTaxLiabilityError {
    val subHeading = "Estimates"
    val heading = "Tax estimate for 2017 - 2018"
    val title = heading
    val p1 = "We can’t display your estimated tax amount at the moment."
    val p2 = "Try refreshing the page in a few minutes."
  }

  object NoReportDeadlines {
    val title = "Report deadlines"
    val heading = "Report deadlines"
    val noReports = "You don’t have any reports due right now. Your next deadline will show here on the first Monday of next month."
  }

  // Statements Page Messages
  object Statements {
    val title = "Income Tax Statement"
    val pageHeading = title
    val p1 = "A record of your charges and payments."
    val taxYear: Int => String = taxYear => s"Tax year: ${taxYear-1}-$taxYear"
    val totalCharges = "Total charges"
    val stillToPay: String => String = amount => s"Still to pay: $amount"
    val dueBy: String => String = date => s"This is due by $date."
    val dueByWithLink: String => String = date => s"This is due by $date. You can pay this now."
    val paymentAriaLabel: Int => String = taxYear => s"pay your ${taxYear-1}-$taxYear bill now"
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

  // Timeout Messages
  object Timeout {
    val title = "Your session has timed out"
    val heading = "Your session has timed out"
    val signIn = "To view your quarterly reporting details, you’ll have to sign in using your Government Gateway ID."
  }

  object Helpers {
    val overdue = "Overdue"
    val received = "Received"
    val due: String => String = duedate => s"Due by $duedate"
  }

  object ISE {
    val title = "Sorry, we are experiencing technical difficulties - 500"
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
    val title = "You can’t view this page"
    val heading = title
    val signUp = "You need to sign up for quarterly reporting before you can view this page."
  }
  object ExitSurvey {
    val title = "Give feedback"
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
    val title = "Thank you"
    val heading = "Thank you"
    val line1 = "Your feedback will help us improve this service."
    val signInLink = "Go back to sign in."
  }

  object Breadcrumbs {
    val bta = "Business tax account"
    val it = "Income Tax account"
    val taxYears = "My tax years"
    val basicItEstimate: Int => String = taxYear => s"${taxYear-1} to $taxYear tax year"
    val itEstimate: Int => String = taxYear => s"Tax estimate for ${taxYear-1} - $taxYear"
    val finalisedBill: Int => String = taxYear => s"Income tax bill for ${taxYear-1} to $taxYear"
    val obligations = "Report deadlines"
    val statement = "Income Tax statement"
    val updates = "Updates"
    val payementsDue = "Income Tax payments"
    def taxYearOverview(firstYear: Int, secondYear: Int): String = s"6 April $firstYear to 5 April $secondYear"
  }

  //ReportDeadlines Page Messages
  object Obligations {
    val title = "Current Updates"
    val heading = "Updates"
    val previousObligations = "Previously submitted updates"
    val tabOne = "Updates due"
    val tabTwo = "Previously submitted updates"
    val subTitle = "Updates due"
    val annualDropdownListOne = "In the annual update you must declare that the 4 quarterly updates you submitted are correct or amend any errors."
    val annualDropdownListTwo = "Using your accounting software , you need to submit one annual update for each source of income at the end of its accounting period."
    val quarterlyDropdownLine1 ="A quarterly update sets out the income and expenses for each 3-month period."
    val quarterlyDropdownLine2 ="Using your accounting software, you must submit 4 quarterly updates in a year for each source of income."
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
    val title = "Previous Submitted Updates"
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
    val title = "Payments Due"
    val heading = "Income Tax payments"
    val subTitle = "Payment due"
    val due = "due"
    def taxYearPeriod(from: String, to:String) = s"Tax year $from to $to"
    val noBills = "No payments due."
    val billLink = "View bill"
    def billLinkAria(fromYear: String, toYear: String) = s"View bill for Tax year $fromYear to $toYear"
    val payNow = "Pay now"
    def payNowAria(fromYear: String, toYear: String) = s"Pay now for Tax year $fromYear to $toYear"
  }

  object TaxYearOverview {
    val title = "Tax year overview"
    def heading(firstYear: Int, secondYear: Int): String = s"Tax year overview 6 April $firstYear to 5 April $secondYear"
    def status(status: String) = s"Tax year status: $status"
    def calculationDate(date: String) = s"Calculation date: $date"
    val summary = "This page shows a summary of your tax year."
    val income = "Income"
    val deductions = "Deduction"
    val taxableIncome = "Total taxable income"
    val taxDue = "Tax due"
    val payment = "Payment"
    val totalRemaining = "Total remaining due"
  }

}
