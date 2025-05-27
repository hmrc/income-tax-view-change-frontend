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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import forms.mappings.Constraints
import forms.models.ChooseTaxYearFormModel
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import javax.inject.Inject

class ChooseTaxYearForm @Inject()(val appConfig: FrontendAppConfig) extends Constraints with FeatureSwitching {

  private val currentYearCheckbox = "current-year-checkbox"
  private val nextYearCheckbox = "next-year-checkbox"

  private def atLeastOneChecked(isOptInOptOutContentUpdateR17: Boolean): Constraint[ChooseTaxYearFormModel] =
    Constraint("constraints.atLeastOneChecked") {
      data =>
        if (data.currentTaxYear.getOrElse(false) || data.nextTaxYear.getOrElse(false)) {
          Valid
        } else {
          Invalid(Seq(ValidationError(noResponseErrorMessageKey(isOptInOptOutContentUpdateR17))))
        }
    }

  def noResponseErrorMessageKey(isOptInOptOutContentUpdateR17: Boolean): String = {
    if (isOptInOptOutContentUpdateR17) "manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description.feature.switched"
    else "manageBusinesses.add.addReportingFrequency.soleTrader.chooseTaxYear.error.description"
  }

  def apply(isOptInOptOutContentUpdateR17: Boolean): Form[ChooseTaxYearFormModel] = {
    Form[ChooseTaxYearFormModel](
      mapping(
        currentYearCheckbox -> optional(boolean),
        nextYearCheckbox -> optional(boolean)
      )(ChooseTaxYearFormModel.apply)(ChooseTaxYearFormModel.unapply)
        .verifying(atLeastOneChecked(isOptInOptOutContentUpdateR17))
    )
  }
}
