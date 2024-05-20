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

package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import testUtils.UnitSpec

class OptOutPropositionYearsToOfferSpec extends UnitSpec {

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  "OptOutDataYearsToUpdate" should {

    "offer years PY, CY, NY" when {
      "each year is V and PY is Not Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe false
        optOutData.isMultiYearOptOut shouldBe true
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }

    "offer years CY, NY" when {
      "each year is V and PY is Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe false
        optOutData.isMultiYearOptOut shouldBe true
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }

    "offer years CY, NY" when {
      "CY is V, and NY is NoStatus and PY is Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe false
        optOutData.isMultiYearOptOut shouldBe true
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }

    "offer years NY" when {
      "only NY is V and PY is Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe true
        optOutData.isMultiYearOptOut shouldBe false
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(nextTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }

    "offer years PY, CY" when {
      "only NY is V and PY is Not Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Mandated, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe false
        optOutData.isMultiYearOptOut shouldBe true
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }

    "offer years PY, NY" when {
      "only NY is V and PY is Not Crystallised" in {

        val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
        val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
        val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

        val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

        optOutData.isOneYearOptOut shouldBe false
        optOutData.isMultiYearOptOut shouldBe true
        optOutData.isNoOptOutAvailable shouldBe false

        val expectedOffered = Seq(previousTaxYearOptOut, nextTaxYearOptOut)

        optOutData.availableOptOutYears shouldBe expectedOffered
      }
    }
  }
}
