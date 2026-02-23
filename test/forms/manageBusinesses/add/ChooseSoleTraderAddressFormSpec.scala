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

  private val form = ChooseSoleTraderAddressForm()

  "ChooseSoleTraderAddressForm" should {

    "bind successfully when new-address is selected" in {

      val data = Map(ChooseSoleTraderAddressForm.response -> ChooseSoleTraderAddressForm.newAddress)

      val bound = form.bind(data)

      bound.hasErrors shouldBe false
      bound.value shouldBe Some(ChooseSoleTraderAddressForm(Some(ChooseSoleTraderAddressForm.newAddress)))
    }

    "bind successfully when existing-address is selected" in {
      val data = Map(ChooseSoleTraderAddressForm.response -> ChooseSoleTraderAddressForm.existingAddress)

      val bound = form.bind(data)

      bound.hasErrors shouldBe false
      bound.value shouldBe Some(ChooseSoleTraderAddressForm(Some(ChooseSoleTraderAddressForm.existingAddress)))
    }

    "fail to bind when the field is missing" in {
      val data = Map.empty[String, String]

      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.response).map(_.message) shouldBe
        Some("Select an option to continue")
    }

    "fail to bind when the field is present but empty" in {
      val data = Map(ChooseSoleTraderAddressForm.response -> "")

      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.response).map(_.message) shouldBe
        Some("Select an option to continue")
    }

    "fail to bind when the field has an invalid value" in {
      val data = Map(ChooseSoleTraderAddressForm.response -> "something-else")

      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.response).map(_.message) shouldBe
        Some("Select an option to continue")
    }

    "fail to bind when the field only contains a partial match" in {
      val data = Map(ChooseSoleTraderAddressForm.response -> "xxx-new-address-yyy")

      val bound = form.bind(data)

      bound.hasErrors shouldBe true
      bound.error(ChooseSoleTraderAddressForm.response).map(_.message) shouldBe
        Some("Select an option to continue")
    }
  }

  "ChooseSoleTraderAddressForm.toFormMap" should {

    "return chosen-address mapped to the selected value when present" in {
      val model = ChooseSoleTraderAddressForm(Some(ChooseSoleTraderAddressForm.newAddress))

      model.toFormMap shouldBe Map(
        ChooseSoleTraderAddressForm.response -> Seq(ChooseSoleTraderAddressForm.newAddress)
      )
    }

    "return chosen-address mapped to N/A when response is None" in {
      val model = ChooseSoleTraderAddressForm(None)

      model.toFormMap shouldBe Map(
        ChooseSoleTraderAddressForm.response -> Seq("N/A")
      )
    }
  }
}