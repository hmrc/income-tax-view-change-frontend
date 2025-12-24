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

import forms.mappings.Constraints
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}

case class IncomeSourceReportingFrequencyForm(yesNo: Option[String])

object IncomeSourceReportingFrequencyForm extends Constraints {

  val responseYes = "true"
  val responseNo = "false"
  val yesNoAnswer = "reporting-quarterly-form"

  def apply(isR17ContentEnabled: Boolean): Form[IncomeSourceReportingFrequencyForm] = {
    val noResponseErrorMessageKey = if (isR17ContentEnabled) {
      "incomeSources.add.reportingFrequency.r17.form.no-select.error"
    } else {
      "incomeSources.add.reportingFrequency.form.no-select.error"
    }

    Form[IncomeSourceReportingFrequencyForm](
      mapping(
        yesNoAnswer -> optional(text)
          .verifying(noResponseErrorMessageKey, value => value.nonEmpty && (value.contains(responseYes) || value.contains(responseNo)))
      )
      (yesNoAnswer => IncomeSourceReportingFrequencyForm(yesNoAnswer))
      (form => Some(form.yesNo))
    )
  }
}
