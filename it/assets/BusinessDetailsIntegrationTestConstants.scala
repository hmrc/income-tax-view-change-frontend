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
import assets.BaseIntegrationTestConstants.{otherTestSelfEmploymentId, testSelfEmploymentId}
import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import models.incomeSourceDetails.BusinessDetailsModel
import utils.ImplicitDateFormatter._

object BusinessDetailsIntegrationTestConstants {

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = "2017-01-01",
      end = "2017-12-31"
    ),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-01-01"),
    cessation = Some(CessationModel(
      date = Some("2017-12-31"),
      reason = Some("It really, really was a bad idea")
    )),
    tradingName = Some("business"),
    address = Some(AddressModel(
      addressLine1 = "64 Zoo Lane",
      addressLine2 = Some("Happy Place"),
      addressLine3 = Some("Magical Land"),
      addressLine4 = Some("England"),
      postCode = Some("ZL1 064"),
      countryCode = "UK"
    )),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = "2018-01-01",
      end = "2018-12-31"
    ),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-01-01"),
    cessation = Some(CessationModel(
      date = Some("2017-12-31"),
      reason = Some("It really, really was a bad idea")
    )),
    tradingName = Some("secondBusiness"),
    address = Some(AddressModel(
      addressLine1 = "742 Evergreen Terrace",
      addressLine2 = Some("Springfield"),
      addressLine3 = Some("Oregon"),
      addressLine4 = Some("USA"),
      postCode = Some("51MP 50N5"),
      countryCode = "USA"
    )),
    contactDetails = None,
    seasonal = None,
    paperless = None
  )

  val businessSuccessResponse: JsValue =
    Json.arr(
      Json.obj(
        "id" -> testSelfEmploymentId,
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

  val otherSuccessResponse: JsValue =
    Json.arr(
      Json.obj(
        "id" -> otherTestSelfEmploymentId,
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


  val multipleSuccessResponse: JsValue =
    Json.arr(
      Json.obj(
        "id" -> testSelfEmploymentId,
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
        "id" -> otherTestSelfEmploymentId,
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

  val emptyBusinessDetailsResponse: JsValue = JsArray()

  def businessFailureResponse(code: String, reason: String): JsValue =
    Json.obj(
      "code" -> code,
      "reason" -> reason
    )

}