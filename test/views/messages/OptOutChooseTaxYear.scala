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


object OptOutChooseTaxYear {

  val heading: String = "Opting out of quarterly reporting"
  val title: String = "Opting out of quarterly reporting - Manage your Self Assessment - GOV.UK"
  val summary: String = "You can opt out from any of the tax years available and report annually from that year onwards. This means you’ll then report annually for all of your current businesses and any that you add in future."
  val whichTaxYear: String = "Which tax year do you want to opt out from?"
  val cancelButton: String = "Cancel"
  val continueButton: String = "Continue"

  def warningInsertMessage(i: Int): String =
    s"You have $i quarterly updates submitted for this tax year. If you continue, these updates will be deleted from our records. You will need to include any income from these updates in your tax return."

}