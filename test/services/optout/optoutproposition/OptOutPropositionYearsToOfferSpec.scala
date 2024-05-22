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

class OptOutPropositionYearsToOfferSpec extends UnitSpec {

  val testCases = List(
    ((false, Voluntary, Voluntary, Voluntary), (false, true), Seq("PY", "CY", "NY")),
    ((true, Voluntary, Voluntary, Voluntary), (false, true), Seq("CY", "NY")),
    ((true, Voluntary, Voluntary, NoStatus), (false, true), Seq("CY", "NY")),
    ((false, Voluntary, Voluntary, Mandated), (false, true), Seq("PY", "CY")),
    ((false, Voluntary, Mandated, Voluntary), (false, true), Seq("PY", "NY")),
  )

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

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

  testCases.foreach {
    case (input, numberOfYearsFlags, output) =>
      val test = optOutPropositionOffersTest _
      test.tupled(input).tupled(numberOfYearsFlags)(output)
  }

}
