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

package services.reportingObligations.optOut

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import services.reportingObligations.optOut.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}

object OptOutTestSupport {

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  object Crystallised {
    val YES = true
    val NO = false
  }

  object OneYearOptOut {
    val YES = true
    val NO = false
  }
  object MultiYearOptOut {
    val YES = true
    val NO = false
  }

  object ToBeOffered {

    val NoOffers = Seq()

    val PY = Seq("PY")
    val CY = Seq("CY")
    val NY = Seq("NY")

    val PY_CY_NY = Seq("PY", "CY", "NY")

    val PY_CY = Seq("PY", "CY")
    val CY_NY = Seq("CY", "NY")
    val PY_NY = Seq("PY", "NY")
  }

  object Intent {
    val PY = "PY"
    val CY = "CY"
    val NY = "NY"
  }

  def buildOneYearOptOutPropositionForPreviousYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      nextTaxYearOptOut
    )
  }

  def buildOneYearOptOutPropositionForCurrentYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      nextTaxYearOptOut
    )
  }

  def buildOneYearOptOutPropositionForNextYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      nextTaxYearOptOut
    )
  }

  def buildThreeYearOptOutProposition(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      nextTaxYearOptOut
    )
  }

  def buildTwoYearOptOutPropositionOfferingPYAndNY(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildTwoYearOptOutPropositionOfferingPYAndCY(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      nextTaxYearOptOut
    )
  }

}