/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import models.incomeSourceDetails.TaxYear
import testUtils.UnitSpec

class TaxYearModelSpec extends UnitSpec {

  "currentTaxYearMinusOne method" when {
    "invoked on a TaxYear object" should {
      "return a new TaxYear object where the years are one year less than before" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearMinusOne = taxYear.addYears(-1)

        val desiredTaxYearObject: TaxYear = TaxYear(2097, 2098)

        taxYearMinusOne shouldBe desiredTaxYearObject
      }
      "return a new TaxYear object where the years are 100 years less than before" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearMinusOne = taxYear.addYears(-100)

        val desiredTaxYearObject: TaxYear = TaxYear(1998, 1999)

        taxYearMinusOne shouldBe desiredTaxYearObject
      }
    }
  }

  "currentTaxYearPlusOne method" when {
    "invoked on a TaxYear object" should {
      "return a new TaxYear object where the years are one year more than before" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearMinusOne = taxYear.addYears(1)

        val desiredTaxYearObject: TaxYear = TaxYear(2099, 2100)

        taxYearMinusOne shouldBe desiredTaxYearObject
      }
      "return a new TaxYear object where the years are one hundred years more than before" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearMinusOne = taxYear.addYears(100)

        val desiredTaxYearObject: TaxYear = TaxYear(2198, 2199)

        taxYearMinusOne shouldBe desiredTaxYearObject
      }
    }
  }

  "formatTaxYearRange method" when {
    "invoked on a TaxYear object" should {
      "return a string with the tax year range" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearRange: String = taxYear.formatTaxYearRange

        val desiredTaxYearRangeString: String = "98-99"

        taxYearRange shouldBe desiredTaxYearRangeString
      }
    }
  }

  "TaxYear.getTaxYearStartYearEndYear" when {
    "given an input of letters with the correct length" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("ABCD-EFGH").isDefined shouldBe false
      }
    }
    "given an input of letters with the incorrect length" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("ABCDEFGH-IJKLMNO").isDefined shouldBe false
      }
    }
    "given an input of numbers with more than one dash" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("2020-2021-2022").isDefined shouldBe false
      }
    }
    "given an empty input" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("").isDefined shouldBe false
      }
    }
    "given an input with no dashes" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("20212023").isDefined shouldBe false
      }
    }
    "given an input with years in the incorrect format" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("21-22").isDefined shouldBe false
      }
    }
    "given an input with years which have length greater than 4" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("20221-20222").isDefined shouldBe false
      }
    }
    "given an input with numerical years in the format YYYY-YYYY with a numerical difference of 2" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("2020-2022").isDefined shouldBe false
      }
    }
    "given an input where yearOne is greater than yearTwo" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearModel("2022-2021").isDefined shouldBe false
      }
    }
    "given an input with numerical years in the format YYYY-YYYY with a numerical difference of 1" should {
      "return a TaxYear model" in {
        TaxYear.getTaxYearModel("2021-2022").isDefined shouldBe true
      }
    }
  }
}
