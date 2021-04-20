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

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testNino}
import models.core.{Nino, NinoResponseError}
import play.api.libs.json.{JsValue, Json}

object NinoLookupTestConstants {
  val testNinoModel: Nino = Nino(nino = testNino)
  val testNinoModelJson: JsValue = Json.obj(
    "nino" -> testNino
  )

  val testNinoErrorModel: NinoResponseError = NinoResponseError(testErrorStatus, testErrorMessage)
  val testNinoErrorModelJson: JsValue = Json.obj(
    "status" -> testErrorStatus,
    "reason" -> testErrorMessage
  )
}
