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
import models.itsaStatus.ITSAStatus.ITSAStatus

case class OptOutProposition(
                              previousTaxYear: PreviousOptOutTaxYear,
                              currentTaxYear: CurrentOptOutTaxYear,
                              nextTaxYear: NextOptOutTaxYear
                            ) {

  private val optOutYears: Seq[OptOutTaxYear] =
    Seq(
      previousTaxYear,
      currentTaxYear,
      nextTaxYear
    )

  val availableTaxYearsForOptOut: Seq[TaxYear] = availableOptOutYears.map(_.taxYear)

  lazy val availableOptOutYears: Seq[OptOutTaxYear] = optOutYears.filter(_.canOptOut)

  val isOneYearOptOut: Boolean = availableOptOutYears.size == 1
  val isMultiYearOptOut: Boolean = availableOptOutYears.size > 1
  val isNoOptOutAvailable: Boolean = availableOptOutYears.isEmpty

  def isCurrentYearQuarterly: Boolean = currentTaxYear.status == ITSAStatus.Voluntary || currentTaxYear.status == ITSAStatus.Mandated

  def isNextYearQuarterly: Boolean = nextTaxYear.status == ITSAStatus.Voluntary || nextTaxYear.status == ITSAStatus.Mandated

  def optOutYearsToUpdate(intent: TaxYear): Seq[TaxYear] = {
    availableOptOutYears.filter(_.shouldBeUpdated(intent)).map(_.taxYear)
  }

  def optOutPropositionType: Option[OptOutPropositionTypes] = {
    (isOneYearOptOut, isMultiYearOptOut) match {
      case (true, false) => Some(OneYearOptOutProposition(this))
      case (false, true) => Some(MultiYearOptOutProposition(this))
      case _ => None
    }
  }

  def expectedItsaStatusesAfter(customerIntent: TaxYear): Seq[ITSAStatus] = {
    Seq(
      previousTaxYear.expectedItsaStatusAfter(customerIntent),
      currentTaxYear.expectedItsaStatusAfter(customerIntent),
      nextTaxYear.expectedItsaStatusAfter(customerIntent))
  }

  def areAllTaxYearsMandated: Boolean =
    previousTaxYear.status == ITSAStatus.Mandated && currentTaxYear.status == ITSAStatus.Mandated && nextTaxYear.status == ITSAStatus.Mandated
}

object OptOutProposition {

  def createOptOutProposition(currentYear: TaxYear,
                              previousYearCrystallised: Boolean,
                              previousYearItsaStatus: ITSAStatus,
                              currentYearItsaStatus: ITSAStatus,
                              nextYearItsaStatus: ITSAStatus): OptOutProposition = {

    val previousYearOptOut = PreviousOptOutTaxYear(
      status = previousYearItsaStatus,
      taxYear = currentYear.previousYear,
      crystallised = previousYearCrystallised
    )

    val currentYearOptOut = CurrentOptOutTaxYear(
      status = currentYearItsaStatus,
      taxYear = currentYear
    )

    val nextYearOptOut = NextOptOutTaxYear(
      status = nextYearItsaStatus,
      taxYear = currentYear.nextYear,
      currentTaxYear = currentYearOptOut
    )

    OptOutProposition(previousYearOptOut, currentYearOptOut, nextYearOptOut)
  }
}