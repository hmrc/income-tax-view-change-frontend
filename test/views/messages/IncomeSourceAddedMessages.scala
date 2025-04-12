/*
 * Copyright 2025 HM Revenue & Customs
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

package views.messages

object IncomeSourceAddedMessages {

  val h1ForeignProperty: String = "Foreign property"
  val h1UKProperty: String = "UK property"
  val h1SoleTraderContent: String = "Test Name"
  val headingBase: String = "has been added to your account"
  val submitTaxReturn: String = "Submit your tax return"
  val submitUpdatesInSoftware: String = "Submit updates in software"
  val yourRevisedDeadlinesHeading: String = "Your revised deadlines"
  val quarterlyText: String = "You must send quarterly updates of your income and expenses using compatible software by the following deadlines:"
  val finalDecHeading: String = "Submit final declarations and pay your tax"
  val finalDecText: String = "You must submit your final declarations and pay the tax you owe by the deadline."
  val tableHeading1: String = "Tax year"
  val tableHeading2: String = "Deadline"
  val prevYearsHeading: String = "Previous tax years"
  val prevYearsText: String = "You must make sure that you have sent all the required income and expenses, and final declarations for tax years earlier than"
  val viewAllBusinessesText: String = "View all your businesses"
  val insetSingleOverdueUpdateText: (String, Int) => String = (month, year) => s"As your business started in $month $year, you have 1 overdue update."
  val insetMultipleOverdueUpdateText: (String, Int, Int) => String = (month, year, overdueUpdates) => s"As your business started in $month $year, you have $overdueUpdates overdue updates."
  val insetWarningOverdueUpdatesText: Int => String = startTaxYear => s"You must make sure that you have sent all the required income and expenses for tax years earlier than $startTaxYear to ${startTaxYear + 1}."
}