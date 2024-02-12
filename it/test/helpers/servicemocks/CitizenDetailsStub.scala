/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import helpers.WiremockHelper
import play.api.libs.json.{JsValue, Json}

object CitizenDetailsStub {

  def citizenDetailsUrl(utr: String): String = s"/citizen-details/sautr/$utr"

  //Financial Transactions
  def stubGetCitizenDetails(utr: String)(status: Int, response: JsValue): Unit =
    WiremockHelper.stubGet(citizenDetailsUrl(utr), status, response.toString())

  //Verifications
  def verifyGetCitizenDetails(utr: String): Unit =
    WiremockHelper.verifyGet(citizenDetailsUrl(utr))

  def validCitizenDetailsResponse(firstName: String, lastName: String, nino: String): JsValue = Json.obj(
    "name" -> Json.obj(
      "current" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName
      )
    ),
    "ids" -> Json.obj(
      "nino" -> nino
    )
  )
}
