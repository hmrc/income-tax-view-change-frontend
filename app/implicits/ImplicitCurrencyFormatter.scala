/*
 * Copyright 2022 HM Revenue & Customs
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

package implicits

import java.text.DecimalFormat

import play.twirl.api.Html

trait ImplicitCurrencyFormatter {
  implicit class CurrencyFormatter(x: BigDecimal) {
    val f = new DecimalFormat("#,##0.00")

    def toCurrency: Html = {
      if (x < 0) {
        Html("&minus;" + "&pound;" + f.format(x).replace("-", ""))
      } else {
        Html("&pound;" + f.format(x))
      }
    }

    def toCurrencyString: String = "£" + f.format(x)

    def toNegativeCurrency: Html = {
      if (x > 0) {
        Html("&minus;" + toCurrency)
      } else {
        toCurrency
      }
    }

    def toPence: Long = (x * 100).toLong
  }

  implicit class CurrencyFormatterInt(x: Int) {
    val f = new DecimalFormat("#,##0.00")

    def toCurrency: Html = Html("&pound;" + f.format(x))

    def toCurrencyString: String = "£" + f.format(x)

    def toNegativeCurrency: Html = {
      if (x > 0) {
        Html("&minus;" + toCurrency)
      } else {
        toCurrency
      }
    }

    def toPence: Long = (x * 100).toLong
  }
}

object ImplicitCurrencyFormatter extends ImplicitCurrencyFormatter
