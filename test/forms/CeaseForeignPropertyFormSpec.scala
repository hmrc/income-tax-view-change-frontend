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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}

import scala.language.postfixOps

class CeaseForeignPropertyFormSpec extends AnyWordSpec with Matchers{

  def testCeaseForeignPropertyForm(declaration: Option[String], csrfToken: String): CeaseForeignPropertyForm = CeaseForeignPropertyForm(declaration, csrfToken)

  def form(optValue: Option[CeaseForeignPropertyForm]): Form[CeaseForeignPropertyForm] = CeaseForeignPropertyForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(
      value => Map(
        CeaseForeignPropertyForm.declaration -> value.declaration.getOrElse("invalid"),
        CeaseForeignPropertyForm.ceaseCsrfToken -> value.csrfToken)))

  "CeaseForeignProperty form" should {
    "bind with a valid declaration" in {
      form(Some(testCeaseForeignPropertyForm(Some("true"), "12345"))).value
        .get.declaration shouldBe Some(testCeaseForeignPropertyForm(Some("true"), "12345")).value.declaration
    }
    "bind with an invalid declaration" in {
      form(Some(testCeaseForeignPropertyForm(None, "12345"))).errors shouldBe
        Seq(FormError("cease-foreign-property-declaration", Seq("incomeSources.ceaseForeignProperty.checkboxError")))
    }
  }
}
