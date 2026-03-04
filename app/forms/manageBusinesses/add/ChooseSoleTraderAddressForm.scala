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

package forms.manageBusinesses.add

import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages

object ChooseSoleTraderAddressForm {

  val fieldName = "value"

  def form(allowedValues: Seq[String])
          (implicit messages: Messages): Form[ChooseSoleTraderAddressForm] = {

    Form(
      mapping(
        fieldName -> optional(text)
          .verifying(
            messages("manageBusinesses.add.chooseSoleTraderAddress.radio.option.error"),
            opt => opt.exists((v: String) => allowedValues.contains(v))
          )
      )(opt => ChooseSoleTraderAddressForm(opt.getOrElse("")))
        (form => Some(Some(form.response)))
    )
  }
}

case class ChooseSoleTraderAddressForm(response: String)