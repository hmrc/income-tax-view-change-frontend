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

package forms.optOut

import models.incomeSourceDetails.TaxYear
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.Messages

object OptOutTaxYearQuestionForm {

  val responseNo: String = "No"
  val responseYes: String = "Yes"
  val response: String = "opt-out-tax-year-question"
  val noSelectErrorMessageKey: String = "optout.taxYearQuestion.error"


  def apply(taxYear: TaxYear)(implicit messages: Messages): Form[OptOutTaxYearQuestionForm] = {
    val noSelectionErrorMessage: String =
      messages(noSelectErrorMessageKey,
        taxYear.startYear.toString,
        taxYear.endYear.toString)

    Form[OptOutTaxYearQuestionForm](
      mapping(
        response -> optional(text)
          .verifying(noSelectionErrorMessage, value => value.nonEmpty && (value.contains(responseYes) || value.contains(responseNo)))
      )(OptOutTaxYearQuestionForm.apply)(OptOutTaxYearQuestionForm.unapply)
    )
  }
}

case class OptOutTaxYearQuestionForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    OptOutTaxYearQuestionForm.response -> Seq(response.getOrElse("N/A"))
  )
}