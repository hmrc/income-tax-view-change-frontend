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
                                  agentType: String,
                                  incomeSources: IncomeSourceBuilderForm,
                                  itsaStatus: ITSAStatusBuilderForm,
                                  obligations: ObligationsBuilderForm
                                )

case class IncomeSourceBuilderForm(
                                    userChannel: String,
                                    activeSoleTrader: Boolean,
                                    latentSoleTrader: Boolean,
                                    ceasedSoleTrader: Boolean,
                                    activeUkProperty: Boolean,
                                    ceasedUkProperty: Boolean,
                                    activeForeignProperty: Boolean,
                                    ceasedForeignProperty: Boolean
                                  )

case class ITSAStatusBuilderForm(
                                  cyMinusOneCrystallisationStatus: String,
                                  cyMinusOneItsaStatus: String,
                                  cyItsaStatus: String,
                                  cyPlusOneItsaStatus: String
                                )

case class ObligationsBuilderForm(
                                   annualObligation: String,
                                   quarterlyUpdate1: String,
                                   quarterlyUpdate2: String,
                                   quarterlyUpdate3: String,
                                   quarterlyUpdate4: String
                                 )

object CustomUserBuilderForm {
  
  private val checkbox: Mapping[Boolean] = optional(text).transform(_.contains("true"), if (_) Some("true") else None)

  private val incomeSourcesMapping: Mapping[IncomeSourceBuilderForm] =
    mapping(
      "userChannel" -> nonEmptyText,
      "activeSoleTrader"  -> checkbox,
      "latentSoleTrader"  -> checkbox,
      "ceasedSoleTrader"  -> checkbox,
      "activeUkProperty"  -> checkbox,
      "ceasedUkProperty"  -> checkbox,
      "activeForeignProperty" -> checkbox,
      "ceasedForeignProperty" -> checkbox
    )(
      (userChannel,
       activeSoleTrader,
       latentSoleTrader,
       ceasedSoleTrader,
       activeUkProperty,
       ceasedUkProperty,
       activeForeignProperty,
       ceasedForeignProperty) =>
        IncomeSourceBuilderForm(
          userChannel,
          activeSoleTrader,
          latentSoleTrader,
          ceasedSoleTrader,
          activeUkProperty,
          ceasedUkProperty,
          activeForeignProperty,
          ceasedForeignProperty
        )
    )(
      form =>
        Some((
          form.userChannel,
          form.activeSoleTrader,
          form.latentSoleTrader,
          form.ceasedSoleTrader,
          form.activeUkProperty,
          form.ceasedUkProperty,
          form.activeForeignProperty,
          form.ceasedForeignProperty
        ))
    )
  
  private val itsaStatusMapping: Mapping[ITSAStatusBuilderForm] =
    mapping(
      "cyMinusOneCrystallisationStatus" -> nonEmptyText,
      "cyMinusOneItsaStatus" -> nonEmptyText,
      "cyItsaStatus" -> nonEmptyText,
      "cyPlusOneItsaStatus" -> nonEmptyText
    )(
      (cyMinusOneCrystallisationStatus,
       cyMinusOneItsaStatus,
       cyItsaStatus,
       cyPlusOneItsaStatus) =>
        ITSAStatusBuilderForm(
          cyMinusOneCrystallisationStatus,
          cyMinusOneItsaStatus,
          cyItsaStatus,
          cyPlusOneItsaStatus
        )
    )(
      form =>
        Some((
          form.cyMinusOneCrystallisationStatus,
          form.cyMinusOneItsaStatus,
          form.cyItsaStatus,
          form.cyPlusOneItsaStatus
        ))
    )
  
  private val obligationsMapping: Mapping[ObligationsBuilderForm] =
    mapping(
      "annualObligation" -> nonEmptyText,
      "quarterlyUpdate1" -> nonEmptyText,
      "quarterlyUpdate2" -> nonEmptyText,
      "quarterlyUpdate3" -> nonEmptyText,
      "quarterlyUpdate4" -> nonEmptyText
    )(
      (annualObligation,
       quarterlyUpdate1,
       quarterlyUpdate2,
       quarterlyUpdate3,
       quarterlyUpdate4) =>
        ObligationsBuilderForm(
          annualObligation,
          quarterlyUpdate1,
          quarterlyUpdate2,
          quarterlyUpdate3,
          quarterlyUpdate4
        )
    )(
      form =>
        Some((
          form.annualObligation,
          form.quarterlyUpdate1,
          form.quarterlyUpdate2,
          form.quarterlyUpdate3,
          form.quarterlyUpdate4
        ))
    )
  
  val form: Form[CustomUserBuilderForm] =
    Form(
      mapping(
        "agentType"     -> nonEmptyText,
        "incomeSources" -> incomeSourcesMapping,
        "itsaStatus"    -> itsaStatusMapping,
        "obligations"   -> obligationsMapping
      )(
        (agentType, incomeSources, itsaStatus, obligations) =>
          CustomUserBuilderForm(
            agentType,
            incomeSources,
            itsaStatus,
            obligations
          )
      )(
        form =>
          Some((
            form.agentType,
            form.incomeSources,
            form.itsaStatus,
            form.obligations
          ))
      )
    )
}