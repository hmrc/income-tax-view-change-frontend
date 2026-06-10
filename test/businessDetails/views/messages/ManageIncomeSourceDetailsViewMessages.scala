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

package businessDetails.views.messages

import common.models.core.AddressModel

object ManageIncomeSourceDetailsViewMessages {

  val unknown: String = "Unknown"
  val heading: String = "Manage your details"
  val soleTrader: String = "Sole trader"
  val businessName: String = "Business name"
  val businessAddress: String = "Address"
  val dateStarted: String = "Date started"
  val typeOfTrade: String = "Type of trade"
  val quarterlyPeriodType: String = "Update period"
  val reportingMethod1: String = "Using Making Tax Digital for Income Tax for 2022 to 2023"
  val reportingMethod2: String = "Using Making Tax Digital for Income Tax for 2023 to 2024"

  val change: String = "Change"
  val quarterly: String = "Quarterly"
  val annually: String = "Annual"
  val signUp: String = "Sign up"
  val optOut: String = "Opt out"

  val quarterlyGracePeriod: String = "Yes"
  val annuallyGracePeriod: String = "No"

  val standard: String = "Standard"
  val calendar: String = "Calendar"

  val expectedAddress: Option[AddressModel] = Some(AddressModel(Some("Line 1"), Some("Line 2"), Some("Line 3"), Some("Line 4"), Some("LN12 2NL"), Some("NI")))

  val expectedViewAddressString1: String = "Line 1 Line 2 Line 3 Line 4 LN12 2NL United Kingdom"

  val expectedBusinessName: String = "Business income"
  val expectedBusinessStartDate: String = "1 January 2022"

  val expandableInfoStandardSummary: String = "What is a standard quarterly period?"
  val expandableInfoStandardContentP1: String = "Standard update periods align to the tax year (6 April to 5 April)."
  val expandableInfoStandardContentP2: String = "If your accounting period doesn’t end on 31 March, standard update periods will make your record keeping simpler."
  val expandableInfoStandardContentP3: String = "You can choose to report using calendar update periods (which run from 1 April to 31 March) instead. You can only make the change:"
  val expandableInfoStandardContentBullet1: String = "in compatible software that supports this option"
  val expandableInfoStandardContentBullet2: String = "before you send your first quarterly update of the tax year"

  val expandableInfoContentP3: String = "Learn more about standard and calendar quarters"
  val expandableMoreInfoLink = "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"
  val opensInNewTabText: String = "(opens in new tab)"

  val reportingFrequencyText: String = "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
  val newBusinessInsetText: String = "Because this is still a new business, for up to 2 tax years you can choose if you want to use Making Tax Digital for Income Tax. From April 2024, you could be required to use the service."
}
