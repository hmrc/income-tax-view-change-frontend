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
import models.itsaStatus.ITSAStatus

case class OptOutProposition(previousTaxYear: PreviousOptOutTaxYear,
                             currentTaxYear: CurrentOptOutTaxYear,
                             nextTaxYear: NextOptOutTaxYear) {

  private val optOutYears: Seq[OptOutTaxYear] = Seq[OptOutTaxYear](
    previousTaxYear,
    currentTaxYear,
    nextTaxYear)

  def availableOptOutYears: Seq[OptOutTaxYear] = optOutYears.filter(_.canOptOut)

  private lazy val countVoluntaryOptOutYears: Int = availableOptOutYears.size

  def optOutForSingleYear[T](function: (OptOutProposition, OptOutTaxYear) => T): Option[T] = {
    if (countVoluntaryOptOutYears == 1) Some(function(this, availableOptOutYears.head)) else
      None
  }

  val isOneYearOptOut: Boolean = availableOptOutYears.size == 1
  val isMultiYearOptOut: Boolean = availableOptOutYears.size > 1
  val isNoOptOutAvailable: Boolean = availableOptOutYears.isEmpty

  def optOutYearsToUpdate(intent: OptOutTaxYear) : Seq[OptOutTaxYear] = {

    val intentYearAndOnwards = availableOptOutYears.filter(offered => offered.taxYear.isSameAs(intent.taxYear) ||
      offered.taxYear.isAfter(intent.taxYear))

    val nextYearIsOffered = nextTaxYear.canOptOut
    val nextYearIsNoStatus = nextTaxYear.status == ITSAStatus.NoStatus
    val nextYearIsNotIntent = !intent.taxYear.isSameAs(nextTaxYear.taxYear)

    val nextYearShouldBeExcludedFromUpdate = nextYearIsOffered && nextYearIsNoStatus && nextYearIsNotIntent
    if(nextYearShouldBeExcludedFromUpdate) {
      intentYearAndOnwards.filter(offered => offered.taxYear.isBefore(nextTaxYear.taxYear))
    } else intentYearAndOnwards
  }

  def optOutTaxYearFor(taxYear: TaxYear): Option[OptOutTaxYear] =
    availableOptOutYears.find(offered => offered.taxYear.isSameAs(taxYear))

}
