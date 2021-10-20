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
import implicits.ImplicitDateFormatter
import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import models.incomeSourceDetails.BusinessDetailsModel

object BusinessDetailsIntegrationTestConstants {

  val b1CessationDate = LocalDate.of(2017,12,31)
  val b1CessationReason = "It really, really was a bad idea"
  val b1TradingStart = "2017-01-01"
  val b1TradingName = "business"
  val b1AccountingStart = LocalDate.of(2017, 1, 1)
  val b1AccountingEnd = LocalDate.of(2017,12,31)
  val b1AddressLine1 = "64 Zoo Lane"
  val b1AddressLine2 = "Happy Place"
  val b1AddressLine3 = "Magical Land"
  val b1AddressLine4 = "England"
  val b1AddressLine5 = "ZL1 064"
  val b1CountryCode = "UK"

  val b2CessationDate = LocalDate.of(2018,12,31)
  val b2CessationReason = "It really, really was a bad idea"
  val b2TradingStart = "2018-01-01"
  val b2TradingName = "secondBusiness"
  val b2AccountingStart = LocalDate.of(2018,1,1)
  val b2AccountingEnd = LocalDate.of(2018,12,31)
  val b2AddressLine1 = "742 Evergreen Terrace"
  val b2AddressLine2 = "Springfield"
  val b2AddressLine3 = "Oregon"
  val b2AddressLine4 = "USA"
  val b2AddressLine5 = "51MP 50N5"
  val b2CountryCode = "USA"
  val testMtdItId = "XIAT0000000000A"

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    ),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some(b1TradingStart),
    cessation = Some(CessationModel(
      date = Some(b1CessationDate),
      reason = Some(b1CessationReason)
    )),
    tradingName = Some(b1TradingName),
    address = Some(AddressModel(
      addressLine1 = b1AddressLine1,
      addressLine2 = Some(b1AddressLine2),
      addressLine3 = Some(b1AddressLine3),
      addressLine4 = Some(b1AddressLine4),
      postCode = Some(b1AddressLine5),
      countryCode = b1CountryCode
    )),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = Some(b1AccountingEnd)
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    ),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some(b2TradingStart),
    cessation = Some(CessationModel(
      date = Some(b2CessationDate),
      reason = Some(b2CessationReason)
    )),
    tradingName = Some(b2TradingName),
    address = Some(AddressModel(
      addressLine1 = b2AddressLine1,
      addressLine2 = Some(b2AddressLine2),
      addressLine3 = Some(b2AddressLine3),
      addressLine4 = Some(b2AddressLine4),
      postCode = Some(b2AddressLine5),
      countryCode = b2CountryCode
    )),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = Some(b2AccountingEnd)
  )

}