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

import auth.MtdItUser
import models._
import play.api.http.Status
import play.api.libs.json.Json
import utils.ImplicitDateFormatter

object TestConstants extends ImplicitDateFormatter {

  val testMtditid = "XAIT0000123456"
  val testNino = "AB123456C"
  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino)
  val testSelfEmploymentId = "XA00001234"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorMessage = "Dummy Error Message"

  object BusinessDetails {

    val business1 = BusinessModel(
      id = testSelfEmploymentId,
      accountingPeriod = AccountingPeriodModel(start ="2017-04-06", end = "2018-04-05"),
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = Some("2017-12-31"),
      tradingName = "Test Business",
      businessDescription = Some("a business"),
      businessAddressLineOne = Some("64 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )
    val business2 = BusinessModel(
      id = "5678",
      accountingPeriod = AccountingPeriodModel(start = "2017-1-1", end = "2017-12-31"),
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = Some("2017-12-31"),
      tradingName = "otherBusiness",
      businessDescription = Some("some business"),
      businessAddressLineOne = Some("65 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )

    val businessesSuccessResponse = List(business1, business2)
    val businessesSuccessModel = BusinessDetailsModel(businessesSuccessResponse)
    val businessSuccessEmptyModel = BusinessDetailsModel(List.empty)
    val businessSuccessString: String =
      s"""
          {
             "business":[
                {
                   "id":"$testSelfEmploymentId",
                   "accountingPeriod":{
                                        "start":"2017-04-06",
                                        "end":"2018-04-05"
                                      },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "cessationDate":"2017-12-31",
                   "tradingName":"Test Business",
                   "businessDescription":"a business",
                   "businessAddressLineOne":"64 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                },
                {
                   "id":"5678",
                   "accountingPeriod":{
                      "start":"2017-01-01",
                      "end":"2017-12-31"
                   },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "cessationDate":"2017-12-31",
                   "tradingName":"otherBusiness",
                   "businessDescription":"some business",
                   "businessAddressLineOne":"65 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                }
             ]
          }
      """.stripMargin
    val businessSuccessJson = Json.parse(businessSuccessString)


    val businessErrorModel = BusinessDetailsErrorModel(testErrorStatus, testErrorMessage)
    val businessErrorString =
      s"""
        |{
        |  "code":$testErrorStatus,
        |  "message":"$testErrorMessage"
        |}
      """.stripMargin
    val businessListErrorJson = Json.parse(businessErrorString)
  }

  object PropertyDetails {
    val propertySuccessModel = PropertyDetailsModel(AccountingPeriodModel("2017-04-06", "2018-04-05"))
    val propertyErrorModel = PropertyDetailsErrorModel(testErrorStatus, testErrorMessage)
  }

  object Estimates {

    val testYear = 2018
    val testCalcType = "it"

    val lastTaxCalcSuccess = LastTaxCalculation(
      calcID = "01234567",
      calcTimestamp = "2017-07-06T12:34:56.789Z",
      calcAmount = 543.21
    )

    val lastTaxCalcError = LastTaxCalculationError(testErrorStatus, testErrorMessage)
  }

  object Obligations {

    def fakeObligationsModel(m: ObligationModel): ObligationModel = new ObligationModel(m.start,m.end,m.due,m.met) {
      override def currentTime() = "2017-10-31"
    }

    val receivedObligation = fakeObligationsModel(ObligationModel(
      start = "2017-04-01",
      end = "2017-6-30",
      due = "2017-7-31",
      met = true
    ))

    val overdueObligation = fakeObligationsModel(ObligationModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-30",
      met = false
    ))

    val openObligation = fakeObligationsModel(ObligationModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-31",
      met = false
    ))

    val obligationsDataSuccessModel = ObligationsModel(List(receivedObligation, overdueObligation, openObligation))
    val obligationsDataSuccessString =
      """
        |{
        |  "obligations": [
        |    {
        |      "start": "2017-04-01",
        |      "end": "2017-06-30",
        |      "due": "2017-07-31",
        |      "met": true
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-30",
        |      "met": false
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-31",
        |      "met": false
        |    }
        |  ]
        |}
      """.stripMargin
    val obligationsDataSuccessJson = Json.parse(obligationsDataSuccessString)

    val obligationsDataErrorModel = ObligationsErrorModel(testErrorStatus, testErrorMessage)
    val obligationsDataErrorString =
      s"""
        |{
        |  "code":$testErrorStatus,
        |  "message":"$testErrorMessage"
        |}
      """.stripMargin
    val obligationsDataErrorJson = Json.parse(obligationsDataErrorString)

  }

  object IncomeSourceDetails {

    //Constructors
    val accountingPeriodModel = AccountingPeriodModel("2017-04-06", "2018-04-05")
    val businessIncomeModel =
      BusinessIncomeModel(
        selfEmploymentId = testSelfEmploymentId,
        accountingPeriod = accountingPeriodModel,
        tradingName = "Test Business"
      )
    val propertyIncomeModel = PropertyIncomeModel(AccountingPeriodModel("2017-04-06", "2018-04-05"))

    //Outputs
    val bothIncomeSourceSuccess = IncomeSourceModel(Some(businessIncomeModel), Some(propertyIncomeModel))
    val businessIncomeSourceSuccess = IncomeSourceModel(Some(businessIncomeModel),None)
    val propertyIncomeSourceSuccess = IncomeSourceModel(None,Some(propertyIncomeModel))
    val noIncomeSourceSuccess = IncomeSourceModel(None, None)

  }
}
