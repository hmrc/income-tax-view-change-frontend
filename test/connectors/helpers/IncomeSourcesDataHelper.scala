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

import models.addIncomeSource.{AddressDetails, BusinessDetails, CreateBusinessIncomeSourceRequest, CreateForeignPropertyIncomeSource}
import models.incomeSourceDetails.viewmodels._

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
    cashOrAccrualsFlag = "",
    cessationDate = Some(""),
    cessationReason = None
  )

  val createBusinessDetailsRequestObject = CreateBusinessIncomeSourceRequest(businessDetails =
    List(businessDetails)
  )

  val createForeignPropertyRequestObject = CreateForeignPropertyIncomeSource(tradingStartDate = LocalDate.of(2011, 1, 1).toString,
    cashOrAccrualsFlag = "Cash", startDate = LocalDate.of(2011, 1, 1).toString
  )

  val createBusinessViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("someBusinessName"),
    businessStartDate = Some(LocalDate.of(2022, 11, 11)),
    businessTrade = "someBusinessTrade",
    businessAddressLine1 = "businessAddressLine1",
    businessPostalCode = Some("SE15 4ER"),
    businessAccountingMethod = None,
    accountingPeriodEndDate = LocalDate.of(2022, 11, 11),
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessCountryCode = Some("UK"),
    cashOrAccrualsFlag = "CASH"
  )

  val createForeignPropertyViewModel = CheckForeignPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1), cashOrAccrualsFlag = "Cash")
}
