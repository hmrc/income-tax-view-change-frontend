/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.adjustPoa

import forms.utils.ConstraintUtil.ConstraintUtil
import forms.validation.CustomConstraints
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages


case class EnterPoaAmountForm(amount: BigDecimal)

object EnterPoaAmountForm extends CustomConstraints{

  val amount: String = "poa-amount"

  val emptyError = "claimToAdjustPoa.enterPoaAmount.emptyError"
  val sameError = "claimToAdjustPoa.enterPoaAmount.sameError"
  val higherError = "claimToAdjustPoa.enterPoaAmount.higherError"
  private val invalidError = "claimToAdjustPoa.enterPoaAmount.invalidError"

  private val isZeroOrMore: Constraint[BigDecimal] = min[BigDecimal](0, errorMessage = invalidError)
  private val isNumber: Constraint[BigDecimal] = Constraint{value => if(value.isValidLong) Valid else Invalid(invalidError) } //NOPE
  private val isValidNumber = isNumber andThen isZeroOrMore

  val form: Form[EnterPoaAmountForm] = Form(
    mapping(
      amount -> bigDecimal
        .verifying(
          firstError(isValidNumber))
    )(EnterPoaAmountForm.apply)(EnterPoaAmountForm.unapply)
  )

  def checkValueConstraints(form: Form[EnterPoaAmountForm], totalAmount: BigDecimal, relevantAmount: BigDecimal)(implicit messages: Messages): Form[EnterPoaAmountForm] = {
    if (form.get.amount == totalAmount) {
      form.withError(EnterPoaAmountForm.amount, messages(sameError, totalAmount.toCurrencyString))
    }
    else if (form.get.amount > relevantAmount) {
      form.withError(EnterPoaAmountForm.amount, messages(higherError, relevantAmount.toCurrencyString))
    } else form
  }

}
