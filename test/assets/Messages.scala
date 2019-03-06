/*
 * Copyright 2019 HM Revenue & Customs
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
    val title = "Estimates"
    val heading = "Estimates"
    val noEstimates = "You don't have an estimate right now. We'll show your next Income Tax estimate when you submit a report using software."
    val p1 = "View your current estimates:"
    val taxYearLink: (String, String) => String = (year, yearPlusOne) => s"$year to $yearPlusOne tax year"
  }

  // Home Page Messages
  object HomePage {
    val title = "Your Income Tax"
    val heading = "Income Tax"

    object EstimatesSection {
      val heading = "Estimates"
      val paragraph = "Check what you might owe, based on figures you have submitted."
    }
    object BillsSection {
      val heading = "Bills"
      val paragraph = "View your current and previous Income Tax bills."
    }
    object ReportDeadlinesSection {
      val heading = "Report deadlines"
      val paragraph = "Check when your reports are due."
    }
    object StatementSection {
      val heading = "Statements"
      val paragraph = "View your Income Tax transactions, including charges and payments."
    }
    object AccountDetailsSection {
      val heading = "Account details"
      val paragraph = "See contact information and other details we have for your businesses."
    }
  }

  // Estimated Tax Liability Page Messages
  class Calculation(taxYear: Int) {
    val heading = s"${taxYear-1} to $taxYear tax year"
    val subheading = s"Estimates"
    val title = heading
    val reportedFigures = "These estimates are based on the figures you already submitted for this tax year."
    object Crystallised {
      val heading = s"${taxYear-1} to $taxYear tax year"
      val tabTitle = heading
      val subHeading = s"Bills"
      val p1 = "This figure is based on the information you provided in your quarterly reports and final report."
      val warning = s"Your payment could take up to 5 days to process. You may be fined if it is late."
      val breakdownHeading = "How this figure was calculated"
      val errors = "If there are any errors, you can make adjustments through your software."
      val changes = s"If you make these changes before 31 January ${taxYear + 1} you will not be penalised."
      val payNow = "Continue to payment"
      val payDeadline = s"due by 31 January ${taxYear + 1}"
    }
    object EoyEstimate {
      val heading: String => String = eoyEstimate => s"Annual estimate: $eoyEstimate"
      val p1 = s"This is for the ${taxYear-1} to $taxYear tax year."
    }
    object InYearEstimate {
      val heading: String => String = inYearEstimate => s"Current estimate: $inYearEstimate"
      val p1: String => String = calcDate => s"This is for 6 April ${taxYear-1} to $calcDate."
      object CalculationBreakdown {
        val heading = "How we calculated this estimate"
        val nationalRegime = "National Regime"
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
        val heading = "Why your current estimates may change"
        val p1 = "Your estimates could change because:"
        val bullet1 = "rates and allowances will not be applied in full until the end of the tax year"
        val bullet2 = "you may earn more money"
        val bullet3 = "you may have income that is not reported in your accounting software"
      }
    }
    object Bills {
      val billsTitle = "Bills"
      val billsHeading = "Bills"
      val viewBills = "View your finalised bills:"
      val billLink = s"${taxYear-1} to $taxYear tax year"
      val billsLinkAriaLabel = s"view bill for $billLink"
      val noBills = "You've had no bills since you started reporting through software."
      val earlierBills = "For earlier bills, view your Self Assessment calculations (opens in a new tab)."
    }
  }

  // No Estimated Tax Liability Page Messages
  object NoEstimatedTaxLiability {
    val subHeading = "Estimates"
    val heading = "2017 to 2018 tax year"
    val title = heading
    val p1 = "Once you've submitted a report using your accounting software, you can view your tax estimate here."
  }

  // Estimated Tax Liability Error Page Messages
  object EstimatedTaxLiabilityError {
    val subHeading = "Estimates"
    val heading = "2017 to 2018 tax year"
    val title = heading
    val p1 = "We can't display your estimated tax amount at the moment."
    val p2 = "Try refreshing the page in a few minutes."
  }

  //ReportDeadlines Page Messages
  object ReportDeadlines {
    val title = "Report deadlines"
    val propertyHeading = "Property income"
    val periodHeading = "Report period"
    val statusHeading = "Report due date"
    val ceased: String => String = date => s"This business ceased trading on $date."
    val ceasedProperty: String => String = date => s"Ceased trading on $date."
    val portfolio = "This covers all properties that you earn income from."
    val eops = "Whole tax year (final check)"

    object Dropdown {
      val dropdownText = "How to submit a report"
      val dropdownLink = "Choose accounting software that supports this service (opens in a new tab)"
      val dropdown1 = "if you have not already."
      val dropdown2 = "Use your software to record your income and expenses, then send an update to HMRC at least every quarter."
      val dropdown3 = "Send your final report by 31 January. In this report you can add any other income sources, allowances or reliefs."
      val dropdown4 = "After you send your final report, you can see the Income Tax you owe for the tax year."
    }
    object Errors {
      val p1 = "We can't display your next report due date at the moment."
      val p2 = "Try refreshing the page in a few minutes."
    }
  }

  object NoReportDeadlines {
    val title = "Report deadlines"
    val heading = "Report deadlines"
    val noReports = "You don't have any reports due right now. Your next deadline will show here on the first Monday of next month."
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
    val paidBill = "You've paid this bill."
    val transactions = "Your transactions"
    val charge: String => String = amount => s"You had a charge of $amount."
    val youPaid: (String, String) => String = (amount, date) => s"You paid $amount on $date"
    val earlierTransactions = "For earlier transactions, view your Self Assessment."
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
    val ceasedProperties: String => String = date => s"Ceased trading on $date."
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
    val heading = "Income Tax reporting through software"
    val p1 = "You've signed up to report your Income Tax through software. This will replace your Self Assessment tax return."
    val p2 = "You can view your report deadlines, bills and an estimate for the next tax year."
    val button = "View your Income Tax details"
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
    val bta = "Business tax account"
    val it = "Income Tax"
    val estimates = "Estimates"
    val itEstimate: Int => String = taxYear => s"${taxYear-1} to $taxYear tax year"
    val bills = "Bills"
    val finalisedBill: Int => String = taxYear => s"${taxYear-1} to $taxYear tax year"
    val obligations = "Report deadlines"
    val statement = "Income Tax statement"
    val details = "Account details"
  }
}
