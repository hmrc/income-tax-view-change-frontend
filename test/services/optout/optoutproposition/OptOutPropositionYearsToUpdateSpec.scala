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

import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, NoStatus, Voluntary}
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec

import services.optout.OptOutTestSupport._


class OptOutPropositionYearsToUpdateSpec extends UnitSpec {

  val testCases = List(

    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), Intent.PY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.PY_CY_NY),
    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), Intent.CY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.CY_NY),
    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), Intent.NY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.NY),

    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), Intent.PY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.PY_CY),
    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), Intent.CY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.CY),
    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), Intent.NY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.NY),

    ((Crystallised.YES, Voluntary, Voluntary, Voluntary), Intent.CY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.CY_NY),
    ((Crystallised.YES, Voluntary, Voluntary, Voluntary), Intent.NY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.NY),
    ((Crystallised.YES, Voluntary, Voluntary, NoStatus), Intent.NY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.NY),

    ((Crystallised.NO, Voluntary, Mandated, Voluntary), Intent.PY, (OneYearOptOut.NO, MultiYearOptOut.YES), ToBeUpdated.PY_NY),
    ((Crystallised.NO, Voluntary, Mandated, NoStatus), Intent.PY, (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeUpdated.PY),
    ((Crystallised.NO, Voluntary, Mandated, Mandated), Intent.PY, (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeUpdated.PY),
    ((Crystallised.YES, Voluntary, Voluntary, Mandated), Intent.CY, (OneYearOptOut.YES, MultiYearOptOut.NO), ToBeUpdated.CY),

    ((Crystallised.YES, Voluntary, Mandated, Voluntary), Intent.NY, (OneYearOptOut.YES, OneYearOptOut.NO), ToBeUpdated.NY),

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
