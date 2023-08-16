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

package testConstants

import java.time.LocalDate
import BaseIntegrationTestConstants.{otherTestSelfEmploymentId, testSelfEmploymentId}
import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import models.incomeSourceDetails.BusinessDetailsModel
import testConstants.BaseIntegrationTestConstants.getCurrentTaxYearEnd

object BusinessDetailsIntegrationTestConstants {
  val startYear = getCurrentTaxYearEnd.getYear - 5
  val endYear = getCurrentTaxYearEnd.getYear - 4
  val b1CessationDate = LocalDate.of(startYear, 12, 31)
  val b1CessationReason = "It really, really was a bad idea"
  val b1TradingStart = LocalDate.parse("2017-01-01")
  val b1TradingName = "business"
  val b1AccountingStart = LocalDate.of(startYear, 1, 1)
  val b1AccountingEnd = LocalDate.of(startYear, 12, 31)
  val b1AddressLine1 = "64 Zoo Lane"
  val b1AddressLine2 = "Happy Place"
  val b1AddressLine3 = "Magical Land"
  val b1AddressLine4 = "England"
  val b1AddressLine5 = "ZL1 064"
  val b1CountryCode = "UK"
  val b1AccountingMethod = false

  val b2CessationDate = LocalDate.of(endYear, 12, 31)
  val b2CessationReason = "It really, really was a bad idea"
  val b2TradingStart = LocalDate.parse("2018-01-01")
  val b2TradingName = "secondBusiness"
  val b2AccountingStart = LocalDate.of(endYear, 1, 1)
  val b2AccountingEnd = LocalDate.of(endYear, 12, 31)
  val b2AddressLine1 = "742 Evergreen Terrace"
  val b2AddressLine2 = "Springfield"
  val b2AddressLine3 = "Oregon"
  val b2AddressLine4 = "USA"
  val b2AddressLine5 = "51MP 50N5"
  val b2CountryCode = "USA"
  val testMtdItId = "XIAT0000000000A"
  val ceasedBusinessTradingName = "ceasedBusiness"
  val testBusinessAddress: AddressModel = AddressModel(
    addressLine1 = "64 Zoo Lane",
    addressLine2 = Some("Happy Place"),
    addressLine3 = Some("Magical Land"),
    addressLine4 = Some("England"),
    postCode = Some("ZL1 064"),
    countryCode = "UK"
  )

  val address = AddressModel("8 Test", Some("New Court"), Some("New Town"), Some("New City"), Some("NE12 6CI"), "United Kingdom")

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    cessation = None,
    address = Some(address)
  )

  val business1WithAddress2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    cessation = None,
    address = Some(testBusinessAddress),
    cashOrAccruals = Some(b1AccountingMethod)
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    )),
    tradingName = Some(b2TradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd),
    tradingStartDate = Some(b2TradingStart),
    cessation = None,
    address = Some(address)
  )

  val business3WithUnknowns: BusinessDetailsModel = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = None,
    cessation = None,
    address = None,
    cashOrAccruals = None
  )

  val businessWithAddressAndAccountingMethod = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    cessation = None,
    address = Some(testBusinessAddress),
    cashOrAccruals = Some(b1AccountingMethod)
  )

  val ceasedBusiness1 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    )),
    tradingName = Some(ceasedBusinessTradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd),
    tradingStartDate = Some(b2TradingStart),
    cessation = Some(CessationModel(Some(b2CessationDate), Some(b2CessationReason))),
    address = Some(address)
  )

  val businessUnknownAddressName = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    cessation = None,
    address = None
  )
}