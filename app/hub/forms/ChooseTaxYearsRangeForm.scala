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

package hub.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}

object ChooseTaxYearsRangeForm {
  val response: String = "choose-tax-years-range"
  val mtdOption: String = "MTD"
  val legacyOption: String = "SA"
  private val noSelectionError = "manageBusinesses.type-of-property.error"

  def apply(): Form[ChooseTaxYearsRangeOption] =
    Form(
      mapping(
        response -> optional(text)
          .verifying(noSelectionError, value => value.nonEmpty && value.exists(v => v == mtdOption || v == legacyOption))
      )(value => ChooseTaxYearsRangeOption(value))(value => Some(value.selection))
    )
}

case class ChooseTaxYearsRangeOption(selection: Option[String])
