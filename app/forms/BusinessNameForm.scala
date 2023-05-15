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

package forms

import forms.utils.ConstraintUtil.ConstraintUtil
import forms.utils.SessionKeys
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation._

import scala.util.matching.Regex

case class BusinessNameForm(name: String)

object BusinessNameForm {

  private val validBusinessName: Regex = "^[A-Za-z0-9 ,.&'\\\\/-]+$".r
  val businessNameLength: Int = 105
  val bnf: String = "addBusinessName"
  val invalidName: String = "Invalid Name Characters £££"

  val businessNameEmptyError: String = "add-business-name.form.error.required"
  val businessNameLengthIncorrect: String = "add-business-name.form.error.maxLength"
  val businessNameInvalidChar: String = "add-business-name.form.error.invalidNameFormat"

  val containsValidChar: Constraint[String] = Constraint(value =>
    if (validBusinessName.pattern.matcher(value).matches()) {
      Valid
    } else {
      Invalid(businessNameInvalidChar)
    }
  )

  val isValidNameLength: Constraint[String] = Constraint(value =>
    if (value.length <= businessNameLength) {
      Valid
    } else {
      Invalid(businessNameLengthIncorrect)
    }
  )

  val nonEmptyBusinessName: Constraint[String] = Constraint( value =>
    if (value.isEmpty) Invalid(businessNameEmptyError) else Valid
  )

  val form: Form[BusinessNameForm] = Form(mapping(
    SessionKeys.businessName -> text.verifying(nonEmptyBusinessName andThen containsValidChar andThen isValidNameLength)
  )(BusinessNameForm.apply)(BusinessNameForm.unapply)
  )
}


