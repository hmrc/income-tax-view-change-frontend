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

package testConstants.messages

import helpers.servicemocks.AuthStub.{messagesAPI, lang}

object TaxDueSummaryMessages {

  val voluntaryClass2Nics: String = messagesAPI("taxCal_breakdown.table.nic2.true")
  val nonVoluntaryClass2Nics: String = messagesAPI("taxCal_breakdown.table.nic2.false")
  val additionCharges: String = messagesAPI("taxCal_breakdown.additional_charges")
}
