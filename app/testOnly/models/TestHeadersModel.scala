/*
 * Copyright 2020 HM Revenue & Customs
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

package testOnly.models

import play.api.libs.json.{Json, OFormat}

case class TestHeadersModel(headerName: String)

object TestHeadersModel {
  implicit val formats: OFormat[TestHeadersModel] = Json.format[TestHeadersModel]

  val validTestHeaders: Map[String, String] = Map(
    "ITVC_DEFAULT" -> "Default",
    "ITVC_SCOTLAND_DEFAULT" -> "Scottish Default",
    "UK_MULTIPLE_INCOMES_EXAMPLE" -> "Multiple Incomes",
    "UK_PROP_DIVIDENDS_EXAMPLE" -> "Property with Dividends",
    "UK_PROP_SAVINGS_EXAMPLE" -> "Property with Savings",
    "SCOT_SE_DIVIDENDS_EXAMPLE" -> "Scottish Self Employed with Dividends",
    "UK_SE_SAVINGS_EXAMPLE" -> "Self Employed with Savings",
    "ERROR_MESSAGES_EXIST" -> "Failed Calculation",
    "NOT_FOUND" -> "Calculation Not Found",
    "ITVC_CRYSTALLISATION_METADATA" -> "Crystallised Calculation"
  )

  val validCalcIdHeaders: Seq[String] = Seq("DEFAULT", "NOT_FOUND")
}
