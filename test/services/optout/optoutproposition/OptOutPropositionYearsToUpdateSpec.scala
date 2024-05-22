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
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, NoStatus, Voluntary}
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec

import OptOutPropositionYearsToUpdateSpec._

object OptOutPropositionYearsToUpdateSpec {

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  val Crystallised = true
  val NotCrystallised = false

  val OneYearOptOut = true
  val NotOneYearOptOut = false

  val MultiYearOptOut = true
  val NotMultiYearOptOut = false

  object ToBeUpdated {

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
}

class OptOutPropositionYearsToUpdateSpec extends UnitSpec {

  val testCases = List(

    ((NotCrystallised, Voluntary, Voluntary, Voluntary), Intent.PY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.PY_CY_NY),
    ((NotCrystallised, Voluntary, Voluntary, Voluntary), Intent.CY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.CY_NY),
    ((NotCrystallised, Voluntary, Voluntary, Voluntary), Intent.NY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.NY),

    ((NotCrystallised, Voluntary, Voluntary, NoStatus), Intent.PY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.PY_CY),
    ((NotCrystallised, Voluntary, Voluntary, NoStatus), Intent.CY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.CY),
    ((NotCrystallised, Voluntary, Voluntary, NoStatus), Intent.NY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.NY),

    ((Crystallised, Voluntary, Voluntary, Voluntary), Intent.CY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.CY_NY),
    ((Crystallised, Voluntary, Voluntary, Voluntary), Intent.NY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.NY),
    ((Crystallised, Voluntary, Voluntary, NoStatus), Intent.NY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.NY),

    ((NotCrystallised, Voluntary, Mandated, Voluntary), Intent.PY, (NotOneYearOptOut, MultiYearOptOut), ToBeUpdated.PY_NY),
    ((NotCrystallised, Voluntary, Mandated, NoStatus), Intent.PY, (OneYearOptOut, NotMultiYearOptOut), ToBeUpdated.PY),
    ((NotCrystallised, Voluntary, Mandated, Mandated), Intent.PY, (OneYearOptOut, NotMultiYearOptOut), ToBeUpdated.PY),
    ((Crystallised, Voluntary, Voluntary, Mandated), Intent.CY, (OneYearOptOut, NotMultiYearOptOut), ToBeUpdated.CY),

    ((Crystallised, Voluntary, Mandated, Voluntary), Intent.NY, (OneYearOptOut, false), ToBeUpdated.NY),

  )

  testCases.foreach {
    case (input, intent, numberOfYearsFlags, output) =>
      val test = optOutPropositionUpdatesTest _
      test.tupled(input)(intent).tupled(numberOfYearsFlags)(output)
  }

  def optOutPropositionUpdatesTest(crystallised: Boolean, pyStatus: ITSAStatus, cyStatus: ITSAStatus, nyStatus: ITSAStatus)
                                  (intent: String)
                                  (isOneYearOptOut: Boolean, isMultiYearOptOut: Boolean)
                                  (expectedToUpdate: Seq[String]): Unit = {

    s"update years ${expectedToUpdate.mkString(",")}" when {
      s"proposition is ${(crystallised, pyStatus, cyStatus, nyStatus)}" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(pyStatus, previousTaxYear, crystallised = crystallised)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(cyStatus, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(nyStatus, nextTaxYear, currentTaxYearOptOut)

        val intentYear = intent match {
          case "PY" => previousTaxYearOptOut
          case "CY" => currentTaxYearOptOut
          case _ => nextTaxYearOptOut
        }

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe isOneYearOptOut
        optOutData.isMultiYearOptOut shouldBe isMultiYearOptOut
        optOutData.isNoOptOutAvailable shouldBe !isOneYearOptOut && !isMultiYearOptOut

        optOutData.optOutYearsToUpdate(intentYear).map {
          case _:PreviousOptOutTaxYear => "PY"
          case _:CurrentOptOutTaxYear => "CY"
          case _ => "NY"
        }.sortBy(_.trim) shouldBe expectedToUpdate.sortBy(_.trim)

      }
    }
  }

}
