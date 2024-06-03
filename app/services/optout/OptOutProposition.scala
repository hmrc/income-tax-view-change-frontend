/*
 * Copyright 2024 HM Revenue & Customs
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

package services.optout

import models.incomeSourceDetails.TaxYear


case class OptOutProposition(previousTaxYear: PreviousOptOutTaxYear,
                             currentTaxYear: CurrentOptOutTaxYear,
                             nextTaxYear: NextOptOutTaxYear) {

  private val optOutYears: Seq[OptOutTaxYear] = Seq[OptOutTaxYear](
    previousTaxYear,
    currentTaxYear,
    nextTaxYear)

  lazy val availableOptOutYears: Seq[OptOutTaxYear] = optOutYears.filter(_.canOptOut)


  val isOneYearOptOut: Boolean = availableOptOutYears.size == 1
  val isMultiYearOptOut: Boolean = availableOptOutYears.size > 1
  val isNoOptOutAvailable: Boolean = availableOptOutYears.isEmpty

  def optOutYearsToUpdate(intent: OptOutTaxYear): Seq[OptOutTaxYear] = {
    availableOptOutYears.filter(_.shouldBeUpdated(intent))
  }

  def optOutTaxYearFor(taxYear: TaxYear): Option[OptOutTaxYear] =
    availableOptOutYears.find(offered => offered.taxYear.isSameAs(taxYear))

  def optOutPropositionType: Option[OptOutPropositionTypes] = {
    (isOneYearOptOut, isMultiYearOptOut) match {
      case (true, _) => Some(OneYearOptOutProposition(this))
      case (_, true) => Some(MultiYearOptOutProposition(this))
      case _ => None
    }
  }

}
