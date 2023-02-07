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

package implicits

import implicits.ImplicitJsonValidationFormatter._
import org.scalatest.Matchers
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import testUtils.UnitSpec


class ImplicitJsonValidationFormatterSpec extends UnitSpec with Matchers {

  val jSPath: JsPath = JsPath \ "someNode"
  val someValidationError1: String = "someValidationError1"
  val someValidationError2: String = "someValidationError2"
  val someArg1: String = "someArg1"
  val someArg2: String = "someArg2"

  val testValidationErrors: Seq[(JsPath, Seq[ValidationError])] = Seq((jSPath, Seq(ValidationError(Seq(someValidationError1, someValidationError2), Seq(someArg1, someArg2)))))
  val testEmptyValidationErrors: Seq[(JsPath, Seq[ValidationError])] = Seq((jSPath, Seq.empty))
  val testEmptySequence: Seq[(JsPath, Seq[ValidationError])] = Seq.empty

  "ImplicitJsonValidationFormatter.CurrencyFormatter" when {

    "calling asString" should {

      "return a properly formatted string"
        testValidationErrors.asString shouldBe s", path: /someNode and errors: ValidationError(List($someValidationError1, $someValidationError2),ArraySeq(List($someArg1, $someArg2)))"
      }
      "return a properly formatted a strings when ValidationErrors are empty" in {
        testEmptyValidationErrors.asString shouldBe ", path: /someNode and errors: "
      }

      "return an empty string when passing an empty sequence" in {
        testEmptySequence.asString shouldBe ""
      }
    }
  }
}
