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

package models.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, NoStatus, Voluntary}
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.TestSupport

class NextUpdatesOptOutViewModelSpec extends TestSupport {

  def buildOptOutProposition(cyStatus: ITSAStatus, pyStatus: ITSAStatus, pyCrystallised: Boolean): OptOutProposition = {
    val currentYear = TaxYear.forYearEnd(2024)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(pyStatus, previousYear, crystallised = pyCrystallised)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(cyStatus, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  "QuarterlyReportingContentChecks" when {
    "called showUpdateTypeDetailsSection" should {
      "return Some[Unit] indicating to show - update type detail section" in {
        Seq((Voluntary, Mandated, true),
          (Mandated, NoStatus, true),
          (Mandated, Mandated, false),
          (Voluntary, NoStatus, false),
          (NoStatus, Voluntary, false)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUpdateTypeDetailsSection shouldBe Some({})
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((NoStatus, NoStatus, true),
          (NoStatus, NoStatus, false),
          (NoStatus, Voluntary, true)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUpdateTypeDetailsSection shouldBe None
        }
      }
    }


    "called showUseCompatibleSoftwareSection" should {
      "return Some[Unit] indicating to show - update type detail section" in {
        Seq((Voluntary, Mandated, true),
          (Mandated, NoStatus, true),
          (Mandated, Mandated, false),
          (Voluntary, NoStatus, false),
          (NoStatus, Voluntary, false)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUseCompatibleSoftwareSection shouldBe Some({})
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((NoStatus, NoStatus, true),
          (NoStatus, NoStatus, false),
          (NoStatus, Voluntary, true)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUseCompatibleSoftwareSection shouldBe None
        }
      }


    }
  }
}
