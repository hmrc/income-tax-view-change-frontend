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

package forms.manageBusinesses.add

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}

class BusinessTradeFormSpec extends AnyWordSpec with Matchers {

  def form(value: String): Form[BusinessTradeForm] = BusinessTradeForm.form.bind(Map(BusinessTradeForm.businessTrade -> value))

  "BusinessTradeForm" must {

    "return a valid form" when {
      "valid business trade entered" in {
        val result = form("Test Trade").value
        result mustBe Some(BusinessTradeForm("Test Trade"))
      }
    }

    "return an error" when {
      "the business trade is empty" in {
        val result = form("").errors
        result mustBe Seq(FormError(BusinessTradeForm.businessTrade, BusinessTradeForm.tradeEmptyError))
      }

      "the business trade is too short" in {
        val result = form("A").errors
        result mustBe Seq(FormError(BusinessTradeForm.businessTrade, BusinessTradeForm.tradeShortError, Seq(BusinessTradeForm.MIN_LENGTH)))
      }

      "the business trade is too long" in {

        val overMaxLength: String = (1 to BusinessTradeForm.MAX_LENGTH + 1).map(_ => "a").mkString
        val result = form(overMaxLength).errors

        result mustBe Seq(FormError(BusinessTradeForm.businessTrade, BusinessTradeForm.tradeLongError, Seq(BusinessTradeForm.MAX_LENGTH)))
      }

      "the business trade contains invalid characters" in {
        val result = form("Test Business *").errors
        result mustBe Seq(FormError(BusinessTradeForm.businessTrade, BusinessTradeForm.tradeInvalidCharError, Seq(BusinessTradeForm.permittedChars)))
      }
    }
  }

}
