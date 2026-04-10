/*
 * Copyright 2026 HM Revenue & Customs
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

package testOnly.forms.customLoginV2

import play.api.data.{Form, Mapping}
import play.api.data.Forms.*

case class CustomUserBuilderForm(
                                  userType: String,
                                  activeSoleTrader: Boolean,
                                  activeUkProperty: Boolean,
                                  activeForeignProperty: Boolean,
                                  previousYearCrystallisationStatus: String,
                                  previousYearItsaStatus: String,
                                  currentYearItsaStatus: String,
                                  nextYearItsaStatus: String
                                )

object CustomUserBuilderForm {

  private val checkbox: Mapping[Boolean] = optional(text).transform(_.contains("true"), if (_) Some("true") else None)

  val form: Form[CustomUserBuilderForm] = Form(
    mapping(
      "AgentType"               -> nonEmptyText,
      "SoleTraderCheckbox"      -> checkbox,
      "UkPropertyCheckbox"      -> checkbox,
      "ForeignPropertyCheckbox" -> checkbox,
      "cyMinusOneCrystallisationStatus" -> nonEmptyText,
      "cyMinusOneItsaStatus"    -> nonEmptyText,
      "cyItsaStatus"            -> nonEmptyText,
      "cyPlusOneItsaStatus"     -> nonEmptyText
    )(
      (userType,
       activeSoleTrader,
       activeUkProperty,
       activeForeignProperty,
       previousYearCrystallisationStatus,
       previousYearItsaStatus,
       currentYearItsaStatus,
       nextYearItsaStatus) => CustomUserBuilderForm(
        userType,
        activeSoleTrader,
        activeUkProperty,
        activeForeignProperty,
        previousYearCrystallisationStatus,
        previousYearItsaStatus,
        currentYearItsaStatus,
        nextYearItsaStatus
      )
    )(
      form => Some(
        form.userType,
        form.activeSoleTrader,
        form.activeUkProperty,
        form.activeForeignProperty,
        form.previousYearCrystallisationStatus,
        form.previousYearItsaStatus,
        form.currentYearItsaStatus,
        form.nextYearItsaStatus
      )
    )
  )
}