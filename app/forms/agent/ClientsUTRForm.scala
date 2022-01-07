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

package forms.agent

import forms.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.Forms.{default, single, text}
import play.api.data.validation.Constraints.nonEmpty
import play.api.data.validation.{Constraint, Invalid, Valid}

object ClientsUTRForm {

  val utr: String = "utr"
  val utrLength: Int = 10

  val utrEmptyError: String = "agent.error.enter_clients_utr.empty"
  val utrLengthIncorrect: String = "agent.error.enter_clients_utr.length"
  val utrNonNumeric: String = "agent.error.enter_clients_utr.non_numeric"

  val containsOnlyNumbers: Constraint[String] = Constraint(value =>
    if (value.forall(_.isDigit)) {
      Valid
    } else {
      Invalid(utrNonNumeric)
    }
  )

  val isValidUTRLength: Constraint[String] = Constraint(value =>
    if (value.length == utrLength) {
      Valid
    } else {
      Invalid(utrLengthIncorrect)
    }
  )

  val nonEmptyUTR: Constraint[String] = nonEmpty(utrEmptyError)

  val form: Form[String] = Form[String](
    single(
      utr -> default(text, "")
        .transform[String](_.replaceAll("\\s", ""), identity)
        .verifying(
        nonEmptyUTR andThen containsOnlyNumbers andThen isValidUTRLength
      )
    )
  )

}
