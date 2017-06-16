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

import models._
import play.api.http.Status
import utils.ImplicitDateFormatter

object TestConstants extends ImplicitDateFormatter {

  val testMtditid = "XAIT0000123456"
  val testNino = "AB123456C"
  val testSelfEmploymentId = "XA00001234"

  object BusinessDetails {

    val jsonString =
      s"""
        {
          "business":[
            {
              "id":"$testSelfEmploymentId",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            },
            {
              "id":"5678",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
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
      """.stripMargin.split("\\s{2,}").mkString

    val business1 = BusinessModel(
      id = testSelfEmploymentId,
      accountingPeriod = AccountingPeriod(start = localDate("2017-1-1"), end = localDate("2017-12-31")),
      accountingType = "CASH",
      commencementDate = Some(localDate("2017-1-1")),
      cessationDate = Some(localDate("2017-12-31")),
      tradingName = "business",
      businessDescription = Some("a business"),
      businessAddressLineOne = Some("64 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )
    val business2 = BusinessModel(
      id = "5678",
      accountingPeriod = AccountingPeriod(start = localDate("2017-1-1"), end = localDate("2017-12-31")),
      accountingType = "CASH",
      commencementDate = Some(localDate("2017-1-1")),
      cessationDate = Some(localDate("2017-12-31")),
      tradingName = "otherBusiness",
      businessDescription = Some("some business"),
      businessAddressLineOne = Some("65 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )

    val businesses = BusinessListModel(List(business1, business2))

  }

  object Estimates {

    val lastTaxCalcSuccess = LastTaxCalculation(
      calcId = "01234567",
      calcTimestamp = "2017-07-06 12:34:56.789",
      calcAmount = 543.21
    )

    val lastTaxCalcError = LastTaxCalculationError(
      Status.INTERNAL_SERVER_ERROR,
      "Error Message"
    )
  }

  object Obligations {

    val obligation1: ObligationModel = ObligationModel(
      start = "2017-04-06",
      end = "2017-07-05",
      due = "2017-08-05",
      met = true
    )

    val obligationsDataResponse: ObligationsModel = ObligationsModel(
      List(obligation1)
    )

    val noObligationsErrorResponse = ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")

    val invalidObligationModel = ObligationModel(start = "2017-1-1", end = null, due = "2017-1-1", met = false)

    val invalidObligationsResponse = ObligationsModel(List())

  }
}
