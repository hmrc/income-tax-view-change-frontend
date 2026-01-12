/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.helpers

import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import models.createIncomeSource._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckPropertyViewModel}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

trait IncomeSourcesDataHelper {


  val businessDetails: BusinessDetails = BusinessDetails(
    accountingPeriodStartDate = "01-02-2023",
    accountingPeriodEndDate = "",
    tradingName = "",
    address =
      AddressDetails(
        addressLine1 = "tests test",
        addressLine2 = Some(""),
        addressLine3 = None,
        addressLine4 = None,
        countryCode = Some("UK"),
        postalCode = Some("")
      ),
    typeOfBusiness = None,
    tradingStartDate = "",
    cessationDate = Some(""),
    cessationReason = None
  )

  val createBusinessDetailsRequestObject: CreateBusinessIncomeSourceRequest = CreateBusinessIncomeSourceRequest(
    mtdbsa = "XIAT00000000000",
    businessDetails = List(businessDetails)
  )

  val createBusinessDetailsRequestObjectJson: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XIAT00000000000",
      |  "businessDetails" : [ {
      |    "accountingPeriodStartDate" : "01-02-2023",
      |    "accountingPeriodEndDate" : "",
      |    "tradingName" : "",
      |    "address" : {
      |      "addressLine1" : "tests test",
      |      "addressLine2" : "",
      |      "countryCode" : "UK",
      |      "postalCode" : ""
      |    },
      |    "tradingStartDate" : "",
      |    "cessationDate" : ""
      |  } ]
      |}
      |""".stripMargin
  )

  val createForeignPropertyRequestObject: CreateForeignPropertyIncomeSourceRequest = CreateForeignPropertyIncomeSourceRequest(
    mtdbsa = "XIAT00000000000",
    foreignPropertyDetails = PropertyDetails(tradingStartDate = LocalDate.of(2011, 1, 1).toString, startDate = LocalDate.of(2011, 1, 1).toString)
  )

  val createForeignPropertyRequestObjectJson: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XIAT00000000000",
      |  "foreignPropertyDetails" : {
      |    "tradingStartDate" : "2011-01-01",
      |    "startDate" : "2011-01-01"
      |  }
      |}
      |""".stripMargin
  )

  val createUKPropertyRequestObject: CreateUKPropertyIncomeSourceRequest = CreateUKPropertyIncomeSourceRequest(
    mtdbsa = "XIAT00000000000",
    ukPropertyDetails = PropertyDetails(tradingStartDate = LocalDate.of(2011, 1, 1).toString, startDate = LocalDate.of(2011, 1, 1).toString)
  )

  val createUKPropertyRequestObjectJson: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XIAT00000000000",
      |  "ukPropertyDetails" : {
      |    "tradingStartDate" : "2011-01-01",
      |    "startDate" : "2011-01-01"
      |  }
      |}
      |""".stripMargin
  )

  val createBusinessViewModel: CheckBusinessDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some("someBusinessName"),
      businessStartDate = Some(LocalDate.of(2022, 11, 11)),
      businessTrade = "someBusinessTrade",
      businessPostalCode = Some("SE15 4ER"),
      accountingPeriodEndDate = LocalDate.of(2022, 11, 11),
      businessAddressLine1 = "businessAddressLine1",
      businessAddressLine2 = Some(""),
      businessAddressLine3 = Some(""),
      businessAddressLine4 = None,
      businessCountryCode = Some("GB"),
      addressId = None
    )

  val createForeignPropertyViewModel: CheckPropertyViewModel = CheckPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1), incomeSourceType = ForeignProperty)

  val createUKPropertyViewModel: CheckPropertyViewModel = CheckPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1), incomeSourceType = UkProperty)
}
