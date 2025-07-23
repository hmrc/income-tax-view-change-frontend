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
import services.optout.{CurrentOptOutTaxYear, MultiYearOptOutDefault, NextOptOutTaxYear, NextYearOptOut, OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated, PreviousOptOutTaxYear}
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

    "messageSuffix" should {
      val cases = Seq(
        (previousOptOutTaxYear, Some(MultiYearOptOutDefault), "previousYear.multiYear"),
        (currentOptOutTaxYear, Some(MultiYearOptOutDefault), "currentYear.multiYear"),
        (nextOptOutTaxYear, Some(MultiYearOptOutDefault), "nextYear.multiYear"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), "previousYear.singleYearFollowedByMandated"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), "currentYear.singleYearFollowedByMandated"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), "nextYear.singleYearFollowedByMandated"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), "previousYear.singleYearFollowedByAnnual"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), "currentYear.singleYearFollowedByAnnual"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), "nextYear.singleYearFollowedByAnnual"),
        (previousOptOutTaxYear, Some(NextYearOptOut), "previousYear.nextYearOptOut"),
        (currentOptOutTaxYear, Some(NextYearOptOut), "currentYear.nextYearOptOut"),
        (nextOptOutTaxYear, Some(NextYearOptOut), "nextYear.nextYearOptOut"),
        (previousOptOutTaxYear, None, "previousYear.noState"),
        (currentOptOutTaxYear, None, "currentYear.noState"),
        (nextOptOutTaxYear, None, "nextYear.noState")
      )
      for ((taxYear, optOutState, expectedSuffix) <- cases) {
        s"return $expectedSuffix for ${taxYear.getClass.getSimpleName} with $optOutState" in {
          val model = OptOutTaxYearQuestionViewModel(taxYear, optOutState)
          model.messageSuffix shouldBe expectedSuffix
        }
      }
    }
  }
}
