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

package assets

import assets.BaseTestConstants._
import assets.ReportDeadlinesTestConstants._
import models._
import play.api.libs.json.{JsValue, Json}

object BusinessDetailsTestConstants {

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = "2017-6-1", end = "2018-5-30")
  val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = "2017-3-5", end = "2018-3-6")
  val testTradeName = "business"
  val testTradeName2 = "business"

  val business1 = BusinessModel(
    id = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    accountingType = "CASH",
    commencementDate = Some("2017-1-1"),
    cessationDate = None,
    tradingName = testTradeName,
    businessDescription = Some("a business"),
    businessAddressLineOne = Some("64 Zoo Lane"),
    businessAddressLineTwo = Some("Happy Place"),
    businessAddressLineThree = Some("Magical Land"),
    businessAddressLineFour = Some("England"),
    businessPostcode = Some("ZL1 064")
  )
  val business2 = BusinessModel(
    id = testSelfEmploymentId2,
    accountingPeriod = testBusinessAccountingPeriod,
    accountingType = "CASH",
    commencementDate = Some("2017-1-1"),
    cessationDate = None,
    tradingName = testTradeName2,
    businessDescription = Some("some business"),
    businessAddressLineOne = Some("65 Zoo Lane"),
    businessAddressLineTwo = Some("Happy Place"),
    businessAddressLineThree = Some("Magical Land"),
    businessAddressLineFour = Some("England"),
    businessPostcode = Some("ZL1 064")
  )
  val ceasedBusiness = BusinessModel(
    id = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    accountingType = "CASH",
    commencementDate = Some("2017-1-1"),
    cessationDate = Some("2018-5-30"),
    tradingName = testTradeName,
    businessDescription = Some("a business"),
    businessAddressLineOne = Some("64 Zoo Lane"),
    businessAddressLineTwo = Some("Happy Place"),
    businessAddressLineThree = Some("Magical Land"),
    businessAddressLineFour = Some("England"),
    businessPostcode = Some("ZL1 064")
  )

  val businessesSuccessResponse = List(business1)
  val multipleBusinessSuccessResponse = List(business1, business2)
  val noBusinessDetails = BusinessDetailsModel(List())
  val businessSuccessEmptyResponse = "[]"
  val businessesSuccessModel = BusinessDetailsModel(businessesSuccessResponse)
  val multipleBusinessesSuccessModel = BusinessDetailsModel(multipleBusinessSuccessResponse)

  val businessSuccessJson: JsValue = Json.obj(

    "businesses" -> Json.arr(
      Json.obj(
        "id" -> testSelfEmploymentId,
        "accountingPeriod" -> Json.obj(
          "start" -> testBusinessAccountingPeriod.start,
          "end" -> testBusinessAccountingPeriod.end
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2017-01-01",
        "tradingName" -> testTradeName,
        "businessDescription" -> "a business",
        "businessAddressLineOne" -> "64 Zoo Lane",
        "businessAddressLineTwo" -> "Happy Place",
        "businessAddressLineThree" -> "Magical Land",
        "businessAddressLineFour" -> "England",
        "businessPostcode" -> "ZL1 064"
      ),
      Json.obj(
        "id" -> testSelfEmploymentId2,
        "accountingPeriod" -> Json.obj(
          "start" -> testBusinessAccountingPeriod.start,
          "end" -> testBusinessAccountingPeriod.end
        ),
        "accountingType" -> "CASH",
        "commencementDate" -> "2017-01-01",
        "tradingName" -> testTradeName2,
        "businessDescription" -> "some business",
        "businessAddressLineOne" -> "65 Zoo Lane",
        "businessAddressLineTwo" -> "Happy Place",
        "businessAddressLineThree" -> "Magical Land",
        "businessAddressLineFour" -> "England",
        "businessPostcode" -> "ZL1 064"
      )
    )
  )


  val businessErrorModel = BusinessDetailsErrorModel(testErrorStatus, testErrorMessage)
  val businessErrorJson: JsValue =Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val businessIncomeModel =
    BusinessIncomeModel(
      testSelfEmploymentId,
      testTradeName,
      None,
      testBusinessAccountingPeriod,
      obligationsDataSuccessModel
    )

  val businessIncomeModel2 =
    BusinessIncomeModel(
      testSelfEmploymentId2,
      testTradeName2,
      None,
      testBusinessAccountingPeriod,
      obligationsDataSuccessModel
    )

  val business2018IncomeModel =
    BusinessIncomeModel(
      testSelfEmploymentId,
      testTradeName,
      None,
      test2018BusinessAccountingPeriod,
      obligationsDataSuccessModel
    )

  val businessIncomeModelAlignedTaxYear =
    BusinessIncomeModel(
      testSelfEmploymentId,
      testTradeName,
      None,
      AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5"),
      obligationsDataSuccessModel
    )

  val business2019IncomeModel =
    BusinessIncomeModel(
      testSelfEmploymentId,
      testTradeName,
      None,
      AccountingPeriodModel(start = "2018-3-5", end = "2019-3-6"),
      obligationsDataSuccessModel
    )
}