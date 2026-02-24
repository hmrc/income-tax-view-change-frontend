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

package forms.optOut

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.Messages

case class ConfirmOptOutMultiTaxYearChoiceForm(choice: Option[String])

object ConfirmOptOutMultiTaxYearChoiceForm {

  val choiceField: String = "choice"
  val noResponseErrorMessageKey: String = "optOut.ConfirmOptOutMultiTaxYearChoice.form.no-select.error"
  val csrfToken: String = "csrfToken"

  def
  apply(optionValue: List[String])(implicit messages: Messages): Form[ConfirmOptOutMultiTaxYearChoiceForm] = {
    val noSelectionErrorMessage: String = messages(noResponseErrorMessageKey)

    form(noSelectionErrorMessage, optionValue)

  }

  def form(msg: String, optOutYears: List[String]): Form[ConfirmOptOutMultiTaxYearChoiceForm] = Form[ConfirmOptOutMultiTaxYearChoiceForm](
    mapping(
      choiceField -> optional(text).verifying(msg, optionalChoice => optionalChoice.nonEmpty && optOutYears.contains(optionalChoice.get))
    )
    (choice => ConfirmOptOutMultiTaxYearChoiceForm(choice))(
      form => Some(form.choice))
  )


}