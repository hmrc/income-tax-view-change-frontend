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

object Messages {

  // Estimated Tax Liability Page Messages
  object EstimatedTaxLiability {
    val pageHeading = "Your tax estimate"
    val taxYear = "2017 to 2018 tax year"
    val title = taxYear + " " + pageHeading
    object EstimateTax {
      val calcDate: String => String = date => s"""Estimate up to your <span id="calc-date">$date</span> submission"""
      val changes = "Your estimate is a current figure and could change because:"
      val changesBullet1 = "it's based on figures from the start of your accounting period up to your last report"
      val changesBullet2 = "rates and allowances won't be applied in full until the end of the tax year"
      val changesBullet3 = "you may earn more money"
      val changesBullet4 = "you may have income that's not reported in your software"
      val payment: String => String = date => s"You must pay any tax you owe for the whole tax year by $date."
      val toDate = "Estimate to date"
    }
  }

  // No Estimated Tax Liability Page Messages
  object NoEstimatedTaxLiability {
    val pageHeading = "Your tax estimate"
    val taxYear = "2017 to 2018 tax year"
    val title = taxYear + " " + pageHeading
    val p1 = "Once you've submitted a report using your accounting software, you can view your tax estimate here."
  }

  //Obligations Page Messages
  object Obligations {
    val title = "Your report deadlines"
    val info  = "You must submit a report once every quarter using your accounting software."
    val businessHeading = "Business income"
    val propertyHeading = "Property income"
    val periodHeading = "Report period"
    val statusHeading = "Report due date"
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
    val futureTaxYearsHeading = "Future tax years"
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
}
