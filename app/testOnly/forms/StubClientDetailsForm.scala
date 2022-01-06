/*
 * Copyright 2022 HM Revenue & Customs
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

package testOnly.forms

import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Mapping}
import testOnly.forms.validation.utils.ConstraintUtil.constraint
import testOnly.models.StubClientDetailsModel

object StubClientDetailsForm {

  val ninoKey: String = "nino"
  val utrKey: String = "utr"
  val statusKey: String = "status"

  val isNumberConstraint: Constraint[String] = constraint[String] { text =>
    try {
      text.toInt
      Valid
    } catch {
      case _: Exception => Invalid("Invalid status")
    }
  }

  def nonEmptyText(msg: String): Mapping[String] = default(text, "").verifying(msg, _.nonEmpty)

  val clientDetailsForm: Form[StubClientDetailsModel] = Form(
    mapping(
      ninoKey -> nonEmptyText("Must have an nino"),
      utrKey -> nonEmptyText("Must have a utr"),
      statusKey -> nonEmptyText("Must have a status").verifying(isNumberConstraint).transform[Int](_.toInt, _.toString)
    )(StubClientDetailsModel.apply)(StubClientDetailsModel.unapply)
  )

}
