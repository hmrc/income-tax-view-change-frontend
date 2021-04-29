/*
 * Copyright 2021 HM Revenue & Customs
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

package assets

import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import play.api.libs.json.{JsValue, Json}
import BaseTestConstants._

object CitizenDetailsTestConstants {
  val testValidCitizenDetailsModelJson: JsValue = Json.obj(
    "name" -> Json.obj(
      "current" -> Json.obj(
        "firstName" -> "John",
        "lastName" -> "Smith"),
      "previous" -> ""
    ),
    "ids" -> Json.obj("nino" -> "AA055075C"),
    "dateOfBirth" -> "11121971"
  )

  val testValidCitizenDetailsModel: CitizenDetailsModel = CitizenDetailsModel(Some("John"), Some("Smith"), Some("AA055075C"))

  val testInvalidCitizenDetailsJson: JsValue = Json.obj(
    "ids" -> Json.obj("nino" -> 123.4)
  )

  val testCitizenDetailsErrorModelParsing: CitizenDetailsErrorModel = CitizenDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing Citizen Details Data Response")

  val testCitizenDetailsErrorModel: CitizenDetailsErrorModel = CitizenDetailsErrorModel(testErrorStatus, testErrorMessage)
  val testCitizenDetailsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )
}
