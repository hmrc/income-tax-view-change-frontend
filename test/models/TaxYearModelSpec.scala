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


  "TaxYear.getTaxYearStartYearEndYear" when {
    "given an input of letters with the correct length" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("ABCD-EFGH").isDefined shouldBe false
      }
    }
    "given an input of letters with the incorrect length" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("ABCDEFGH-IJKLMNO").isDefined shouldBe false
      }
    }
    "given an input of numbers with more than one dash" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("2020-2021-2022").isDefined shouldBe false
      }
    }
    "given an empty input" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("").isDefined shouldBe false
      }
    }
    "given an input with no dashes" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("20212023").isDefined shouldBe false
      }
    }
    "given an input with years in the incorrect format" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("21-22").isDefined shouldBe false
      }
    }
    "given an input with years which have length greater than 4" should {
      "not return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("20221-20222").isDefined shouldBe false
      }
    }
    "given an input with numerical years in the format YYYY-YYYY" should {
      "return a TaxYear model" in {
        TaxYear.getTaxYearStartYearEndYear("2021-2022").isDefined shouldBe true
      }
    }
  }
}
