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

package forms.incomeSources.add

import forms.manageBusinesses.add.ChooseTaxYearForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import testUtils.TestSupport

class ChooseTaxYearFormSpec extends AnyWordSpec with Matchers with TestSupport {

  val form: ChooseTaxYearForm = app.injector.instanceOf[ChooseTaxYearForm]

  "ChooseTaxYearForm" when {

    "isOptInOptOutContentUpdateR17 == true" should {

      "return a valid form" when {
        "the current year checkbox is checked" in {
          val validForm = form(true).bind(Map("current-year-checkbox" -> "true"))

          validForm.hasErrors shouldBe false
          validForm.data.get("current-year-checkbox") shouldBe Some("true")
          validForm.data.contains("next-year-checkbox") shouldBe false
        }
        "the next year checkbox is checked" in {
          val validForm = form(true).bind(Map("next-year-checkbox" -> "true"))

          validForm.hasErrors shouldBe false
          validForm.data.get("next-year-checkbox") shouldBe Some("true")
          validForm.data.contains("current-year-checkbox") shouldBe false
        }
        "both checkboxes are checked" in {
          val validForm = form(true).bind(Map(
            "current-year-checkbox" -> "true",
            "next-year-checkbox" -> "true"
          ))

          validForm.hasErrors shouldBe false
          validForm.data.get("current-year-checkbox") shouldBe Some("true")
          validForm.data.get("next-year-checkbox") shouldBe Some("true")
        }
      }

      "return an error" when {
        "none of the checkboxes are checked/defined" in {
          val invalidForm = form(true).bind(Map(
            "Invalid" -> "Invalid"
          ))

          invalidForm.hasErrors shouldBe true
          invalidForm.data.contains("current-year-checkbox") shouldBe false
          invalidForm.data.contains("next-year-checkbox") shouldBe false
          invalidForm.errors shouldBe List(FormError("", List("manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description.feature.switched"), List()))
        }
      }
    }

    "isOptInOptOutContentUpdateR17 == false" should {

      "return a valid form" when {
        "the current year checkbox is checked" in {
          val validForm = form(false).bind(Map("current-year-checkbox" -> "true"))

          validForm.hasErrors shouldBe false
          validForm.data.get("current-year-checkbox") shouldBe Some("true")
          validForm.data.contains("next-year-checkbox") shouldBe false
        }
        "the next year checkbox is checked" in {
          val validForm = form(false).bind(Map("next-year-checkbox" -> "true"))

          validForm.hasErrors shouldBe false
          validForm.data.get("next-year-checkbox") shouldBe Some("true")
          validForm.data.contains("current-year-checkbox") shouldBe false
        }
        "both checkboxes are checked" in {
          val validForm = form(false).bind(Map(
            "current-year-checkbox" -> "true",
            "next-year-checkbox" -> "true"
          ))

          validForm.hasErrors shouldBe false
          validForm.data.get("current-year-checkbox") shouldBe Some("true")
          validForm.data.get("next-year-checkbox") shouldBe Some("true")
        }
      }

      "return an error" when {
        "none of the checkboxes are checked/defined" in {
          val invalidForm = form(false).bind(Map(
            "Invalid" -> "Invalid"
          ))

          invalidForm.hasErrors shouldBe true
          invalidForm.data.contains("current-year-checkbox") shouldBe false
          invalidForm.data.contains("next-year-checkbox") shouldBe false
          invalidForm.errors shouldBe List(FormError("", List("manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description"), List()))
        }
      }
    }
  }
}
