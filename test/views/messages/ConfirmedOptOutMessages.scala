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

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.reportingObligations.optOut.{CurrentOptOutTaxYear, OptOutTaxYear}

object ConfirmedOptOutMessages {

  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val optOutTaxYear: OptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, taxYear)

  val singleYearReportingUpdatesInset = s"From 6 April ${taxYear.endYear}, you’ll be required to send quarterly updates through software compatible with Making Tax Digital for Income Tax (opens in new tab)"
  val singleYearReportingUpdatesListP1 = "HMRC lowered the income threshold for Making Tax Digital"
  val singleYearReportingUpdatesListP2 = "you reported an increase in your qualifying income in last year’s tax return"
  val singleYearReportingUpdatesP1 = "This could be because:"
  val singleYearReportingUpdatesHeading = "Reporting quarterly from next tax year onwards"
  val singleYearSoftwareCompatibleLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"


  val heading: String = "Opt out completed"
  val title: String =  "Opt out completed - Manage your Self Assessment - GOV.UK"
  val panelBodyOneYear: String = s"You are reporting annually for the ${taxYear.startYear} to ${taxYear.endYear} tax year"
  val panelBodyMultiYear: String = s"You are reporting annually from the ${taxYear.startYear} to ${taxYear.endYear} tax year onwards"

  val submitTaxHeading: String = "Submit your tax return"
  val submitTaxP1: String = "Now you have opted out, you will need to go back to the way you have previously filed your Self Assessment tax return (opens in new tab)."
  val submitTaxP1Link: String = "filed your Self Assessment tax return"
  val submitTaxP2: String = "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
  val submitTaxP2Link: String = "software compatible with Making Tax Digital for Income Tax"

  val yourRevisedDeadlinesHeading: String = "Your revised deadlines"
  val yourRevisedDeadlinesContentP1: String = s"Your tax return for the ${taxYear.startYear} to ${taxYear.endYear} tax year is due by 31 January ${taxYear.nextYear.endYear}."
  val yourRevisedDeadlinesContentP2: String = "You can decide at any time to opt back in to reporting quarterly for all of your businesses on your reporting frequency page."
  val reportQuarterly: String = "You could be required to report quarterly again in the future if:"
  val multiYearReportingUpdatesHeading = "Reporting quarterly again in the future"
  val multiYearReportingUpdatesP1 = "You could be required to report quarterly again in the future if:"
  val multiYearReportingUpdatesListP1 = "HMRC lowers the income threshold for Making Tax Digital"
  val multiYearReportingUpdatesListP2 = "you report an increase in your qualifying income in a tax return"
  val multiYearReportingUpdatesInset = s"For example, if your qualifying income exceeds the threshold in the ${optOutTaxYear.taxYear.startYear} to ${optOutTaxYear.taxYear.endYear} tax year, you would have to report quarterly from 6 April ${optOutTaxYear.taxYear.nextYear.endYear}."
  val multiYearReportingUpdatesP2 = "If this happens, we will write to you to let you know."
  val multiYearReportingUpdatesP3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital (opens in new tab) ."
  val multiYearReportingUpdatesP3Link = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-will-need-to-sign-up"
}