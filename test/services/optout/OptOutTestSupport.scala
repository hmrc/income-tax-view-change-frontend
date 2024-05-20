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
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}

object OptOutTestSupport {

  def buildOneYearOptOutDataForPreviousYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForCurrentYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForNextYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildMultiYearOptOutData(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

}