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

import forms.reportingObligations.signUp.ChooseTaxYearForm
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.{mock, when}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import testUtils.UnitSpec

class ChooseTaxYearFormSpec extends UnitSpec {

  val forYearEnd = 2023
  val taxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)
  val choices: List[String] = List(taxYear, taxYear.nextYear).map(_.toString)

  val errorMessage: String = s"Select the tax year you want to report quarterly from"
  implicit val mockMessages: Messages = mock(classOf[play.api.i18n.Messages])

  def validFormData(taxYearToValidate: TaxYear): Map[String, String] = Map(ChooseTaxYearForm.choiceField -> taxYearToValidate.toString)
  val invalidFormData: Map[String, String] = Map(ChooseTaxYearForm.choiceField -> "")

  when(mockMessages.apply(ChooseTaxYearForm.noResponseErrorMessageKey)
  ).thenReturn(errorMessage)

  "ChooseTaxYearForm" when {

    "bind with a valid response" should {
      "contain the response - true" in {
        val completedForm: Form[ChooseTaxYearForm] = ChooseTaxYearForm(choices).bind(validFormData(taxYear))
        completedForm.hasErrors shouldBe false
        completedForm.data.get(ChooseTaxYearForm.choiceField) shouldBe Some(taxYear.toString)
      }
    }

    "bind with a empty response" should {
      "contain the error message" in {
        val completedForm: Form[ChooseTaxYearForm] = ChooseTaxYearForm(choices).bind(invalidFormData)
        completedForm.data.get(ChooseTaxYearForm.choiceField) shouldBe Some("")
        completedForm.errors shouldBe List(FormError(ChooseTaxYearForm.choiceField, List(errorMessage)))
      }
    }
  }
}