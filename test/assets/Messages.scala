/*
 * Copyright 2018 HM Revenue & Customs
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

  object Base {
    val backToHome = "Back to Income Tax home"
  }

  object RecruitmentBanner {
    val text = "Help improve this service"
    val link = "Join our research panel by answering a few questions."
    val dismiss = "No, thankyou."
  }

  //Estimates Page Messages
  class Estimates {
    val title = "Current estimates"
    val heading = "View your estimates"
    val noEstimates = "You don't have an estimate right now. We'll show your next Income Tax estimate when you submit a report using software."
    val noEstimatesHeading = "Your Income Tax estimate"
    val p1 = "View your current estimates."
    val taxYearLink: (String, String) => String = (year, yearPlusOne) => s"Tax year: $year to $yearPlusOne"
  }

  // Home Page Messages
  object HomePage {
    val title = "Your Income Tax"
    val topHeading = "Reporting through software"
    val topSubHeading = "Income Tax"
    val topText = "You send your Income Tax reports using accounting software."
    val dropDown = "How to do this"
    val dropDownText1 = "You'll send four reports during the year."
    val dropDownText2 = "After that, you'll need to confirm and finalise your figures in your accounting software."
    val pageHeading = "Your account"
    val pageSubHeading: String => String = mtditid => s"Ref: $mtditid"
    object EstimatesSection {
      val heading = "Estimates"
      val paragraph = "See what we think you'll owe."
      val link = "View your estimates"
    }
    object BillsSection {
      val heading = "Bills"
      val paragraph = "See your current and previous Income Tax bills."
      val link = "View your bills"
    }
    object ReportDeadlinesSection {
      val heading = "Report deadlines"
      val paragraph = "Check your report deadlines and if we've received them."
      val link = "View your deadlines"
    }
    object StatementSection {
      val heading = "Statements"
      val paragraph = "View your income tax transactions, including charges and payments."
      val link = "View your statement"
    }
    object AccountDetailsSection {
      val heading = "Account details"
      val paragraph = "See contact information and other details we have for your businesses."
      val link = "Check your account details"
    }
  }

  // Estimated Tax Liability Page Messages
  class Calculation(taxYear: Int) {
    val pageHeading = "Your Income Tax estimate"
    val taxYearSubHeading = s"Tax year: ${taxYear-1} to $taxYear"
    val title = taxYearSubHeading
    object Crystallised {
      val tabTitle = "Your final submission"
      val heading = "Your finalised Income Tax bill"
      val subHeading = s"Tax year: ${taxYear-1} to $taxYear"
      val wyoHeading: String => String = whatYouOwe => s"What you owe: $whatYouOwe"
      val p1 = "This figure is based on the information you provided in your quarterly reports and final report. You told us this information is accurate."
      val warning = s"If you pay this bill after 31 January ${taxYear + 1} you'll get penalties."
      val breakdownHeading = "How this figure was calculated"
      val incorrect = "Incorrect figures"
      val errors = "If there are any errors, you can make adjustments through your software."
      val changes = s"If you make these changes before 31 January ${taxYear + 1} you will not be penalised."
      val payNow = "Continue to payment"
    }
    object EoyEstimate {
      val heading: String => String = eoyEstimate => s"Annual estimate: $eoyEstimate"
      val p1 = s"This is an estimate of what you'll pay for the whole of this tax year, beginning 6 April ${taxYear-1} and ending 5 April $taxYear."
      val p2 = "It's based on your current estimate and is a total of all income tax, from any source that you report through accounting software."
    }
    object InYearEstimate {
      val heading: String => String = inYearEstimate => s"Current estimate: $inYearEstimate"
      val p1: String => String = calcDate => s"This is an estimate of the tax you owe from 6 April ${taxYear-1} to $calcDate."
      val p2 = "It's based on the information you report through accounting software."
      object CalculationBreakdown {
        val heading = "How your current estimate was calculated"
        val businessProfit = "Business profit"
        val propertyProfit = "Property profit"
        val personalAllowance = "Personal Allowance (for period reported)"
        val personalAllowanceSavingsEstimates = "Personal Allowance, incl. Savings (for period reported)"
        val personalAllowanceSavingsBills = "Personal Allowance (Income Tax and savings)"
        val additionalAllowances = "Additional allowances"
        val yourTaxableIncome = "Your taxable income (Income Tax)"
        val atBR: String => String = amount => s"Income Tax ($amount at 20%)"
        val atHR: String => String = amount => s"Income Tax ($amount at 40%)"
        val atAR: String => String = amount => s"Income Tax ($amount at 45%)"
        val dividendIncome = "Income from dividends"
        val dividendAllowance = "Personal Allowance (dividends)"
        val taxableDividends = "Your taxable income (dividends)"
        def dividendAtRate(amount: String, rate: String): String = s"Dividend tax ($amount at $rate%)"
        val nic2 = "Class 2 National Insurance"
        val nic4 = "Class 4 National Insurance"
        val reliefs = "Your tax reliefs"
        val total = "Your Income Tax and National Insurance estimate"
      }
      val accuracy = "Although more accurate than your annual estimate, your current estimate may not be a true reflection of the tax you owe."
      object WhyThisMayChange {
        val heading = "Why your current estimate may change"
        val p1 = "Your estimate could change because:"
        val bullet1 = "rates and allowances won't be applied in full until the end of the tax year"
        val bullet2 = "you may earn more money"
        val bullet3 = "you may have income that's not reported in your accounting software"
      }
    }
    object Bills {
      val billsTitle = "Previous statements"
      val billsHeading = "Income Tax bills"
      val viewBills = "View finalised bills."
      val billLink = s"Tax year: ${taxYear-1} to $taxYear"
      val noBills = "You've had no bills since you started reporting through software."
      val earlierBills = "For earlier bills, view your self assessment calculations."
    }
  }

  // No Estimated Tax Liability Page Messages
  object NoEstimatedTaxLiability {
    val pageHeading = "Your Income Tax estimate"
    val taxYearSubheading = "Tax year: 2017 to 2018"
    val title = taxYearSubheading
    val p1 = "Once you've submitted a report using your accounting software, you can view your tax estimate here."
  }

  // Estimated Tax Liability Error Page Messages
  object EstimatedTaxLiabilityError {
    val pageHeading = "Your Income Tax estimate"
    val taxYearSubheading = "Tax year: 2017 to 2018"
    val title = taxYearSubheading
    val p1 = "We can't display your estimated tax amount at the moment."
    val p2 = "Try refreshing the page in a few minutes."
  }

  //ReportDeadlines Page Messages
  object ReportDeadlines {
    val title = "Your Income Tax report deadlines"
    val info  = "You must submit a report once every quarter using accounting software."
    val propertyHeading = "Property income"
    val periodHeading = "Report period"
    val statusHeading = "Report due date"
    val ceased: String => String = date => s"This business ceased trading on $date."
    val portfolio = "This covers all properties that you earn income from."
    val eops = "Whole tax year (final check)"
    object Errors {
      val p1 = "We can't display your next report due date at the moment."
      val p2 = "Try refreshing the page in a few minutes."
    }
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
    val nothingToPay = "Nothing left to pay"
    val paidBill = "You've paid this bill."
    val transactions = "Your transactions"
    val charge: String => String = amount => s"You had a charge of $amount."
    val youPaid: (String, String) => String = (amount, date) => s"You paid $amount on $date"
    val earlierTransactions = "For earlier transactions, view your self assessment."
    val noTransactions = "You've had no transactions since you started reporting through accounting software."
    object Error {
      val pageHeading = "We can't show your statement right now"
      val p1 = "Try reloading the page or coming back later."
    }
  }

  //Account Details Messages
  object AccountDetails {
    val title = "Account details"
    val heading = "Account details"
    val yourBusinesses = "Your businesses"
    val yourProperties = "Your properties"
    val reportingPeriod: (String, String) => String = (start, end) => "Reporting period: " + start + " - " + end
  }

  //Business Details Messages
  object BusinessDetails {
    val reportingPeriod: (String, String) => String = (start, end) => "Reporting period: " + start + " - " + end
    val ceasedTrading: String => String = date => "This business ceased trading on " + date + "."
    val addressAndContact = "Address and contact details"
    val tradingName = "Trading name"
    val businessAddress = "Business address"
    val additionalInfo = "Additional information"
    val accountingMethod: String => String = method => "This business uses the " + method + " accounting method."
    val backToAccount = "Back to account details"
  }

  // Timeout Messages
  object Timeout {
    val title = "Your session has timed out"
    val heading = "Your session has timed out"
    val signIn = "To view your quarterly reporting details, you'll have to sign in using your Government Gateway ID."
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
    val initial = "You've signed up for quarterly reporting. You may also need to make an annual Self Assessment return and pay anything you owe."
    val heading = "Quarterly reporting"
    val reportDue: String => String = date => "Your next report is due by " + date
    val currentEstimate: String => String = amount => "Your estimated tax amount is " + amount
    val currentEstimateYear: (Int, String) => String = (taxYear, amount) => "Your estimated tax amount for " + (taxYear-1) + " to " + taxYear + " is " + amount
    val noEstimate: Int => String = (taxYear) => "Once you've submitted a report using your accounting software, you can view your estimate for " +(taxYear-1)+ " to " +taxYear+ " tax year here."
    object Error {
      val estimateErrorP1 = "We can't display your estimated tax amount at the moment."
      val estimateErrorP2 = "Try refreshing the page in a few minutes."
      val obligationErrorP1 = "We can't display your next report due date at the moment."
      val obligationErrorP2 = "Try refreshing the page in a few minutes."
    }
  }

  object NotEnrolled {
    val title = "You can't view this page"
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
    val p1 = "Please don't include any personal or financial information, for example your National Insurance or credit card numbers."
    object Errors {
      val maxImprovementsError = "You can't enter more than 1200 characters for your feedback"
    }
  }

  object Thankyou {
    val title = "Thank you"
    val heading = "Thank you"
    val line1 = "Your feedback will help us improve this service."
    val signInLink = "Go back to sign in."
  }

  object Breadcrumbs {
    val bta = "Business tax home"
    val it = "Income Tax"
    val estimates = "View your estimates"
    val itEstimate = "Your Income Tax estimate"
    val bills = "Income Tax bills"
    val finalisedBill = "Your finalised Income Tax bill"
    val obligations = "Your Income Tax report deadlines"
    val statement = "Income Tax statement"
    val details = "Account details"
  }
}
