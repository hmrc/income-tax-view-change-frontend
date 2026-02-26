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

import models.incomeSourceDetails.TaxYear
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import play.api.i18n.Messages

case class SingleTaxYearOptInWarningForm(choice: Option[Boolean])

object SingleTaxYearOptInWarningForm {

  val choiceField: String = "choice"
  val noResponseErrorMessageKey: String = "optIn.singleTaxYearWarning.form.error"

  def apply(taxYear: TaxYear)(implicit messages: Messages): Form[SingleTaxYearOptInWarningForm] = {
    val noSelectionErrorMessage: String =
      messages(noResponseErrorMessageKey, taxYear.startYear.toString, taxYear.endYear.toString)

    Form[SingleTaxYearOptInWarningForm](
      mapping(
        choiceField ->
          optional(boolean).verifying(noSelectionErrorMessage, optionalChoice => optionalChoice.nonEmpty)
      )
      (choice => SingleTaxYearOptInWarningForm(choice))
      (form => Some(form.choice))
    )
  }
}