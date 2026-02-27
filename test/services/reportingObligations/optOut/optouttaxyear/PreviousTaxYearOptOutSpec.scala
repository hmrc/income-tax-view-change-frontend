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
import services.reportingObligations.optOut.PreviousOptOutTaxYear
import testUtils.UnitSpec

class PreviousTaxYearOptOutSpec extends UnitSpec {

  val previousTaxYear = TaxYear.forYearEnd(2024).previousYear

  "PreviousTaxYearOptOut" should {
    "offer opt-out" when {
      "previous year is not crystallised" when {
        s"previous year is ${ITSAStatus.Voluntary}" in {
          PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false).canOptOut shouldBe true
        }
      }
    }

    "not offer opt-out" when {
      "previous year is crystallised" when {
        s"previous year is ${ITSAStatus.Voluntary}" in {
          PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Annual}" in {
          PreviousOptOutTaxYear(ITSAStatus.Annual, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Mandated}" in {
          PreviousOptOutTaxYear(ITSAStatus.Mandated, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.NoStatus}" in {
          PreviousOptOutTaxYear(ITSAStatus.NoStatus, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }
      }

      "previous year is not crystallised" when {
        s"previous year is ${ITSAStatus.Mandated}" in {
          PreviousOptOutTaxYear(ITSAStatus.Mandated, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Annual}" in {
          PreviousOptOutTaxYear(ITSAStatus.Annual, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.NoStatus}" in {
          PreviousOptOutTaxYear(ITSAStatus.NoStatus, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }
      }
    }
  }
}
