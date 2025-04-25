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
import services.optout.OptOutTestSupport._
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec


class OptOutPropositionYearsToUpdateSpec extends UnitSpec {

  val testCases = List(

    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), previousTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(previousTaxYear, currentTaxYear, nextTaxYear)),
    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), currentTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(currentTaxYear, nextTaxYear)),
    ((Crystallised.NO, Voluntary, Voluntary, Voluntary), nextTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(nextTaxYear)),

    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), previousTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(previousTaxYear, currentTaxYear)),
    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), currentTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(currentTaxYear)),
    ((Crystallised.NO, Voluntary, Voluntary, NoStatus), nextTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(nextTaxYear)),

    ((Crystallised.YES, Voluntary, Voluntary, Voluntary), currentTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(currentTaxYear, nextTaxYear)),
    ((Crystallised.YES, Voluntary, Voluntary, Voluntary), nextTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(nextTaxYear)),
    ((Crystallised.YES, Voluntary, Voluntary, NoStatus), nextTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(nextTaxYear)),

    ((Crystallised.NO, Voluntary, Mandated, Voluntary), previousTaxYear, (OneYearOptOut.NO, MultiYearOptOut.YES), Seq(previousTaxYear, nextTaxYear)),
    ((Crystallised.NO, Voluntary, Mandated, NoStatus), previousTaxYear, (OneYearOptOut.YES, MultiYearOptOut.NO), Seq(previousTaxYear)),
    ((Crystallised.NO, Voluntary, Mandated, Mandated), previousTaxYear, (OneYearOptOut.YES, MultiYearOptOut.NO), Seq(previousTaxYear)),
    ((Crystallised.YES, Voluntary, Voluntary, Mandated), currentTaxYear, (OneYearOptOut.YES, MultiYearOptOut.NO), Seq(currentTaxYear)),

    ((Crystallised.YES, Voluntary, Mandated, Voluntary), nextTaxYear, (OneYearOptOut.YES, OneYearOptOut.NO), Seq(nextTaxYear)),

  )

  testCases.foreach {
    case (input, intent, numberOfYearsFlags, output) =>
      val test = optOutPropositionUpdatesTest _
      test.tupled(input)(intent).tupled(numberOfYearsFlags)(output)
  }

  def optOutPropositionUpdatesTest(crystallised: Boolean, pyStatus: ITSAStatus, cyStatus: ITSAStatus, nyStatus: ITSAStatus)
                                  (intent: TaxYear)
                                  (isOneYearOptOut: Boolean, isMultiYearOptOut: Boolean)
                                  (expectedToUpdate: Seq[TaxYear]): Unit = {

    s"update years ${expectedToUpdate.mkString(",")}" when {
      s"proposition is ${(crystallised, pyStatus, cyStatus, nyStatus)}" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(pyStatus, previousTaxYear, crystallised = crystallised)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(cyStatus, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(nyStatus, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe isOneYearOptOut
        optOutData.isMultiYearOptOut shouldBe isMultiYearOptOut
        optOutData.isNoOptOutAvailable shouldBe !isOneYearOptOut && !isMultiYearOptOut

        optOutData.optOutYearsToUpdate(intent) shouldBe expectedToUpdate

      }
    }
  }

}
