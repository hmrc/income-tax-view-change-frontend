/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.optOut

import forms.optOut.ConfirmOptOutSingleTaxYearForm._
import play.api.data.{Form, FormError}
import testUtils.UnitSpec

class ConfirmOptOutSingleTaxYearFormSpec extends UnitSpec {

  val validFormData: Map[String, String] = Map(confirmOptOutField -> "yes")
  val invalidFormData: Map[String, String] = Map(confirmOptOutField -> "")

  "ConfirmOptOutSingleTaxYearForm" when {
    "bind with a valid response" should {
      "contain the response" in {
        val completedForm: Form[ConfirmOptOutSingleTaxYearForm] = ConfirmOptOutSingleTaxYearForm().bind(validFormData);
        completedForm.data.get("confirm.opt.out") shouldBe Some("yes")
      }
    }
    "bind with a empty response" should {
      "contain the error message" in {
        val completedForm: Form[ConfirmOptOutSingleTaxYearForm] = ConfirmOptOutSingleTaxYearForm().bind(invalidFormData);
        completedForm.data.get("confirm.opt.out") shouldBe Some("")
        completedForm.errors shouldBe List(FormError(confirmOptOutField, List(noResponseErrorMessageKey)))
      }
    }
  }
}
