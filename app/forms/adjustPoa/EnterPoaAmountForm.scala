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

import forms.validation.CustomConstraints
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages

import scala.util.control.Exception.nonFatalCatch


case class EnterPoaAmountForm(amount: BigDecimal)

object EnterPoaAmountForm extends CustomConstraints{

  val amount: String = "poa-amount"

  private val emptyError = "claimToAdjustPoa.enterPoaAmount.emptyError"
  private val sameError = "claimToAdjustPoa.enterPoaAmount.sameError"
  private val higherError = "claimToAdjustPoa.enterPoaAmount.higherError"
  private val invalidError = "claimToAdjustPoa.enterPoaAmount.invalidError"

  val form: Form[EnterPoaAmountForm] = Form(
    mapping(
      amount -> of(currencyFormatter())
    )(EnterPoaAmountForm.apply)(EnterPoaAmountForm.unapply)
  )

  def checkValueConstraints(form: Form[EnterPoaAmountForm], totalAmount: BigDecimal, relevantAmount: BigDecimal)(implicit messages: Messages): Form[EnterPoaAmountForm] = {
    if (form.hasErrors) form else {
      if (form.get.amount == totalAmount) {
        form.withError(EnterPoaAmountForm.amount, messages(sameError, totalAmount.toCurrencyString))
      }
      else if (form.get.amount >= relevantAmount) {
        form.withError(EnterPoaAmountForm.amount, messages(higherError, relevantAmount.toCurrencyString))
      } else form
    }
  }

  private def currencyFormatter(): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {

      val is2dp = """\d+|\d*\.\d{1,2}"""
      val validNumeric = """[0-9.]*"""

      private val baseFormatter = stringFormatter(emptyError)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .map(_.replace("£", ""))
          .flatMap {
            case s if s.isEmpty =>
              Left(Seq(FormError(key, emptyError)))
            case s if !s.matches(validNumeric) || !s.matches(is2dp) =>
              Left(Seq(FormError(key, invalidError)))
            case s =>
                nonFatalCatch
                  .either(BigDecimal(s))
                  .left.map(_ => Seq(FormError(key, invalidError)))
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private def stringFormatter(errorKey: String): Formatter[String] = new Formatter[String] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
      data.get(key) match {
        case None => Left(Seq(FormError(key, errorKey)))
        case Some(s) => Right(s.trim)
      }

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value.trim)
  }

}
