/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.incomeSources.add

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}

object BusinessAccountingMethodForm {

  val responseCashBasis: String = "incomeSources.add.business-accounting-method.radio-1-title"
  val responseTraditional: String = "incomeSources.add.business-accounting-method.radio-2-title"
  val response: String = "business-accounting-method"
  val radiosEmptyError: String = "incomeSources.add.business-accounting-method.no-selection"
  val csrfToken: String = "csrfToken"

  val form: Form[BusinessAccountingMethodForm] = Form[BusinessAccountingMethodForm](
    mapping(
      response -> optional(text)
        .verifying(radiosEmptyError, _.nonEmpty)
    )(BusinessAccountingMethodForm.apply)(BusinessAccountingMethodForm.unapply)
  )
}

case class BusinessAccountingMethodForm(response: Option[String]) {

  def toFormMap: Map[String, Seq[String]] = Map(
    BusinessAccountingMethodForm.response -> Seq(response.getOrElse("N/A"))
  )
}
