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

import forms.utils.ConstraintUtil.ConstraintUtil
import forms.validation.CustomConstraints
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation._

import scala.util.matching.Regex

case class BusinessTradeForm(trade: String)

object BusinessTradeForm extends CustomConstraints {

  val businessTrade = "business-trade"
  val MIN_LENGTH = 2
  val MAX_LENGTH = 35
  val permittedChars: Regex = "^[A-Za-z0-9 ,.&'\\\\/-]+$".r

  val tradeEmptyError = "add-business-trade.form.error.empty"
  val tradeShortError = "add-business-trade.form.error.short"
  val tradeLongError = "add-business-trade.form.error.long"
  val tradeInvalidCharError = "add-business-trade.form.error.invalid"
  val tradeSameNameError = "You cannot enter the same trade and same business name"

  private val isNotTooLong: Constraint[String] = maxLength(MAX_LENGTH, tradeLongError)
  private val isNotTooShort: Constraint[String] = minLength(MIN_LENGTH, tradeShortError)
  private val isNotEmpty: Constraint[String] = nonEmpty(errorMessage = tradeEmptyError)
  private val isValidChars: Constraint[String] = pattern(regex = permittedChars, error = tradeInvalidCharError)

  val form: Form[BusinessTradeForm] = Form(
    mapping(
      businessTrade -> text
        .verifying(
          firstError(isNotEmpty andThen isNotTooLong andThen isNotTooShort andThen isValidChars))
    )(BusinessTradeForm.apply)(BusinessTradeForm.unapply)
  )
}
