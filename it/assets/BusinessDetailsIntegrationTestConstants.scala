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

package assets

import play.api.libs.json.{JsArray, JsValue, Json}

object BusinessDetailsIntegrationTestConstants {
  def businessSuccessResponse(selfEmploymentId: String): JsValue =
    Json.arr(
      Json.obj(
        "id" -> selfEmploymentId,
        "accountingPeriod" -> Json.obj(
          "start" -> "2017-01-01",
          "end" -> "2017-12-31"
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2017-01-01",
        "cessationDate" -> "2017-12-31",
        "tradingName" -> "business",
        "businessDescription" -> "a business",
        "businessAddressLineOne" -> "64 Zoo Lane",
        "businessAddressLineTwo" -> "Happy Place",
        "businessAddressLineThree" -> "Magical Land",
        "businessAddressLineFour" -> "England",
        "businessPostcode" -> "ZL1 064"
      )
    )

  def otherSuccessResponse(selfEmploymentId: String): JsValue =
    Json.arr(
      Json.obj(
        "id"  ->  selfEmploymentId,
        "accountingPeriod" -> Json.obj(
          "start" -> "2018-01-01",
          "end" -> "2018-12-31"
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2018-01-01",
        "cessationDate" -> "2018-12-31",
        "tradingName" -> "business",
        "businessDescription" -> "a business",
        "businessAddressLineOne" -> "64 Zoo Lane",
        "businessAddressLineTwo" -> "Happy Place",
        "businessAddressLineThree" -> "Magical Land",
        "businessAddressLineFour" -> "England",
        "businessPostcode" -> "ZL1 064"
      )
    )


  def multipleSuccessResponse(id1: String, id2: String): JsValue =
    Json.arr(
      Json.obj(
        "id" -> id1,
        "accountingPeriod" -> Json.obj(
          "start" -> "2017-01-01",
          "end" -> "2017-12-31"
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2017-01-06",
        "cessationDate" -> "2017-12-31",
        "tradingName" -> "firstBusiness",
        "businessDescription" -> "a first business",
        "businessAddressLineOne" -> "64 Zoo Lane",
        "businessAddressLineTwo" -> "Happy Place",
        "businessAddressLineThree" -> "Magical Land",
        "businessAddressLineFour" -> "England",
        "businessPostcode" -> "ZL1 064"
      ),
      Json.obj(
        "id" -> id2,
        "accountingPeriod" -> Json.obj(
          "start" -> "2018-01-01",
          "end" -> "2018-12-31"
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2017-01-01",
        "cessationDate" -> "2017-12-31",
        "tradingName" -> "secondBusiness",
        "businessDescription" -> "a second business",
        "businessAddressLineOne" -> "742 Evergreen Terrace",
        "businessAddressLineTwo" -> "Springfield",
        "businessAddressLineThree" -> "Oregon",
        "businessAddressLineFour" -> "USA",
        "businessPostcode" -> "51MP 50N5"
      )
    )

  def emptyBusinessDetailsResponse(): JsValue = JsArray()

  def businessFailureResponse(code: String, reason: String): JsValue =
    Json.obj(
      "code" -> code,
      "reason" -> reason
    )

}