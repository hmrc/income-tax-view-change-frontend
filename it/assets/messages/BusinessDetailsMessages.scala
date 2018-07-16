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

package assets.messages

import java.time.LocalDate

import helpers.ComponentSpecBase

object BusinessDetailsMessages extends ComponentSpecBase {

  val reportingPeriod: (LocalDate,LocalDate) => String =
    (start,end) => s"Reporting period: ${start.toLongDateNoYear} - ${end.toLongDateNoYear}"
  val cessationDate: LocalDate => String =
    date => s"This business ceased trading on ${date.toLongDate}."
  val addressDetails = "Address and contact details"
  val tradingName = "Trading name"
  val businessAddress = "Business address"
  val additionalInformation = "Additional information"
  val accountingMethod = "This business uses the cash accounting method."
}
