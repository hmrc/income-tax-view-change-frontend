/*
 * Copyright 2021 HM Revenue & Customs
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

import implicits.ImplicitDateFormatter
import models.core.AccountingPeriodModel
import models.core.AccountingPeriodModel.determineTaxYearFromPeriodEnd
import org.scalatest.Matchers
import testUtils.TestSupport

class AccountingPeriodModelSpec extends TestSupport with Matchers with ImplicitDateFormatter {

  "The AccountingPeriodModel Model" when {
    "the end date is before the Start of the next Tax Year" should {
      "return the current Tax Year" in {
        AccountingPeriodModel("2017-04-06", "2018-04-05").determineTaxYear shouldBe 2018
        determineTaxYearFromPeriodEnd("2018-04-05") shouldBe 2018
      }
    }
    "the end date is on the Start of the next Tax Year" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-04-07", "2018-04-06").determineTaxYear shouldBe 2019
        determineTaxYearFromPeriodEnd("2018-04-06") shouldBe 2019
      }
    }
    "the end date is after the Start of the next Tax Year" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-04-08", "2018-04-07").determineTaxYear shouldBe 2019
        determineTaxYearFromPeriodEnd("2018-04-07") shouldBe 2019
      }
    }
    "the end date is 6th of March" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-03-05", "2018-03-06").determineTaxYear shouldBe 2018
        determineTaxYearFromPeriodEnd("2018-03-06") shouldBe 2018
      }
    }
    "the end date is 1st of June" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-03-05", "2018-06-01").determineTaxYear shouldBe 2019
        determineTaxYearFromPeriodEnd("2018-06-01") shouldBe 2019
      }
    }
  }
}
