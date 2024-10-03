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

package services.optIn.core

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus}
import services.optIn.core.OptInProposition.OneItem

object OptInProposition {
  val OneItem = 1

  def createOptInProposition(currentYear: TaxYear,
                             nextYear: TaxYear,
                             initialState: OptInInitialState): OptInProposition = {

    val currentOptInTaxYear =
      CurrentOptInTaxYear(
        status = initialState.currentYearItsaStatus,
        taxYear = currentYear
      )

    val nextYearOptOut =
      NextOptInTaxYear(
        status = initialState.nextYearItsaStatus,
        taxYear = nextYear,
        currentOptInTaxYear = currentOptInTaxYear
      )

    OptInProposition(currentOptInTaxYear, nextYearOptOut)
  }

}

case class OptInProposition(currentTaxYear: CurrentOptInTaxYear, nextTaxYear: NextOptInTaxYear) {

  private val optInYears: Seq[OptInTaxYear] = Seq[OptInTaxYear](currentTaxYear, nextTaxYear)

  lazy val availableOptInYears: Seq[OptInTaxYear] = optInYears.filter(_.canOptIn)

  val availableTaxYearsForOptIn: Seq[TaxYear] = availableOptInYears.map(_.taxYear)

  val isOneYearOptIn: Boolean = availableOptInYears.size == OneItem
  val isTwoYearOptIn: Boolean = availableOptInYears.size > OneItem
  val isNoOptInAvailable: Boolean = availableOptInYears.isEmpty

  def optInPropositionType: Option[OptInPropositionTypes] = {
    (isOneYearOptIn, isTwoYearOptIn) match {
      case (true, false) => Some(OneYearOptInProposition(this))
      case (false, true) => Some(MultiYearOptInProposition(this))
      case _ => None
    }
  }

  def isCurrentTaxYear(target: TaxYear): Boolean = currentTaxYear.taxYear == target

  def anyYearsAnnual: Boolean = Seq(currentTaxYear.status, nextTaxYear.status).contains(Annual)

//  def afterwardsStatus(selectedTaxYear: TaxYear): Seq[ITSAStatus] = {
//
//  }


}