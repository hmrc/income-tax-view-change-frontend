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
    val title = "Your estimated tax amount"
    val heading = "Your estimated tax amount"
    val preheading = "2017/18"
    object EstimateTax {
      val h2 = "Your estimated tax amount"
      val p1 = "This estimate is calculated using the figures you've submitted using your accounting software. " +
        "The amount includes both your Income Tax and National Insurance."
      val payment = "You must pay any tax you owe for the whole tax year by 31 January 2019."
      val toDate = "Estimate to date"
    }
  }

  //Obligations Page Messages
  object Obligations {
    val title = "Your Income Tax reports"
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
    val mtditidHeading = "Quarterly reporting reference:"
    val reportsHeading = "Reports"
    val reportsLink = "View your Income Tax reports"
    val estimatesHeading = "Estimates"
    val estimatesLink = "View your estimated tax amount"
  }

  object ISE {
    val title = "Sorry, we are experiencing technical difficulties - 500"
  }
}
