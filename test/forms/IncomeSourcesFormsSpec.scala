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


import forms.incomeSources.add.BusinessTradeForm
import generators.IncomeSourceGens.{businessNameGenerator, businessTradeGenerator}
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Gen, Properties}
import play.api.data.Form

object IncomeSourcesFormsSpec  extends Properties("incomeSourcesForms.validation") {

  val businessNameForm = (optValue: Option[String]) => BusinessNameForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map(BusinessNameForm.bnf -> value))
  )

  val businessTradeForm = (optValue: Option[String]) => BusinessTradeForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("businessTrade" -> value))
  )

  property("businessName.validation") = forAll(businessNameGenerator) { (charsList: List[Char]) =>
    (charsList.length > 0 && charsList.length <= BusinessNameForm.businessNameLength) ==> {
      val businessName = charsList.mkString("")
      //println(s"Generate business name: ${businessName}")
      businessNameForm(Some(businessName)).errors.isEmpty
    }
  }

  property("businessTrade.validation") = forAll(businessTradeGenerator) { (charsList: List[Char]) =>
    val businessTrade = charsList.mkString("").trim
    (businessTrade.length > 2 && businessTrade.length <= BusinessTradeForm.maxLength) ==> {
      println(s"Generate business trade: ${businessTrade}")
      businessTradeForm(Some(businessTrade)).errors.isEmpty
    }
  }

}