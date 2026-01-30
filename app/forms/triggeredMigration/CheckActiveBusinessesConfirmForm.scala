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

package forms.triggeredMigration

import play.api.data.Form
import play.api.data.Forms._

object CheckActiveBusinessesConfirmForm {

  val responseYes: String = "Yes"
  val responseNo: String  = "No"
  val response: String    = "check-active-businesses-confirm-form"
  val errorKey = "triggered-migration.check-active-businesses-confirm.error.required"

  def apply(): Form[CheckActiveBusinessesConfirmForm] = {
    Form[CheckActiveBusinessesConfirmForm](
      mapping(
        response -> optional(text)
          .verifying(errorKey, value =>
            value.nonEmpty && (value.contains(responseYes) || value.contains(responseNo))
          )
      )
      (response => CheckActiveBusinessesConfirmForm(response))
      (form => Some(form.response))
    )
  }
}

case class CheckActiveBusinessesConfirmForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    CheckActiveBusinessesConfirmForm.response -> Seq(response.getOrElse("N/A"))
  )
}
