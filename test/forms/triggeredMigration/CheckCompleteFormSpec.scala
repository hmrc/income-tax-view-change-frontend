/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.triggeredMigration

import play.api.data.Form
import testUtils.TestSupport

class CheckCompleteFormSpec extends TestSupport {

  lazy val form: Form[CheckCompleteForm] = CheckCompleteForm()

  "CheckCompleteForm" should {

    "bind with a valid response - Continue" in {
      val result = form.bind(Map(CheckCompleteForm.response -> CheckCompleteForm.responseContinue))
      result.errors shouldBe empty
      result.value shouldBe Some(CheckCompleteForm(Some(CheckCompleteForm.responseContinue)))
    }

    "fail to bind with an invalid response" in {
      val result = form.bind(Map(CheckCompleteForm.response -> "invalid"))
      result.errors.map(_.message) should contain(CheckCompleteForm.errorKey)
    }
  }
}
