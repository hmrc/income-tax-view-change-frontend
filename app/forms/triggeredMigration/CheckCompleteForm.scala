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

object CheckCompleteForm {

  val responseContinue: String  = "Continue"
  val response: String    = "check-complete-confirm"
  val errorKey = "triggered-migration.check-active-businesses-confirm.error.required"

  def apply(): Form[CheckCompleteForm] = {
    Form[CheckCompleteForm](
      mapping(
        response -> optional(text)
          .verifying(errorKey, value =>
            value.nonEmpty && value.contains(responseContinue)
          )
      )(CheckCompleteForm.apply)(CheckCompleteForm.unapply)
    )
  }
}

case class CheckCompleteForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    CheckCompleteForm.response -> Seq(response.getOrElse("N/A"))
  )
}
