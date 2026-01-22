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

import forms.mappings.Mappings
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages


case class EnterPoaAmountForm(amount: BigDecimal)

object EnterPoaAmountForm extends Mappings {

  val amount: String = "poa-amount"

  private val emptyErrorMessageKey = "claimToAdjustPoa.enterPoaAmount.emptyError"
  private val sameErrorMessageKey = "claimToAdjustPoa.enterPoaAmount.sameError"
  private val higherErrorMessageKey = "claimToAdjustPoa.enterPoaAmount.higherError"
  private val invalidErrorMessageKey = "claimToAdjustPoa.enterPoaAmount.invalidError"

  val form: Form[EnterPoaAmountForm] = Form(
    mapping(
      amount -> currency(emptyErrorMessageKey, invalidErrorMessageKey)
    )(
      amount => EnterPoaAmountForm(amount)
    )(
      form => Some(form.amount)
    )
  )

  def checkValueConstraints(form: Form[EnterPoaAmountForm], totalAmount: BigDecimal, relevantAmount: BigDecimal)
                           (implicit messages: Messages): Form[EnterPoaAmountForm] = {
    if (form.hasErrors) form else {
      if (form.get.amount == totalAmount) {
        form.withError(EnterPoaAmountForm.amount, messages(sameErrorMessageKey, totalAmount.toCurrencyString))
      }
      else if (form.get.amount >= relevantAmount) {
        form.withError(EnterPoaAmountForm.amount, messages(higherErrorMessageKey, relevantAmount.toCurrencyString))
      } else form
    }
  }

}
