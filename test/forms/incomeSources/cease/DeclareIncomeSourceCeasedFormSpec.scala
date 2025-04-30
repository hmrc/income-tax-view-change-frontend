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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}


class DeclareIncomeSourceCeasedFormSpec extends AnyWordSpec with Matchers {

  def testDeclarePropertyCeasedForm(declaration: Option[String], csrfToken: String): DeclareIncomeSourceCeasedForm = DeclareIncomeSourceCeasedForm(declaration, csrfToken)

  def form(incomeSourceType: IncomeSourceType, optValue: Option[DeclareIncomeSourceCeasedForm]): Form[DeclareIncomeSourceCeasedForm] = DeclareIncomeSourceCeasedForm.form(incomeSourceType).bind(
    optValue.fold[Map[String, String]](Map.empty)(
      value => Map(
        DeclareIncomeSourceCeasedForm.declaration -> value.declaration.getOrElse("invalid"),
        DeclareIncomeSourceCeasedForm.ceaseCsrfToken -> value.csrfToken)))

  "UK Property - declare property ceased form" should {
    "bind with a valid declaration" in {
      form(UkProperty, Some(testDeclarePropertyCeasedForm(Some("true"), "12345"))).value
        .get.declaration shouldBe Some(testDeclarePropertyCeasedForm(Some("true"), "12345")).value.declaration
    }
    "bind with an invalid declaration" in {
      form(UkProperty, Some(testDeclarePropertyCeasedForm(None, "12345"))).errors shouldBe
        Seq(FormError(DeclareIncomeSourceCeasedForm.declaration, Seq("incomeSources.cease.UK.checkboxError")))
    }
  }

  "Foreign Property - declare property ceased form" should {
    "bind with a valid declaration" in {
      form(ForeignProperty, Some(testDeclarePropertyCeasedForm(Some("true"), "12345"))).value
        .get.declaration shouldBe Some(testDeclarePropertyCeasedForm(Some("true"), "12345")).value.declaration
    }
    "bind with an invalid declaration" in {
      form(ForeignProperty, Some(testDeclarePropertyCeasedForm(None, "12345"))).errors shouldBe
        Seq(FormError(DeclareIncomeSourceCeasedForm.declaration, Seq("incomeSources.cease.FP.checkboxError")))
    }
  }

  "SelfEmployment - declare business ceased form" should {
    "bind with a valid declaration" in {
      form(SelfEmployment, Some(testDeclarePropertyCeasedForm(Some("true"), "12345"))).value
        .get.declaration shouldBe Some(testDeclarePropertyCeasedForm(Some("true"), "12345")).value.declaration
    }
    "bind with an invalid declaration" in {
      form(SelfEmployment, Some(testDeclarePropertyCeasedForm(None, "12345"))).errors shouldBe
        Seq(FormError(DeclareIncomeSourceCeasedForm.declaration, Seq("incomeSources.cease.SE.checkboxError")))
    }
  }
}
