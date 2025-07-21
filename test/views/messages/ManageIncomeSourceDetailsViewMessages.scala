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

import models.core.AddressModel

object ManageIncomeSourceDetailsViewMessages {

  val unknown: String = "Unknown"
  val heading: String = "Manage your details"
  val soleTrader: String = "Sole trader"
  val businessName: String = "Business name"
  val businessAddress: String = "Address"
  val dateStarted: String = "Date started"
  val typeOfTrade: String = "Type of trade"
  val isTraditionalAccountingMethod: String = "Traditional accounting"
  val ukAccountingMethod: String = "Accounting method"
  val quarterlyPeriodType: String = "Update period"
  val foreignAccountingMethod: String = "Accounting method"
  val reportingMethod1: String = "Reporting frequency 2022 to 2023"
  val reportingMethod2: String = "Reporting frequency 2023 to 2024"

  val change: String = "Change"
  val quarterly: String = "Quarterly"
  val annually: String = "Annual"

  val quarterlyGracePeriod: String = "Quarterly"
  val annuallyGracePeriod: String = "Annual"

  val cashBasisAccounting: String = "Cash basis accounting"

  val standard: String = "Standard"
  val calendar: String = "Calendar"

  val expectedAddress: Option[AddressModel] = Some(AddressModel(Some("Line 1"), Some("Line 2"), Some("Line 3"), Some("Line 4"), Some("LN12 2NL"), Some("NI")))

  val expectedViewAddressString1: String = "Line 1 Line 2 Line 3 Line 4 LN12 2NL United Kingdom"

  val expectedBusinessName: String = "Business income"
  val expectedBusinessStartDate: String = "1 January 2022"
  val expandableInfoStandardSummary: String = "What is a standard quarterly period?"
  val expandableInfoStandardContentP1: String = "This business is reporting from 6 April in line with the tax year, also known as using standard update periods."
  val expandableInfoStandardContentP2: String = "If your software supports it, you can choose to report using calendar update periods which end on the last day of the month."

  val expandableInfoContentP3: String = "Learn more about standard and calendar quarters"
  val expandableMoreInfoLink = "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"
  val opensInNewTabText: String = "(opens in new tab)"
  val reportingFrequencyText: String = "View and change your reporting frequency for all your businesses"
  val newBusinessInsetText: String = "Because this is still a new business, you can change how often you report for it for up to 2 tax years. From April 2024, you could be required to report quarterly."


}
