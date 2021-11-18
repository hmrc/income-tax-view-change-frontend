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

object TaxYearOverviewMessages {

  val title: String = "Tax year overview - Business Tax account - GOV.UK"
  val agentTitle: String = "Tax year overview - Your client’s Income Tax details - GOV.UK"
  val heading: String = "Tax year overview"
  val headingNoCalcData: String = "No calculation yet"
  val noCalcDataNote: String = "You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software."

  def titleOld(firstYear: Int, secondYear: Int): String = s"6 April $firstYear to 5 April $secondYear - Business Tax account - GOV.UK"
  def headingOld(firstYear: Int, secondYear: Int): String = s"Tax year overview 6 April $firstYear to 5 April $secondYear"
  def calculationDateOld(date: String): String = s"Calculation date: $date"

}
