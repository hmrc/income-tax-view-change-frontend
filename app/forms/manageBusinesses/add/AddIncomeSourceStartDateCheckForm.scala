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

import play.api.data.Form
import play.api.data.Forms._


object AddIncomeSourceStartDateCheckForm {

  val responseNo: String = "No"
  val responseYes: String = "Yes"
  val response: String = "start-date-check"


  def apply(messagesPrefix: String): Form[AddIncomeSourceStartDateCheckForm] = {

    val radiosEmptyError: String = s"$messagesPrefix.error"

    Form[AddIncomeSourceStartDateCheckForm](
      mapping(
        response -> optional(text)
          .verifying(radiosEmptyError, value => value.nonEmpty && (value.contains(responseYes) || value.contains(responseNo)))
      )(AddIncomeSourceStartDateCheckForm.apply)(AddIncomeSourceStartDateCheckForm.unapply)
    )
  }
}

case class AddIncomeSourceStartDateCheckForm(response: Option[String]) {
  def toFormMap: Map[String, Seq[String]] = Map(
    AddIncomeSourceStartDateCheckForm.response -> Seq(response.getOrElse("N/A"))
  )
}
