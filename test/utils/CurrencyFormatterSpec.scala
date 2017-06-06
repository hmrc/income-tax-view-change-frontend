/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import play.twirl.api.Html

class CurrencyFormatterSpec extends TestSupport with ImplicitCurrencyFormatter {

  "The implicit currency formatter" should {

    s"format with leading pound sign (£)" in {
      val amount: BigDecimal = 12.55
      amount.toCurrency shouldBe Html("&pound;12.55")
    }

    s"format with leading pound sign (£) and commas in correct place" in {
      val amount: BigDecimal = 12123.55
      amount.toCurrency shouldBe Html("&pound;12,123.55")
    }

    s"format with leading pound sign (£) and commas in correct place with trailing zeros" in {
      val amount: BigDecimal = 134432
      amount.toCurrency shouldBe Html("&pound;134,432.00")
    }

    s"format large numbers with leading pound sign (£) and commas in correct place" in {
      val amount: BigDecimal = 555134432
      amount.toCurrency shouldBe Html("&pound;555,134,432.00")
    }
  }
}