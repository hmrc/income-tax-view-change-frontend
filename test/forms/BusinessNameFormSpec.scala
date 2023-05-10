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

package forms

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}

class BusinessNameFormSpec extends AnyWordSpec with Matchers {

  def form(value: String): Form[BusinessNameForm] = BusinessNameForm.form.bind(Map("name" -> value))

  "BusinessNameForm" must {

    "return a valid form" when {
      "valid business name entered" in {
        val result = form("Test Business").value
        result mustBe Some(BusinessNameForm("Test Business"))
      }
    }

    "return an error" when {
      "the business name is empty" in {
        val result = form("").errors
        result mustBe Seq(FormError("name", BusinessNameForm.businessNameEmptyError))
      }

      "the business name is too long" in {
        val result = form("Lorem ipsum dolor sit amet consectetur adipiscing elit " +
          "Phasellus vel ante ut tellus interdum fermentum Suspendisse potenti").errors
        result mustBe Seq(FormError("name", BusinessNameForm.businessNameLengthIncorrect))
      }

      "the business name contains invalid characters" in {
        val result = form("Test Business *").errors
        result mustBe Seq(FormError("name", BusinessNameForm.businessNameInvalidChar))
      }
    }
  }

}
