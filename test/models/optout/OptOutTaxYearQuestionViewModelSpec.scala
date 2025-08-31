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
import models.itsaStatus.ITSAStatus.{Annual, Mandated, NoStatus, Voluntary}
import services.optout.{CurrentOptOutTaxYear, MultiYearOptOutDefault, NextOptOutTaxYear, NextYearOptOut, OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated, PreviousOptOutTaxYear}
import testUtils.UnitSpec

class OptOutTaxYearQuestionViewModelSpec extends UnitSpec {

  val previousOptOutTaxYear = PreviousOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026), false)
  val currentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2025, 2026))
  val nextOptOutTaxYear = NextOptOutTaxYear(ITSAStatus.Voluntary, TaxYear(2026, 2027), currentOptOutTaxYear)

  "OptOutTaxYearQuestionViewModel" when {

    ".showInset" should {
      "return true" when {
        "the optOutState is MultiYearOptOutDefault and the tax year is current" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary)
          model.showInset shouldBe true
        }
        "the optOutState is MultiYearOptOutDefault and the tax year is previous" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary)
          model.showInset shouldBe true
        }
        "the optOutState is OneYearOptOutFollowedByMandated and the tax year isn't previous" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 0, Voluntary)
          model.showInset shouldBe true
        }
      }
      "return false" when {
        "the optOutState is None" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, None, 0, NoStatus)
          model.showInset shouldBe false
        }
      }
    }

    ".showQuarterlyUpdatesInset" should {
      "return true" when {
        "the optOutState is OneYearOptOutFollowedByAnnual and there are updates submitted" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 1, Voluntary)
          model.showQuarterlyUpdatesInset shouldBe true
        }
        "the optOutState is OneYearOptOutFollowedByMandated and the tax year is previous" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showQuarterlyUpdatesInset shouldBe true
        }
        "the optOutState is OneYearOptOutFollowedByAnnual and the tax year is previous" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showQuarterlyUpdatesInset shouldBe true
        }
      }
      "return false" when {
        "the optOutState is None" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, None, 0, NoStatus)
          model.showQuarterlyUpdatesInset shouldBe false
        }
        "the optOutState is OneYearOptOutFollowedByAnnual and there are no updates submitted" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showQuarterlyUpdatesInset shouldBe false
        }
      }
    }

    ".showSecondParagraph" should {
      "return true" when {
        "the optOutState is MultiYearOptOutDefault" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary)
          model.showSecondParagraph shouldBe true
        }
        "the optOutState is OneYearOptOutFollowedByAnnual" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showSecondParagraph shouldBe true
        }
        "the optOutState is NextYearOptOut and the current year status is Mandated" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(NextYearOptOut), 0, Mandated)
          model.showSecondParagraph shouldBe true
        }
        "the optOutState is OneYearOptOutFollowedByMandated and is the previous tax year" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showSecondParagraph shouldBe true
        }
      }
      "return false" when {
        "the optOutState is None" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, None, 0, NoStatus)
          model.showSecondParagraph shouldBe false
        }
        "the optOutState is OneYearOptOutFollowedByMandated and is the current tax year" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary)
          model.showSecondParagraph shouldBe true
        }
        "the optOutState is NextYearOptOut and the current year status is Voluntary" in {
          val model = OptOutTaxYearQuestionViewModel(currentOptOutTaxYear, Some(NextYearOptOut), 0, Voluntary)
          model.showSecondParagraph shouldBe false
        }
      }
    }

    ".showThirdParagraph" should {
      "return true" when {
        "the optOutState is MultiYearOptOutDefault" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary)
          model.showThirdParagraph shouldBe true
        }
      }
      "return false" when {
        "the optOutState is None" in {
          val model = OptOutTaxYearQuestionViewModel(previousOptOutTaxYear, None, 0, NoStatus)
          model.showThirdParagraph shouldBe false
        }
      }
    }

    "messageSuffix" should {
      val cases = Seq(
        (previousOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary, "previousYear.multiYear"),
        (currentOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary, "currentYear.multiYear"),
        (nextOptOutTaxYear, Some(MultiYearOptOutDefault), 0, Voluntary, "nextYear.multiYear"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 0, Voluntary, "previousYear.singleYearFollowedByMandated"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 0, Voluntary, "currentYear.singleYearFollowedByMandated"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 0, Voluntary, "nextYear.singleYearFollowedByMandated"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 1, Voluntary, "previousYear.singleYearFollowedByMandated"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 1, Voluntary, "currentYear.singleYearFollowedByMandatedWithUpdates"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByMandated), 1, Voluntary, "nextYear.singleYearFollowedByMandatedWithUpdates"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary, "previousYear.singleYearFollowedByAnnual"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary, "currentYear.singleYearFollowedByAnnual"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 0, Voluntary, "nextYear.singleYearFollowedByAnnual"),
        (previousOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 1, Voluntary, "previousYear.singleYearFollowedByAnnual"),
        (currentOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 1, Voluntary, "currentYear.singleYearFollowedByAnnualWithUpdates"),
        (nextOptOutTaxYear, Some(OneYearOptOutFollowedByAnnual), 1, Voluntary, "nextYear.singleYearFollowedByAnnualWithUpdates"),
        (previousOptOutTaxYear, Some(NextYearOptOut), 0, Mandated, "previousYear.nextYearOptOutMandated"),
        (currentOptOutTaxYear, Some(NextYearOptOut), 0, Mandated, "currentYear.nextYearOptOutMandated"),
        (nextOptOutTaxYear, Some(NextYearOptOut), 0, Mandated, "nextYear.nextYearOptOutMandated"),
        (previousOptOutTaxYear, Some(NextYearOptOut), 0, Annual, "previousYear.nextYearOptOutAnnual"),
        (currentOptOutTaxYear, Some(NextYearOptOut), 0, Annual, "currentYear.nextYearOptOutAnnual"),
        (nextOptOutTaxYear, Some(NextYearOptOut), 0, Annual, "nextYear.nextYearOptOutAnnual"),
        (previousOptOutTaxYear, None, 0, NoStatus, "previousYear.noState"),
        (currentOptOutTaxYear, None, 0, NoStatus, "currentYear.noState"),
        (nextOptOutTaxYear, None, 0, NoStatus, "nextYear.noState")
      )
      for ((taxYear, optOutState, amountOfQuarterlyUpdates, cyItsaStatus, expectedSuffix) <- cases) {
        s"return $expectedSuffix for ${taxYear.getClass.getSimpleName} with $optOutState with $amountOfQuarterlyUpdates quarterly updates and a CY ITSA status of $cyItsaStatus" in {
          val model = OptOutTaxYearQuestionViewModel(taxYear, optOutState, amountOfQuarterlyUpdates, cyItsaStatus)
          model.messageSuffix shouldBe expectedSuffix
        }
      }
    }
  }
}
