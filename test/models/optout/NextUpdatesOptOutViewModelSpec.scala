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

import models.itsaStatus.ITSAStatus.{Mandated, NoStatus, Voluntary}
import testConstants.OptOutTestConstants.buildOptOutProposition
import testUtils.TestSupport

class NextUpdatesOptOutViewModelSpec extends TestSupport {

  "QuarterlyReportingContentChecks" when {
    "called showUpdateTypeDetailsSection" should {
      "return Some[Unit] indicating to show - update type detail section" in {
        Seq((Voluntary, Mandated, true),
          (Mandated, NoStatus, true),
          (Mandated, Mandated, false),
          (Voluntary, NoStatus, false),
          (NoStatus, Voluntary, false)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, Voluntary, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUpdateTypeDetailsSection shouldBe true
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((NoStatus, NoStatus, true),
          (NoStatus, NoStatus, false),
          (NoStatus, Voluntary, true)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, Voluntary, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUpdateTypeDetailsSection shouldBe false
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

            val proposition = buildOptOutProposition(cyStatus, pyStatus, Voluntary, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUseCompatibleSoftwareSection shouldBe true
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((NoStatus, NoStatus, true),
          (NoStatus, NoStatus, false),
          (NoStatus, Voluntary, true)).foreach {

          case (cyStatus, pyStatus, pyCrystallised) =>

            val proposition = buildOptOutProposition(cyStatus, pyStatus, Voluntary, pyCrystallised)

            val viewModel = NextUpdatesOptOutViewModel(proposition, None)

            viewModel.showUseCompatibleSoftwareSection shouldBe false
        }
      }


    }
  }
}
