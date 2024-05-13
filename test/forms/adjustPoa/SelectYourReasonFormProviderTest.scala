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

package forms.adjustPoa

import models.claimToAdjustPoa.{Increase, MainIncomeLower, SelectYourReason}
import play.api.data.{Form, FormError}
import testUtils.TestSupport


class SelectYourReasonFormProviderTest extends TestSupport {


  lazy val form: Form[SelectYourReason] = new SelectYourReasonFormProvider().apply()
  "Select Your Reason form" should {

    "bind" when {
      "with a valid response" in {
        val completedForm = form.bind(Map("value" -> "MainIncomeLower"))
        completedForm.value shouldBe Some(MainIncomeLower)
        completedForm.errors shouldBe List.empty
        completedForm.data shouldBe Map("value" -> "MainIncomeLower")
      }

      "with a invalid response" in {
        val completedForm = form.bind(Map("value" -> "Some other value"))
        completedForm.value shouldBe None
        completedForm.errors shouldBe List(FormError("value", List("error.invalid"), List()))
      }

      "with no response" in {
        val completedForm = form.bind(Map.empty[String, String])
        completedForm.value shouldBe None
        completedForm.errors shouldBe List(FormError("value", List("adjust-poa.select-your-reason.error.required"), List()))
      }
    }

    "fill" when {
      "with a valid response" in {
        val formData = MainIncomeLower
        val completedForm = form.fill(formData)
        completedForm.value shouldBe Some(MainIncomeLower)
        completedForm.errors shouldBe List.empty
        completedForm.data shouldBe Map("value" -> "MainIncomeLower")
      }

      "with a valid response not on form" in {
        val formData = Increase
        val completedForm = form.fill(formData)
        completedForm.value shouldBe Some(Increase)
        completedForm.errors shouldBe List.empty
      }
    }




  }
}
