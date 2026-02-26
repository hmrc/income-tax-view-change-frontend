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

package forms.reportingObligations.optOut

import forms.reportingObligations.optOut.OptOutTaxYearQuestionForm
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import testUtils.UnitSpec

class OptOutTaxYearQuestionFormSpec extends UnitSpec {

  val taxYear = TaxYear(2025, 2026)
  val errorMessage: String = s"Select yes to opt out for the ${taxYear.startYear} to ${taxYear.endYear} tax year"
  implicit val mockMessages: Messages = mock(classOf[play.api.i18n.Messages])

  when(mockMessages.apply(
    OptOutTaxYearQuestionForm.noSelectErrorMessageKey,
    taxYear.startYear.toString,
    taxYear.endYear.toString)).thenReturn(errorMessage)

  "OptOutTaxYearQuestionForm" when {
    "bind with a valid response" should {
      "contain the response - Yes" in {
        val validFormData: Map[String, String] = Map("opt-out-tax-year-question" -> "Yes")

        val completedForm: Form[OptOutTaxYearQuestionForm] = OptOutTaxYearQuestionForm(taxYear).bind(validFormData)
        completedForm.hasErrors shouldBe false
        completedForm.data.get("opt-out-tax-year-question") shouldBe Some("Yes")
      }
      "contain the response - No" in {
        val validFormData: Map[String, String] = Map("opt-out-tax-year-question" -> "No")

        val completedForm: Form[OptOutTaxYearQuestionForm] = OptOutTaxYearQuestionForm(taxYear).bind(validFormData)
        completedForm.hasErrors shouldBe false
        completedForm.data.get("opt-out-tax-year-question") shouldBe Some("No")
      }
    }
    "bind with an empty response" should {
      "contain the error message" in {
        val invalidFormData: Map[String, String] = Map("opt-out-tax-year-question" -> "")
        val completedForm: Form[OptOutTaxYearQuestionForm] = OptOutTaxYearQuestionForm(taxYear).bind(invalidFormData)
        completedForm.data.get("opt-out-tax-year-question") shouldBe Some("")
        completedForm.errors shouldBe List(FormError("opt-out-tax-year-question", List(errorMessage)))
      }
    }
  }
}
