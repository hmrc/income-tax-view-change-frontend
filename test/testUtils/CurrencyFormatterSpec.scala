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

package testUtils

import implicits.ImplicitCurrencyFormatter
import play.twirl.api.Html

class CurrencyFormatterSpec extends TestSupport with ImplicitCurrencyFormatter {

  "The implicit currency formatter" when {

    "toNegativeCurrency" should {
      "return a value as negative" when {
        "the number is more than 0" in {
          val amount: BigDecimal = 0.01
          amount.toNegativeCurrency shouldBe Html("&minus;&pound;0.01")
        }
        "the number is 0" in {
          val amount: BigDecimal = 0.00
          amount.toNegativeCurrency shouldBe Html("&pound;0.00")
        }
      }
    }

    "given the value is 12.55" should {

      val amount: BigDecimal = 12.55

      s"format with leading pound sign (£)" in {
        amount.toCurrency shouldBe Html("&pound;12.55")
        amount.toCurrencyString shouldBe "£12.55"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 1255
      }
    }

    "given the value is 12123.55" should {

      val amount: BigDecimal = 12123.55

      s"format with leading pound sign (£) and commas in correct place" in {
        amount.toCurrency shouldBe Html("&pound;12,123.55")
        amount.toCurrencyString shouldBe "£12,123.55"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 1212355
      }
    }

    "given the value is 134432" should {

      val amount: BigDecimal = 134432

      s"format with leading pound sign (£) and commas in correct place with trailing zeros" in {
        amount.toCurrency shouldBe Html("&pound;134,432.00")
        amount.toCurrencyString shouldBe "£134,432.00"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 13443200
      }
    }

    "given the value is 555134432" should {

      val amount: BigDecimal = 555134432

      s"format large numbers with leading pound sign (£) and commas in correct place" in {
        amount.toCurrency shouldBe Html("&pound;555,134,432.00")
        amount.toCurrencyString shouldBe "£555,134,432.00"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 55513443200L
      }
    }

    "given the value is 12.00" should {

      val amount: BigDecimal = 12.00

      s"format numbers with trailing zeros without the zeros" in {
        amount.toCurrency shouldBe Html("&pound;12.00")
        amount.toCurrencyString shouldBe "£12.00"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 1200
      }
    }

    "given the value is 12.4" should {

      val amount: BigDecimal = 12.4

      s"format numbers with whole pence to include the trailing 0" in {
        amount.toCurrency shouldBe Html("&pound;12.40")
        amount.toCurrencyString shouldBe "£12.40"
      }
      "return the same value in pence" in {
        amount.toPence shouldBe 1240
      }
    }
  }
}
