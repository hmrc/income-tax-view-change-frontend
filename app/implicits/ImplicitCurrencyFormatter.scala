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

package implicits

import java.text.DecimalFormat

import play.twirl.api.Html

trait ImplicitCurrencyFormatter {
  implicit class CurrencyFormatter(x: BigDecimal) {
    val f = new DecimalFormat("#,##0.00")
    def toCurrency: Html = Html("&pound;" + f.format(x).replace(".00",""))
    def toCurrencyString: String = "£" + f.format(x).replace(".00","")
    def toNegativeCurrencyString: String = {
      if(x > 0) {
        "-" + toCurrencyString
      } else {
        toCurrencyString
      }
    }
    def toStringNoDecimal: String = x.toString.replace(".0","")
    def toPence: Long = (x * 100).toLong
  }
}

object ImplicitCurrencyFormatter extends ImplicitCurrencyFormatter
