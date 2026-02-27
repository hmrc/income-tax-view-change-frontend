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

package forms.reportingObligations.signUp

import forms.reportingObligations.signUp.SingleTaxYearOptInWarningForm
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import testUtils.UnitSpec


class SingleTaxYearOptInWarningFormSpec extends UnitSpec {

  val taxYear = TaxYear(2025, 2026)

  val errorMessage: String = "Select yes to opt out for the 2025 to 2026 tax year"
  implicit val mockMessages: Messages = mock(classOf[play.api.i18n.Messages])

  def validFormData(yesNo: Boolean): Map[String, String] =
    Map(SingleTaxYearOptInWarningForm.choiceField -> yesNo.toString)

  val invalidFormData: Map[String, String] = Map(
    SingleTaxYearOptInWarningForm.choiceField -> ""
  )

  when(mockMessages(SingleTaxYearOptInWarningForm.noResponseErrorMessageKey, taxYear.startYear.toString, taxYear.endYear.toString))
    .thenReturn(errorMessage)


  "SingleTaxYearOptInWarningForm" when {

    "bind with a valid response" should {

      "contain the response - true" in {

        val completedForm: Form[SingleTaxYearOptInWarningForm] =
          SingleTaxYearOptInWarningForm(taxYear).bind(validFormData(true))

        completedForm.hasErrors shouldBe false
        completedForm.data.get(SingleTaxYearOptInWarningForm.choiceField) shouldBe Some(true.toString)
      }

      "contain the response - false" in {

        val completedForm: Form[SingleTaxYearOptInWarningForm] =
          SingleTaxYearOptInWarningForm(taxYear).bind(validFormData(false))

        completedForm.hasErrors shouldBe false
        completedForm.data.get(SingleTaxYearOptInWarningForm.choiceField) shouldBe Some(false.toString)
      }
    }

    "bind with an empty response" should {
      "contain the error message" in {

        val completedForm: Form[SingleTaxYearOptInWarningForm] =
          SingleTaxYearOptInWarningForm(taxYear).bind(invalidFormData)

        completedForm.hasErrors shouldBe true
        completedForm.errors shouldBe List(FormError(SingleTaxYearOptInWarningForm.choiceField, List(errorMessage)))
      }
    }
  }

}