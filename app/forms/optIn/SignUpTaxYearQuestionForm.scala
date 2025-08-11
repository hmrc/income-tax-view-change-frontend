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
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.Messages

object SignUpTaxYearQuestionForm {

  val responseNo: String = "No"
  val responseYes: String = "Yes"
  val response: String = "sign-up-tax-year-question"

  def apply(taxYear: TaxYear, signUpForCY: Boolean)(implicit messages: Messages): Form[SignUpTaxYearQuestionForm] = {
    val noSelectionErrorMessage: String = if (signUpForCY) {
      messages("signUp.taxYearQuestion.error.currentYear")
    } else {
      messages("signUp.taxYearQuestion.error.nextYear", taxYear.startYear.toString, taxYear.endYear.toString)
    }

    Form[SignUpTaxYearQuestionForm](
      mapping(
        response -> optional(text)
          .verifying(noSelectionErrorMessage, value => value.nonEmpty && (value.contains(responseYes) || value.contains(responseNo)))
      )(SignUpTaxYearQuestionForm.apply)(SignUpTaxYearQuestionForm.unapply)
    )
  }
}

case class SignUpTaxYearQuestionForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    SignUpTaxYearQuestionForm.response -> Seq(response.getOrElse("N/A"))
  )
}