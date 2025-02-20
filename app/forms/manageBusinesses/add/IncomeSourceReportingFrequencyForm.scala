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

package forms.manageBusinesses.add

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

case class IncomeSourceReportingFrequencyForm(currentTaxYear: Boolean, nextTaxYear: Boolean)

object IncomeSourceReportingFrequencyForm {
  val chooseTaxYearsCheckbox = "choose-tax-year"
  val noResponseErrorMessageKey: String = "manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description"

  def apply(): Form[IncomeSourceReportingFrequencyForm] = {
    Form[IncomeSourceReportingFrequencyForm](
      mapping(
        "current-year-checkbox" -> boolean,
        "next-year-checkbox" -> boolean
      )(IncomeSourceReportingFrequencyForm.apply)(IncomeSourceReportingFrequencyForm.unapply)
        .verifying(atLeastOneChecked)
    )
  }

  private val atLeastOneChecked: Constraint[IncomeSourceReportingFrequencyForm] =
    Constraint("constraints.atLeastOneChecked") {
      data =>
        if (data.currentTaxYear || data.nextTaxYear) {
          Valid
        } else {
          Invalid(Seq(ValidationError(noResponseErrorMessageKey)))
        }
    }
}
