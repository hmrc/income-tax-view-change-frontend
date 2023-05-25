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

import auth.MtdItUser
import forms.utils.ConstraintUtil.ConstraintUtil
import forms.utils.SessionKeys
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation._

import scala.util.matching.Regex

/*object BusinessTradeForm{
  val businessTrade = "businessTrade"
}*/

class BusinessTradeForm() {

  private val validTrade: Regex = "^[A-Za-z0-9 ,.&'\\\\/-]+$".r

  val maxLength = 35

  val businessTrade = "business-trade"

  val tradeEmptyError = "add-business-trade.form.error.empty"
  val tradeShortError = "add-business-trade.form.error.short"
  val tradeLongError = "add-business-trade.form.error.long"
  val tradeInvalidCharError = "add-business-trade.form.error.invalid"
  val tradeSameNameError = "You cannot enter the same trade and same business name"

  val isValidLength: Constraint[String] = Constraint(value =>
    value.length match {
      case i: Int if i==0 => Invalid(tradeEmptyError)
      case i: Int if i<2 => Invalid(tradeShortError)
      case i: Int if i>maxLength => Invalid(tradeLongError)
      case _ => Valid
    }
  )

  val isValidChars: Constraint[String] = Constraint(value =>
    if (validTrade.pattern.matcher(value).matches()) Valid
    else Invalid(tradeInvalidCharError)
  )

  def isNotSameName(businessName: Option[String]): Constraint[String] = Constraint { value =>
    businessName match {
      case Some(name) => if (value.toLowerCase.trim == name.toLowerCase)
            Invalid(tradeSameNameError) else Valid
      case None => Valid
    }
  }


  def apply(implicit user: MtdItUser[_]): Form[String] = {
    val businessName: Option[String] = user.session.get(SessionKeys.businessName)
    Form[String](
      mapping("business-trade" -> text.verifying(isValidLength andThen isValidChars andThen isNotSameName(businessName))
      )(identity)(Some(_))
    )
  }
}
