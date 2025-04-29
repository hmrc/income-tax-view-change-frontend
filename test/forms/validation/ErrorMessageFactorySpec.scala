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

package forms.validation

import forms.validation.models.{FieldError, SummaryError}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ErrorMessageFactorySpec extends AnyWordSpec with Matchers {

  "ErrorMessageFactory" must {
    "mark form as Invalid" when {
      "when validation error" in {

        val result = List(Seq(FieldError("invalid form", Seq("Please enter a value")),
          SummaryError("invalid form", Seq("Please enter a value"))))

        ErrorMessageFactory.error("invalid form", "Please enter a value").errors.map(_.args) mustBe result
      }
    }
  }
}
