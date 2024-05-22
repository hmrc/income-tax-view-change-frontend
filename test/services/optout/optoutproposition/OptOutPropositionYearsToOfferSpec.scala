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

package services.optout.optoutproposition

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, OptOutTaxYear, PreviousOptOutTaxYear}
import testUtils.UnitSpec

import OptOutPropositionYearsToOfferSpec._

object OptOutPropositionYearsToOfferSpec {
  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  val Crystallised = true
  val NotCrystallised = false

  val OneYearOptOut = true
  val NotOneYearOptOut = false

  val MultiYearOptOut = true
  val NotMultiYearOptOut = false

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
}

class OptOutPropositionYearsToOfferSpec extends UnitSpec {

  private val testCases = List(
    ((Crystallised, Mandated, Mandated, Mandated), (NotOneYearOptOut, NotMultiYearOptOut), ToBeOffered.NoOffers),

    ((NotCrystallised, Voluntary, Mandated, Mandated), (OneYearOptOut, NotMultiYearOptOut), ToBeOffered.PY),
    ((Crystallised, Voluntary, Voluntary, Mandated), (OneYearOptOut, NotMultiYearOptOut), ToBeOffered.CY),
    ((Crystallised, Voluntary, Mandated, Voluntary), (OneYearOptOut, NotMultiYearOptOut), ToBeOffered.NY),
    ((Crystallised, Voluntary, Voluntary, NoStatus), (NotOneYearOptOut, MultiYearOptOut), ToBeOffered.CY_NY),

    ((NotCrystallised, Voluntary, Voluntary, Voluntary), (NotOneYearOptOut, MultiYearOptOut), ToBeOffered.PY_CY_NY),
    ((Crystallised, Voluntary, Voluntary, Voluntary), (NotOneYearOptOut, MultiYearOptOut),ToBeOffered.CY_NY ),
    ((Crystallised, Mandated, Voluntary, NoStatus), (NotOneYearOptOut, MultiYearOptOut), ToBeOffered.CY_NY),
    ((NotCrystallised, Voluntary, Voluntary, Mandated), (NotOneYearOptOut, MultiYearOptOut), ToBeOffered.PY_CY),
    ((NotCrystallised, Voluntary, Mandated, Voluntary), (NotOneYearOptOut, MultiYearOptOut), ToBeOffered.PY_NY),
  )

  testCases.foreach {
    case (input, numberOfYearsFlags, output) =>
      val test = optOutPropositionOffersTest _
      test.tupled(input).tupled(numberOfYearsFlags)(output)
  }

  def optOutPropositionOffersTest(crystallised: Boolean, pyStatus: ITSAStatus, cyStatus: ITSAStatus, nyStatus: ITSAStatus)
              (isOneYearOptOut: Boolean, isMultiYearOptOut: Boolean)
              (expectedOffered: Seq[String]): Unit = {

    s"offer years ${expectedOffered.mkString(",")}" when {
      s"proposition is ${(crystallised, pyStatus, cyStatus, nyStatus)}" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(pyStatus, previousTaxYear, crystallised = crystallised)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(cyStatus, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(nyStatus, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe isOneYearOptOut
        optOutData.isMultiYearOptOut shouldBe isMultiYearOptOut
        optOutData.isNoOptOutAvailable shouldBe !isOneYearOptOut && !isMultiYearOptOut

        optOutData.availableOptOutYears.map {
          case _:PreviousOptOutTaxYear => "PY"
          case _:CurrentOptOutTaxYear => "CY"
          case _ => "NY"
        }.sortBy(_.trim) shouldBe expectedOffered.sortBy(_.trim)
      }
    }
  }



}
