/*
 * Copyright 2018 HM Revenue & Customs
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
import testOnly.models.SchemaModel
import testOnly.forms.validation.Constraints._
import testOnly.forms.validation.utils.ConstraintUtil._

object StubSchemaForm {

  val id = "_id"
  val url = "url"
  val method = "method"
  val responseSchema = "responseSchema"

  val stubSchemaForm = Form(
    mapping(
      id -> text.verifying(nonEmpty("Schema Name is Mandatory")),
      url -> text.verifying(nonEmpty("URL Regex is Mandatory")),
      method -> text.verifying(nonEmpty("Method is Mandatory")),
      responseSchema -> text.verifying(nonEmpty("Schema Definition is Mandatory") andThen validJson).transform[JsValue](s => Json.parse(s), j => j.toString())
    )(SchemaModel.apply)(SchemaModel.unapply)
  )
}
