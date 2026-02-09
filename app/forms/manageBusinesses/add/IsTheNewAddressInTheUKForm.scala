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

object IsTheNewAddressInTheUKForm {

  val responseUK: String = "uk-property"
  val responseForeign: String = "foreign-property"
  val response: String = "type-of-business"


  def apply: Form[IsTheNewAddressInTheUKForm] = {

    val radiosEmptyError: String = "manageBusinesses.type-of-property.error"

    Form[IsTheNewAddressInTheUKForm](
      mapping(
        response -> optional(text)
          .verifying(radiosEmptyError, value => value.nonEmpty && (value.contains(responseUK) || value.contains(responseForeign)))
      )
      (response => IsTheNewAddressInTheUKForm(response))
      (form => Some(form.response))
    )
  }
}

case class IsTheNewAddressInTheUKForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    IsTheNewAddressInTheUKForm.response -> Seq(response.getOrElse("N/A"))
  )
}
