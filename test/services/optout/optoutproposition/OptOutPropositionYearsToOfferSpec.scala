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

import models.itsaStatus.ITSAStatus._
import services.optout.OptOutTestSupport._
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec

class OptOutPropositionYearsToOfferSpec extends UnitSpec {

  private val testCases = List(
    ((Crystallised.YES, Mandated, Mandated, Mandated), (OneYearOptOut.NO, MultiYearOptOut.NO), ToBeOffered.NoOffers),

    ((Crystallised.NO, Voluntary, Mandated, Mandated), (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeOffered.PY),
    ((Crystallised.YES, Voluntary, Voluntary, Mandated), (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeOffered.CY),
    ((Crystallised.YES, Voluntary, Mandated, Voluntary), (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeOffered.NY),
    ((Crystallised.YES, Voluntary, Voluntary, NoStatus), (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeOffered.CY_NY),

    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeOffered.PY_CY_NY),
    ((Crystallised.YES, Voluntary, Voluntary, Voluntary), (OneYearOptOut.NO, MultiYearOptOut.YES),ToBeOffered.CY_NY ),
    ((Crystallised.YES, Mandated, Voluntary, NoStatus), (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeOffered.CY_NY),
    ((Crystallised.NO, Voluntary, Voluntary, Mandated), (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeOffered.PY_CY),
    ((Crystallised.NO, Voluntary, Mandated, Voluntary), (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeOffered.PY_NY),
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
