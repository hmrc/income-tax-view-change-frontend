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

package forms.incomeSources.manage

import play.api.data.Form
import play.api.data.Forms._

object ConfirmReportingMethodForm {

  val confirmReportingMethod: String = "incomeSources.manage.propertyReportingMethod"

  val form: Form[ConfirmReportingMethodForm] = Form(
    mapping(
      confirmReportingMethod -> boolean.verifying("incomeSources.manage.propertyReportingMethod.error", _ == true)
    )(ConfirmReportingMethodForm.apply)(ConfirmReportingMethodForm.unapply)
  )
}

case class ConfirmReportingMethodForm(confirmReportingMethod: Boolean)