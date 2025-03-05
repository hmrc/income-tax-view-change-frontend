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

package forms.manageBusinesses.add

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.Messages

case class IncomeSourceReportingFrequencyForm(reportingFrequencyQuarterly: Option[String], csrfToken: String)

object IncomeSourceReportingFrequencyForm {
  val reportQuarterlyField: String = "report-quarterly"
  val noResponseErrorMessageKey: String = "incomeSources.add.reportingFrequency.form.no-select.error"
  val csrfToken: String = "csrfToken"

  def apply()(implicit messages: Messages): Form[IncomeSourceReportingFrequencyForm] = {
    val noSelectionErrorMessage: String = messages(noResponseErrorMessageKey)
    val validRadioOptions = Set("Yes", "No")

    Form[IncomeSourceReportingFrequencyForm](
      mapping(
        reportQuarterlyField -> optional(text)
          .verifying(noSelectionErrorMessage, response => response.isDefined && validRadioOptions.contains(response.get)),
        csrfToken -> text
      )(IncomeSourceReportingFrequencyForm.apply)(IncomeSourceReportingFrequencyForm.unapply)
    )
  }
}
