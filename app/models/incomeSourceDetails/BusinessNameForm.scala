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

package models.incomeSourceDetails

import play.api.data.Form
import play.api.data.Forms.{mapping, text}

import scala.util.matching.Regex

case class BusinessNameForm(name: String)


object BusinessNameForm {

  private val validBusinessName: Regex = "^[A-Za-z0-9 ,.&'\\\\/-]{1,105}$".r

  def validation(name: String): Option[BusinessNameForm] = {
    if (name.nonEmpty && name.length <= 105)
    {
      validBusinessName.findFirstMatchIn(name) match {
        case Some(_) => Option(BusinessNameForm(name))
        case _ => None
      }
    } else None
  }

  val form: Form[BusinessNameForm] = Form(mapping(
    "name" -> text
    //      .verifying("form.error.required", _.nonEmpty)
    //      .verifying("form.error.maxLength", _.length <= 105)
    //      .verifying("form.error.invalidNameFormat", _.matches("^[A-Za-z0-9 ,.&'\\\\/-]{1,105}$"))
  )(BusinessNameForm.apply)(BusinessNameForm.unapply).verifying(
    "Failed form constraints!",
    fields => fields match {
      case businessName => validation(businessName.name).isDefined
    }
  )
  )


}