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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.createIncomeSource._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

trait IncomeSourcesDataHelper {


  val businessDetails = BusinessDetails(accountingPeriodStartDate = "01-02-2023",
    accountingPeriodEndDate = "",
    tradingName = "",
    addressDetails = AddressDetails(
      addressLine1 = "tests test",
      addressLine2 = Some(""),
      addressLine3 = None,
      addressLine4 = None,
      countryCode = Some("UK"),
      postalCode = Some("")
    ),
    typeOfBusiness = None,
    tradingStartDate = "",
    cashOrAccrualsFlag = Some("CASH"),
    cessationDate = Some(""),
    cessationReason = None
  )

  val createBusinessDetailsRequestObject = CreateBusinessIncomeSourceRequest(businessDetails =
    List(businessDetails)
  )

  val createBusinessDetailsRequestObjectJson: JsValue = Json.parse(
    """{
      |    "businessDetails": [
      |        {
      |            "accountingPeriodStartDate": "01-02-2023",
      |            "accountingPeriodEndDate": "",
      |            "tradingName": "",
      |            "addressDetails": {
      |                "addressLine1": "tests test",
      |                "addressLine2": "",
      |                "countryCode": "UK",
      |                "postalCode": ""
      |            },
      |            "tradingStartDate": "",
      |            "cashOrAccrualsFlag": "CASH",
      |            "cessationDate": ""
      |        }
      |    ]
      |}""".stripMargin)

  val createForeignPropertyRequestObject = CreateForeignPropertyIncomeSourceRequest(PropertyDetails(tradingStartDate = LocalDate.of(2011, 1, 1).toString,
    cashOrAccrualsFlag = Some("CASH"), startDate = LocalDate.of(2011, 1, 1).toString)
  )

  val createForeignPropertyRequestObjectJson = Json.parse("""{
        |    "foreignPropertyDetails": {
        |        "tradingStartDate": "2011-01-01",
        |        "cashOrAccrualsFlag": "CASH",
        |        "startDate": "2011-01-01"
        |    }
        |}""".stripMargin)

  val createUKPropertyRequestObject = CreateUKPropertyIncomeSourceRequest(PropertyDetails(tradingStartDate = LocalDate.of(2011, 1, 1).toString,
    cashOrAccrualsFlag = Some("CASH"), startDate = LocalDate.of(2011, 1, 1).toString)
  )

  val createUKPropertyRequestObjectJson  = Json.parse("""{
    |    "ukPropertyDetails": {
    |        "tradingStartDate": "2011-01-01",
    |        "cashOrAccrualsFlag": "CASH",
    |        "startDate": "2011-01-01"
    |    }
    |}""".stripMargin)

  val createBusinessViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("someBusinessName"),
    businessStartDate = Some(LocalDate.of(2022, 11, 11)),
    businessTrade = "someBusinessTrade",
    businessPostalCode = Some("SE15 4ER"),
    incomeSourcesAccountingMethod = None,
    accountingPeriodEndDate = LocalDate.of(2022, 11, 11),
    businessAddressLine1 = "businessAddressLine1",
    businessAddressLine2 = Some(""),
    businessAddressLine3 = Some(""),
    businessAddressLine4 = None,
    businessCountryCode = Some("GB"),
    cashOrAccrualsFlag = Some("CASH"),
    showedAccountingMethod = false
  )

  val createForeignPropertyViewModel = CheckPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1), cashOrAccrualsFlag = Some("CASH"), incomeSourceType = ForeignProperty)

  val createUKPropertyViewModel = CheckPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1), cashOrAccrualsFlag = Some("CASH"), incomeSourceType = UkProperty)
}
