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

package services.reportingObligations.optOut.optouttaxyear

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.reportingObligations.optOut.{CurrentOptOutTaxYear, NextOptOutTaxYear}
import testUtils.UnitSpec

class NextTaxYearOptOutSpec extends UnitSpec {

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(2024)
  val nextTaxYear: TaxYear = currentTaxYear.nextYear

  val anyItsaStatusCurrentTaxYearOptOut: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)

  "NextTaxYearOptOut" should {
    "offer opt-out" when {
      s"next year is ${ITSAStatus.Voluntary}" when {
        "with any ITSA status for current year" in {
          NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, anyItsaStatusCurrentTaxYearOptOut).canOptOut shouldBe true
        }
      }

      s"next year is ${ITSAStatus.NoStatus}" when {
        s"with current year ${ITSAStatus.Voluntary}" in {
          val currentYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
          NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentYear).canOptOut shouldBe true
        }
      }
    }

    "not offer opt-out" when {

      s"current year status any ITSAStatus" when {

        s"next year is ${ITSAStatus.Mandated}" in {
          val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
          NextOptOutTaxYear(ITSAStatus.Mandated, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }

        s"next year is ${ITSAStatus.Annual}" in {
          val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
          NextOptOutTaxYear(ITSAStatus.Annual, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }
      }

      s"current year status ${ITSAStatus.NoStatus}" when {
        s"next year is ${ITSAStatus.NoStatus}" in {
          val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.NoStatus, currentTaxYear)
          NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }
      }

    }
  }

}
