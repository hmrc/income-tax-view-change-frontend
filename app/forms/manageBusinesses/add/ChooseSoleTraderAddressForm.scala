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

  val newAddress: String = "new-address"
  val existingAddress: String = "existing-address"
  val response: String = "chosen-address"


  def apply()(implicit messages: Messages): Form[ChooseSoleTraderAddressForm] = {

    Form[ChooseSoleTraderAddressForm](
      mapping(
        response -> optional(text)
          .verifying(messages("manageBusinesses.add.chooseSoleTraderAddress.radio.option.error"), value => value.nonEmpty && (value.contains(newAddress) || value.contains(existingAddress)))
      )(response => ChooseSoleTraderAddressForm(response))
      (form => Some(form.response))
    )
  }
}

case class ChooseSoleTraderAddressForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    ChooseSoleTraderAddressForm.response -> Seq(response.getOrElse("N/A"))
  )
}
