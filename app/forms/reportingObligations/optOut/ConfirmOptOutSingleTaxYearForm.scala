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

import models.incomeSourceDetails.TaxYear
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.i18n.Messages

case class ConfirmOptOutSingleTaxYearForm(confirmOptOut: Option[Boolean], csrfToken: String)

object ConfirmOptOutSingleTaxYearForm {
  val confirmOptOutField: String = "confirm-opt-out"
  val noResponseErrorMessageKey: String = "optOut.confirmSingleYearOptOut.form.no-select.error"
  val csrfToken: String = "csrfToken"


  def apply(taxYear: TaxYear)(implicit messages: Messages): Form[ConfirmOptOutSingleTaxYearForm] = {
    val noSelectionErrorMessage: String =
      messages(noResponseErrorMessageKey,
        taxYear.startYear.toString,
        taxYear.endYear.toString)

    Form(
      mapping(
        confirmOptOutField -> optional(boolean).verifying(
          noSelectionErrorMessage, response => response.isInstanceOf[Some[Boolean]]),
        csrfToken -> text
      )
      (
        (confirmOptOut, csrfToken) => ConfirmOptOutSingleTaxYearForm(confirmOptOut, csrfToken)
      )
      (
        form => Some((form.confirmOptOut, form.csrfToken))
      )
    )
  }
}
