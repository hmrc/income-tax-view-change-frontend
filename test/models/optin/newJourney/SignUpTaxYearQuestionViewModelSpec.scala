/*
 * Copyright 2025 HM Revenue & Customs
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

package models.optin.newJourney

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear}
import testUtils.UnitSpec

class SignUpTaxYearQuestionViewModelSpec extends UnitSpec {

  val currentOptInTaxYear = CurrentOptInTaxYear(ITSAStatus.Annual, TaxYear(2025, 2026))
  val nextOptInTaxYear = NextOptInTaxYear(ITSAStatus.Annual, TaxYear(2026, 2027), currentOptInTaxYear)
  "SignUpTaxYearQuestionViewModel" should {
    "indicate signing up for the current tax year" when {
      "the signUpTaxYear is an instance of CurrentOptInTaxYear" in {
        val viewModel = SignUpTaxYearQuestionViewModel(currentOptInTaxYear)
        viewModel.signingUpForCY shouldBe true
      }
    }

    "indicate signing up for the next tax year" when {
      "the signUpTaxYear is an instance of NextOptInTaxYear" in {
        val viewModel = SignUpTaxYearQuestionViewModel(nextOptInTaxYear)
        viewModel.signingUpForCY shouldBe false
      }
    }
  }
}
