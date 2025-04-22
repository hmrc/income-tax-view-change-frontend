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

import forms.mappings.Constraints
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

case class ChooseTaxYearForm(currentTaxYear: Option[Boolean], nextTaxYear: Option[Boolean])

object ChooseTaxYearForm extends Constraints {
  val chooseTaxYearsCheckbox = "choose-tax-year"
  val currentYearCheckbox = "current-year-checkbox"
  val nextYearCheckbox = "next-year-checkbox"
  val noResponseErrorMessageKey: String = "manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description"

  def apply(): Form[ChooseTaxYearForm] = {
    Form[ChooseTaxYearForm](
      mapping(
        currentYearCheckbox -> optional(boolean),
        nextYearCheckbox -> optional(boolean)
      )(ChooseTaxYearForm.apply)(ChooseTaxYearForm.unapply)
        .verifying(atLeastOneChecked)
    )
  }

  private val atLeastOneChecked: Constraint[ChooseTaxYearForm] =
    Constraint("constraints.atLeastOneChecked") {
      data =>
        if (data.currentTaxYear.getOrElse(false) || data.nextTaxYear.getOrElse(false)) {
          Valid
        } else {
          Invalid(Seq(ValidationError(noResponseErrorMessageKey)))
        }
    }
}
