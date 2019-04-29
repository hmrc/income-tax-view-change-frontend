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

object CalculationMessages {

  val billsTitle: Int => String = year => s"Income tax bill for ${year - 1} to $year"
  val title: Int => String = year => s"${year - 1} to $year tax year"
  val heading: Int => String = billsTitle(_)
  val estimateTitle: Int => String = year => s"Tax estimate for ${year - 1} - $year"
  val estimateHeading: Int => String = estimateTitle(_)

  object EstimatedTaxAmount {
    val subHeading = "Estimates"
    val currentEstimate: String => String = estimate => s"Current estimate: $estimate"
    val inYearp1: (String, Int) => String = (calcDate, year) => s"This is for 6 April ${year-1} to ${calcDate}."
  }

  object CrystalisedTaxAmount {
    val subHeading = "Bills"
    val calcBreakdown = "How this figure was calculated"
  }

  val internalServerErrorp1 = "We can't display your estimated tax amount at the moment."
  val internalServerErrorp2 = "Try refreshing the page in a few minutes."
}
