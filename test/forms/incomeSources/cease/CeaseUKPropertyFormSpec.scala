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

package forms.incomeSources.cease

import forms.incomeSources.cease.CeaseUKPropertyForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}

import scala.language.postfixOps

class CeaseUKPropertyFormSpec extends AnyWordSpec with Matchers{

  def testCeaseUKPropertyForm(declaration: Option[String], csrfToken: String): CeaseUKPropertyForm = CeaseUKPropertyForm(declaration, csrfToken)

  def form(optValue: Option[CeaseUKPropertyForm]): Form[CeaseUKPropertyForm] = CeaseUKPropertyForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(
      value => Map(
        CeaseUKPropertyForm.declaration -> value.declaration.getOrElse("invalid"),
        CeaseUKPropertyForm.ceaseCsrfToken -> value.csrfToken)))

  "CeaseUKProperty form" should {
    "bind with a valid declaration" in {
      form(Some(testCeaseUKPropertyForm(Some("true"), "12345"))).value
        .get.declaration shouldBe Some(testCeaseUKPropertyForm(Some("true"), "12345")).value.declaration
    }
    "bind with an invalid declaration" in {
      form(Some(testCeaseUKPropertyForm(None, "12345"))).errors shouldBe
        Seq(FormError("cease-uk-property-declaration", Seq("incomeSources.ceaseUKProperty.radioError")))
    }
  }
}
