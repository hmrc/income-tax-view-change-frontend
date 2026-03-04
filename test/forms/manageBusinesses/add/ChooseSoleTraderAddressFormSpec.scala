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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import testUtils.TestSupport

class ChooseSoleTraderAddressFormSpec extends AnyWordSpec with Matchers with TestSupport {

  private val errorMessage = "Select an option to continue"

  "ChooseSoleTraderAddressForm.form" should {

    "bind successfully when a valid existing address id is selected" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map(ChooseSoleTraderAddressForm.fieldName -> "1")
      val bound = form.bind(data)

      bound.hasErrors shouldBe false
      bound.value shouldBe Some(ChooseSoleTraderAddressForm("1"))
    }

    "bind successfully when new-address is selected" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map(ChooseSoleTraderAddressForm.fieldName -> "new-address")
      val bound = form.bind(data)

      bound.hasErrors shouldBe false
      bound.value shouldBe Some(ChooseSoleTraderAddressForm("new-address"))
    }

    "fail to bind when the field is missing" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map.empty[String, String]
      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.fieldName).map(_.message) shouldBe Some(errorMessage)
    }

    "fail to bind when the field is present but empty" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map(ChooseSoleTraderAddressForm.fieldName -> "")
      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.fieldName).map(_.message) shouldBe Some(errorMessage)
    }

    "fail to bind when the field contains an invalid (tampered) value" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map(ChooseSoleTraderAddressForm.fieldName -> "banana")
      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.fieldName).map(_.message) shouldBe Some(errorMessage)
    }

    "fail to bind when allowedValues is empty (no selection should validate)" in {
      val allowedValues = Seq.empty[String]
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val data = Map(ChooseSoleTraderAddressForm.fieldName -> "0")
      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.fieldName).map(_.message) shouldBe Some(errorMessage)
    }

    "fill correctly when a response is provided" in {
      val allowedValues = Seq("0", "1", "2", "new-address")
      val form = ChooseSoleTraderAddressForm.form(allowedValues)

      val filled = form.fill(ChooseSoleTraderAddressForm("2"))

      filled.value shouldBe Some(ChooseSoleTraderAddressForm("2"))
      // Optional: verify the data that would be rendered back to the view
      filled.data.get(ChooseSoleTraderAddressForm.fieldName) shouldBe Some("2")
    }
  }
}