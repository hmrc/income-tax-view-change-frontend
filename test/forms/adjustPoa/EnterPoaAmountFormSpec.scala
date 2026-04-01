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

import play.api.data.Form
import testUtils.TestSupport


class EnterPoaAmountFormSpec extends TestSupport {

  val form = EnterPoaAmountForm.form.bind(Map("poa-amount" -> "1000"))
  "checkValueConstraints" should {
    
    "return a valid form" when {
      
      "an amount is entered below the POA relevant amount" in {
        EnterPoaAmountForm.checkValueConstraints(form, 1500, 2000) shouldBe form
      }

      "an amount is entered with a POA original amount of 0" in {
        EnterPoaAmountForm.checkValueConstraints(form, 1500, 0) shouldBe form
      }
    }

    "return a form with errors" when {

      "an amount is entered equal to the current POA value" in {
        val result = EnterPoaAmountForm.checkValueConstraints(form, 1000, 2000)

        result.hasErrors shouldBe true
        result.errors.exists(_.message == "The amount for each payment on account must be different from the current amount (£1,000.00)") shouldBe true
      }

      "an amount is entered above the POA's original value" in {
        val result = EnterPoaAmountForm.checkValueConstraints(form, 1500, 500)

        result.hasErrors shouldBe true
        result.errors.exists(_.message == "The amount for each payment on account must be lower than the amount initially created by HMRC (£500.00)") shouldBe true
      }
    }
  }
}
