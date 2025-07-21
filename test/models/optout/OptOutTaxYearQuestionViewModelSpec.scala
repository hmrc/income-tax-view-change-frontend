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

package models.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, PreviousOptOutTaxYear}
import testUtils.UnitSpec

class OptOutTaxYearQuestionViewModelSpec extends UnitSpec {

  val previousOptOutTaxYear = PreviousOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026), false)
  val currentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026))
  val nextOptOutTaxYear = NextOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2026, 2027), currentOptOutTaxYear)

  "OptOutTaxYearQuestionViewModel" when {
    ".isCurrentYear" should {
      "return true for CurrentOptOutTaxYear" in {
        val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, None)
        model.isCurrentYear shouldBe true
      }

      "return false for PreviousOptOutTaxYear" in {
        val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, None)
        model.isCurrentYear shouldBe false
      }

      "return false for NextOptOutTaxYear" in {
        val model = OptOutTaxYearQuestionViewModel(nextOptOutTaxYear, None)
        model.isCurrentYear shouldBe false
      }
    }
  }
}
