///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package forms.incomeSources.add
//
//import play.api.data.Form
//import play.api.data.Forms._
//
//
//object AddBusinessStartDateCheckForm {
//
//  val responseNo: String = "No"
//  val responseYes: String = "Yes"
//  val response: String = "start-date-check"
//  val radiosEmptyError: String = "add-business-start-date-check.radio.error"
//  val csrfToken: String = "csrfToken"
//
//  val form: Form[AddBusinessStartDateCheckForm] = Form[AddBusinessStartDateCheckForm](
//    mapping(
//      response -> optional(text)
//        .verifying(radiosEmptyError, _.nonEmpty)
//    )(AddBusinessStartDateCheckForm.apply)(AddBusinessStartDateCheckForm.unapply)
//  )
//}
//
//case class AddBusinessStartDateCheckForm(response: Option[String]) extends IncomeSourceStartDateCheckForm {
//  override def toFormMap: Map[String, Seq[String]] = Map(
//    AddBusinessStartDateCheckForm.response -> Seq(response.getOrElse("N/A"))
//  )
//}
