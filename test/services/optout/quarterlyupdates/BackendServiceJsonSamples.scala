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

package services.optout.quarterlyupdates

import models.nextUpdates.ObligationStatus.Fulfilled
import play.api.libs.json.{JsValue, Json}

object BackendServiceJsonSamples {

  val testSelfEmploymentId = "XA00001234"

  val nextUpdateOverdueJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-30",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#002",
    "status" -> Fulfilled
  )

  val nextUpdateOpenJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-31",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#003",
    "status" -> Fulfilled
  )

  val nextUpdatesDataFromJson: JsValue = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      nextUpdateOverdueJson,
      nextUpdateOpenJson
    )
  )

  val obligationsDataFromJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      nextUpdatesDataFromJson
    )
  )

}