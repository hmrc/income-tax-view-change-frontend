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

package models.reportingObligations.optOut

import models.reportingObligations.optOut.NextUpdatesQuarterlyReportingContentChecks
import testUtils.TestSupport

class NextUpdatesQuarterlyReportingContentChecksSpec extends TestSupport {

  "QuarterlyReportingContentChecks" when {
    "called showUpdateTypeDetailsSection" should {
      "return Some[Unit] indicating to show - update type detail section" in {
        Seq((true, true, true),
          (true, false, true),
          (true, true, false),
          (true, false, false),
          (false, true, false)).foreach {

          case (currentYearMandatoryOrVoluntary, previousYearMandatoryOrVoluntary, previousYearCrystallised) =>
            val viewModel = NextUpdatesQuarterlyReportingContentChecks(
              currentYearItsaStatus = currentYearMandatoryOrVoluntary,
              previousYearItsaStatus = previousYearMandatoryOrVoluntary,
              previousYearCrystallisedStatus = previousYearCrystallised)

            viewModel.showUpdateTypeDetailsSection shouldBe Some({})
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((false, false, true),
          (false, false, false),
          (false, true, true)).foreach {

          case (currentYearMandatoryOrVoluntary, previousYearMandatoryOrVoluntary, previousYearCrystallised) =>

            val viewModel = NextUpdatesQuarterlyReportingContentChecks(
              currentYearItsaStatus = currentYearMandatoryOrVoluntary,
              previousYearItsaStatus = previousYearMandatoryOrVoluntary,
              previousYearCrystallisedStatus = previousYearCrystallised)

            viewModel.showUpdateTypeDetailsSection shouldBe None
        }
      }
    }


    "called showUseCompatibleSoftwareSection" should {
      "return Some[Unit] indicating to show - update type detail section" in {
        Seq((true, true, true),
          (true, false, true),
          (true, true, false),
          (true, false, false),
          (false, true, false)).foreach {

          case (currentYearMandatoryOrVoluntary, previousYearMandatoryOrVoluntary, previousYearCrystallised) =>
            val viewModel = NextUpdatesQuarterlyReportingContentChecks(
              currentYearItsaStatus = currentYearMandatoryOrVoluntary,
              previousYearItsaStatus = previousYearMandatoryOrVoluntary,
              previousYearCrystallisedStatus = previousYearCrystallised)

            viewModel.showUpdateTypeDetailsSection shouldBe Some({})
        }
      }

      "return None indicating absence of - update type detail section" in {
        Seq((false, false, true),
          (false, false, false),
          (false, true, true)).foreach {

          case (currentYearMandatoryOrVoluntary, previousYearMandatoryOrVoluntary, previousYearCrystallised) =>

            val viewModel = NextUpdatesQuarterlyReportingContentChecks(
              currentYearItsaStatus = currentYearMandatoryOrVoluntary,
              previousYearItsaStatus = previousYearMandatoryOrVoluntary,
              previousYearCrystallisedStatus = previousYearCrystallised)

            viewModel.showUpdateTypeDetailsSection shouldBe None
        }
      }
    }


  }
}
