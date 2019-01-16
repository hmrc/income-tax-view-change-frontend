/*
 * Copyright 2019 HM Revenue & Customs
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

package testOnly.forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import testOnly.forms.validation.Constraints._
import testOnly.models.DataModel

object StubDataForm {

  val url = "_id"
  val schemaName = "schemaId"
  val method = "method"
  val status = "status"
  val response = "response"

  val stubDataForm = Form(
    mapping(
      url -> text.verifying(nonEmpty("You must supply the URL to mock the response for")),
      schemaName -> text.verifying(nonEmpty("You must supply a Schema Name to validate against")),
      method -> text.verifying(nonEmpty("You must specify the Http Method of the request being stubbed")),
      status -> text.verifying(isNumeric).transform[Int](_.toInt,_.toString),
      response -> optional(text).verifying(oValidJson).transform[Option[JsValue]](x => x.fold[Option[JsValue]](None)(value => Some(Json.parse(value))),
        y => y.fold[Option[String]](None)(json => Some(json.toString())))
    )(DataModel.apply)(DataModel.unapply)
  )
}
