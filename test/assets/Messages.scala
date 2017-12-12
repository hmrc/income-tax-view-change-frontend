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

import play.twirl.api.Html

object Messages {

  //Estimates Page Messages
  class Estimates {
    val title = "Income Tax Estimates"
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
      val directDebit = "Check if you already pay by direct debit."
      val warning = s"If you pay this bill after 31 January ${taxYear + 1} you'll get penalties."
      val breakdownHeading = "How this figure was calculated"
      val errors = "If there are any errors, you can make adjustments through your software."
      val changes = s"If you make changes before 31 January ${taxYear + 1} you will not be penalised."
      val aPHeading = "Additional payment"
      val advancedPayment = s"An advanced payment on account of ADVANCED-PAYMENT-AMOUNT will be also due by 31 July ${taxYear + 1}" //needs changing once the advanced payment amount is returned
      val aboutPoA = "About payments on account"
      val aPp1 = "These are obligatory payments towards your next tax year. You make two of them: one is paid at the end of the tax year, the other is paid 6 months later."
      val aPp2 = "Each payment is half of the tax you owed this year."
      val aPp3 = "You'll be prompted to pay this through your Income Tax account."
      val aPp4 = "If you think you will earn less next tax year, you can reduce your payment on account online."
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
        val personalAllowance = "Personal Allowance (for the period you've reported)"
        val yourTaxableIncome = "Your taxable income"
        val atBR: String => String = amount => s"Income Tax ($amount at 20%)"
        val atHR: String => String = amount => s"Income Tax ($amount at 40%)"
        val atAR: String => String = amount => s"Income Tax ($amount at 45%)"
        val nic2 = "Class 2 National Insurance"
        val nic4 = "Class 4 National Insurance"
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
    object Errors {
      val p1 = "We can't display your next report due date at the moment."
      val p2 = "Try refreshing the page in a few minutes."
    }
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

  object Sidebar {
    val mtditidHeading = "Income Tax reference:"
    val reportsHeading = "Deadlines"
    val reportsLink = "View report deadlines"
    val estimatesHeading = "Estimates"
    val estimatesLink = "View your tax estimate"
    val previousTaxYearsHeading = "Previous tax years"
    val selfAssessmentLink = "View annual returns"
    val estimatesLinkYear: Int => String = year => s"View ${year - 1} to $year details"
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
}
