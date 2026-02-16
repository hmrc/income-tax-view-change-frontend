/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.data.Form
import testUtils.TestSupport

class IsTheNewAddressInTheUKFormSpec extends TestSupport {

  lazy val form: Form[IsTheNewAddressInTheUKForm] = IsTheNewAddressInTheUKForm.apply
  "IsTheNewAddressInTheUKCheck form" should {
    "bind with a valid response - yes" in {
      val formData = IsTheNewAddressInTheUKForm(Some("uk-property"))
      val completedForm = form.fill(formData)
      completedForm.data.get(IsTheNewAddressInTheUKForm.response) shouldBe Some(IsTheNewAddressInTheUKForm.responseUK)
      completedForm.errors shouldBe List.empty
    }

    "bind with a valid response - No" in {
      val formData = IsTheNewAddressInTheUKForm(Some("foreign-property"))
      val completedForm = form.fill(formData)
      completedForm.data.get(AddProprertyForm.response) shouldBe Some(IsTheNewAddressInTheUKForm.responseForeign)
      completedForm.errors shouldBe List.empty
    }

    "bind with an invalid response" in {
      val completedForm = form.bind(Map(IsTheNewAddressInTheUKForm.response -> "N/A"))
      completedForm.data.get(IsTheNewAddressInTheUKForm.response) shouldBe Some("N/A")
    }
  }
}
