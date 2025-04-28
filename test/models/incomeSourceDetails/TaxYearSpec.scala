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

package models.incomeSourceDetails

import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.libs.json._
import testUtils.{TestSupport, UnitSpec}

import java.time.LocalDate

class TaxYearSpec extends UnitSpec with TestSupport {

  "format" when {

    val startYear = 2022
    val endYear = 2023

    "reading a valid tax year" should {
      "return tax year from end year number" in {
        val validTaxYear = JsNumber(endYear)
        validTaxYear.validate[TaxYear] shouldBe JsSuccess(TaxYear(startYear, endYear))
      }
    }

    "reading an invalid tax year" should {
      "return a validation error for non-integer numbers" in {
        val taxYearWithDecimalPlaces = JsNumber(2023.21)
        taxYearWithDecimalPlaces.validate[TaxYear] shouldBe JsError("error.expected.int")
      }

      "return a validation error for non numeric types" in {
        val taxYearAsString = JsString(s"$endYear")
        taxYearAsString.validate[TaxYear] shouldBe JsError("error.expected.jsnumber")
      }

      "return a validation error integers that fail tax year validation" in {
        val taxYearAsString = JsNumber(20)
        taxYearAsString.validate[TaxYear] shouldBe JsError("Could not parse tax year")
      }

    }

    "writing a valid tax year" should {
      "return a JsNumber with the end year" in {
        val taxYear = TaxYear(startYear, endYear)
        Json.toJson(taxYear) shouldBe JsNumber(endYear)
      }
    }
  }

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
      "return a string with the tax year as short year range" in {
        val taxYear: TaxYear = TaxYear(2098, 2099)
        val taxYearRange: String = taxYear.formatAsShortYearRange

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

  "makeTaxYearWithEndYear" should {
    "create a TaxYear object" when {
      "given the Tax Year end year" in {
        val result = makeTaxYearWithEndYear(2024)

        result.startYear shouldBe 2023
        result shouldBe TaxYear(2023, 2024)
      }
    }
  }

  "isCurrentOrFutureTaxYear method" should {
    "return true" when {
      "the TaxYear object is the current tax year" in {
        val taxYear: TaxYear = TaxYear(2023, 2024)
        val result = taxYear.isFutureTaxYear

        result shouldBe true
      }
      "the TaxYear object is in a future tax year" in {
        val taxYear: TaxYear = TaxYear(2024, 2025)
        val result = taxYear.isFutureTaxYear

        result shouldBe true
      }
    }
    "return false" when {
      "the TaxYear object is in a past tax year" in {
        val taxYear: TaxYear = TaxYear(2022, 2023)
        val result = taxYear.isFutureTaxYear

        result shouldBe false
      }
    }
  }

  "isSameAs method" should {
    "return true" when {
      "the TaxYear object isSameAs given tax year" in {
        TaxYear.forYearEnd(2024).isSameAs(TaxYear.forYearEnd(2024)) shouldBe true
      }
    }
    "return false" when {
      "the TaxYear object is not same as given tax year" in {
        TaxYear.forYearEnd(2024).isSameAs(TaxYear.forYearEnd(2023)) shouldBe false
      }
    }
  }

  "isBefore method" should {
    "return true" when {
      "the TaxYear object isSameAs given tax year" in {
        TaxYear.forYearEnd(2023).isBefore(TaxYear.forYearEnd(2024)) shouldBe true
      }
    }
    "return false" when {
      "the TaxYear object is not before given tax year" in {
        TaxYear.forYearEnd(2024).isBefore(TaxYear.forYearEnd(2023)) shouldBe false
      }
    }
  }

  "isAfter method" should {
    "return true" when {
      "the TaxYear object isAfter given tax year" in {
        TaxYear.forYearEnd(2024).isAfter(TaxYear.forYearEnd(2023)) shouldBe true
      }
    }
    "return false" when {
      "the TaxYear object is not after given tax year" in {
        TaxYear.forYearEnd(2024).isAfter(TaxYear.forYearEnd(2024)) shouldBe false
      }
    }
  }

  "toFinancialYearStart method" should {
    "return start of financial year" in {
      val taxYear = TaxYear.forYearEnd(2024)
      taxYear.toFinancialYearStart shouldBe LocalDate.of(taxYear.startYear, 4, 6)
    }
  }

  "toFinancialYearEnd method" should {
    "return end of financial year" in {
      val taxYear = TaxYear.forYearEnd(2024)
      taxYear.toFinancialYearEnd shouldBe LocalDate.of(taxYear.endYear, 4, 5)
    }
  }

  ".shortenTaxYearEnd()" should {

      "match required format" in {

        TaxYear.forYearEnd(2024).shortenTaxYearEnd shouldBe "2023-24"
      }
    }
}
