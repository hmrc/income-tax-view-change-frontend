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

package forms.manageBusinesses.add

import forms.validation.CustomConstraints
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation._

import scala.util.matching.Regex

case class BusinessNameForm(name: String)

object BusinessNameForm extends CustomConstraints {
  val businessName = "business-name"
  val MAX_LENGTH: Int = 105

  val permittedChars: Regex = "^[A-Za-z0-9 ,.&'\\\\/-]+$".r

  val businessNameEmptyError: String = "add-business-name.form.error.required"
  val businessNameLengthIncorrect: String = "add-business-name.form.error.maxLength"
  val businessNameInvalidChar: String = "add-business-name.form.error.invalidNameFormat"
  val businessNameInvalid: String = "add-business-name.form.error.invalidName"

  private val isValidChars: Constraint[String] = pattern(regex = permittedChars, error = businessNameInvalidChar)
  private val isNotTooLong: Constraint[String] = maxLength(MAX_LENGTH, businessNameLengthIncorrect)
  private val nonEmptyBusinessName: Constraint[String] = nonEmpty(errorMessage = businessNameEmptyError)

  val form: Form[BusinessNameForm] = Form(mapping(
    businessName.trim() -> text
      .verifying(firstError(nonEmptyBusinessName, isValidChars, isNotTooLong))
  )(BusinessNameForm.apply)(BusinessNameForm.unapply)
  )

  def checkBusinessNameWithTradeName(form: Form[BusinessNameForm], businessTradeName: Option[String]): Form[BusinessNameForm] = {
    businessTradeName match {
      case Some(tradeName) if !form.hasErrors && form.get.name.trim.toLowerCase.equals(tradeName.trim.toLowerCase) =>
        form.withError(businessName, businessNameInvalid)
      case _ =>
        form
    }
  }
}
