/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.constraints

import forms.validation.Constraints
import play.api.data.validation.{Invalid, Valid}
import testUtils.TestSupport

class ConstraintsSpec extends Constraints with TestSupport {

  val maxLength = 2
  val errMsg = "Too Long"

  "The Constraints.optMaxLength method" when {

    "supplied with a some string which exceeds the max length" should {
      "return invalid with the correct message" in {
        optMaxLength(maxLength, Invalid(errMsg))(Some("abc")) shouldBe Invalid(errMsg)
      }
    }

    "supplied with a some string which equals the max length" should {
      "return valid" in {
        optMaxLength(maxLength, Invalid(errMsg))(Some("ab")) shouldBe Valid
      }
    }

    "supplied with a some string which is less than the max length" should {
      "return valid" in {
        optMaxLength(maxLength, Invalid(errMsg))(Some("a")) shouldBe Valid
      }
    }

    "supplied with no value" should {
      "return valid" in {
        optMaxLength(maxLength, Invalid(errMsg))(None) shouldBe Valid
      }
    }
  }
}