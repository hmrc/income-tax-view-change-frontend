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

package forms.reportingObligations.optOut

import forms.reportingObligations.optOut.ConfirmOptOutSingleTaxYearForm
import forms.reportingObligations.optOut.ConfirmOptOutSingleTaxYearForm.*
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import testUtils.UnitSpec

class ConfirmOptOutSingleTaxYearFormSpec extends UnitSpec {

  val validFormData: Map[String, String] = Map(confirmOptOutField -> "true", csrfToken -> "")
  val invalidFormData: Map[String, String] = Map(confirmOptOutField -> "", csrfToken -> "")
  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val errorMessage: String = s"Select yes to opt out for the ${taxYear.startYear} to ${taxYear.endYear} tax year"
  implicit val mockMessages: Messages = mock(classOf[play.api.i18n.Messages])

  when(mockMessages.apply(
    ConfirmOptOutSingleTaxYearForm.noResponseErrorMessageKey,
    taxYear.startYear.toString,
    taxYear.endYear.toString)).thenReturn(errorMessage)

  "ConfirmOptOutSingleTaxYearForm" when {
    "bind with a valid response" should {
      "contain the response - true" in {
        val completedForm: Form[ConfirmOptOutSingleTaxYearForm] = ConfirmOptOutSingleTaxYearForm(taxYear).bind(validFormData)
        completedForm.hasErrors shouldBe false
        completedForm.data.get(confirmOptOutField) shouldBe Some("true")
      }
    }

    "bind with a empty response" should {
      "contain the error message" in {
        val completedForm: Form[ConfirmOptOutSingleTaxYearForm] = ConfirmOptOutSingleTaxYearForm(taxYear).bind(invalidFormData)
        completedForm.data.get(confirmOptOutField) shouldBe Some("")
        completedForm.errors shouldBe List(FormError(confirmOptOutField, List(errorMessage)))
      }
    }
  }
}
