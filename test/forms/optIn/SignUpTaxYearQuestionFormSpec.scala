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

package forms.optIn

import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import testUtils.UnitSpec

class SignUpTaxYearQuestionFormSpec extends UnitSpec {

  val taxYear = TaxYear(2025, 2026)
  val nextTaxYear = taxYear.nextYear
  val errorMessageCY: String = "Select yes to sign up for the current tax year"
  val errorMessageNY: String = s"Select yes to sign up from the ${nextTaxYear.startYear} to ${nextTaxYear.endYear} tax year"
  implicit val mockMessages: Messages = mock(classOf[play.api.i18n.Messages])

  "SignUpTaxYearQuestionForm" when {
    "bind with a valid response" should {
      "contain the response - Yes" in {
        val validFormData: Map[String, String] = Map("sign-up-tax-year-question" -> "Yes")

        val completedForm: Form[SignUpTaxYearQuestionForm] = SignUpTaxYearQuestionForm(taxYear, true).bind(validFormData)
        completedForm.hasErrors shouldBe false
        completedForm.data.get("sign-up-tax-year-question") shouldBe Some("Yes")
      }
      "contain the response - No" in {
        val validFormData: Map[String, String] = Map("sign-up-tax-year-question" -> "No")

        val completedForm: Form[SignUpTaxYearQuestionForm] = SignUpTaxYearQuestionForm(taxYear, false).bind(validFormData)
        completedForm.hasErrors shouldBe false
        completedForm.data.get("sign-up-tax-year-question") shouldBe Some("No")
      }
    }
    "bind with an empty response" should {
      "contain the correct error message for CY" in {
        when(mockMessages.apply("signUp.taxYearQuestion.error.currentYear")).thenReturn(errorMessageCY)

        val invalidFormData: Map[String, String] = Map("sign-up-tax-year-question" -> "")
        val completedForm: Form[SignUpTaxYearQuestionForm] = SignUpTaxYearQuestionForm(taxYear, true).bind(invalidFormData)
        completedForm.data.get("sign-up-tax-year-question") shouldBe Some("")
        completedForm.errors shouldBe List(FormError("sign-up-tax-year-question", List(errorMessageCY)))
      }

      "contain the correct error message for CY+1" in {
        when(mockMessages.apply("signUp.taxYearQuestion.error.nextYear",
          taxYear.startYear.toString,
          taxYear.endYear.toString)).thenReturn(errorMessageNY)

        val invalidFormData: Map[String, String] = Map("sign-up-tax-year-question" -> "")
        val completedForm: Form[SignUpTaxYearQuestionForm] = SignUpTaxYearQuestionForm(taxYear, false).bind(invalidFormData)
        completedForm.data.get("sign-up-tax-year-question") shouldBe Some("")
        completedForm.errors shouldBe List(FormError("sign-up-tax-year-question", List(errorMessageNY)))
      }
    }
  }
}
