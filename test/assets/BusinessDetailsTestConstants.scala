/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.NextUpdatesTestConstants.{openObligation, overdueObligation}
import models.core._
import models.incomeSourceDetails.BusinessDetailsModel
import models.nextUpdates.NextUpdatesModel

object BusinessDetailsTestConstants {


  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(2017, 6, 1), end = LocalDate.of(2018, 5, 30))
  val test2019BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(2018, 3, 5), end = LocalDate.of(2019, 3, 6))
  val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(2019, 3, 6), end = LocalDate.of(2018, 3, 6))
  val testTradeName = "business"
  val testTradeName2 = "business"
  val testBizAddress = AddressModel(
    addressLine1 = "64 Zoo Lane",
    addressLine2 = Some("Happy Place"),
    addressLine3 = Some("Magical Land"),
    addressLine4 = Some("England"),
    postCode = Some("ZL1 064"),
    countryCode = "UK"
  )
  val testContactDetails = ContactDetailsModel(Some("123456789"), Some("0123456789"), Some("8008135"), Some("google@chuckNorris.com"))
  val testCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)), Some("It was a stupid idea anyway"))
  val testMtdItId = "XIAT0000000000A"
  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = Some(LocalDate.of(2018, 4, 5))
  )


  val business2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName2),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val business2018 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = test2018BusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val business2019 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = test2019BusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val alignedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(start = LocalDate.of(2017, 4, 6), end = LocalDate.of(2018, 4, 5)),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  val ceasedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = testBusinessAccountingPeriod,
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = Some(testCessation),
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = None
  )

  val oldUseralignedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(start = LocalDate.of(2017, 4, 6), end = LocalDate.of(2018, 4, 5)),
    cashOrAccruals = Some("CASH"),
    tradingStartDate = Some("2017-1-1"),
    cessation = None,
    tradingName = Some(testTradeName),
    address = Some(testBizAddress),
    contactDetails = None,
    seasonal = None,
    paperless = None,
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  val businessErrorModel = ErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testSelfEmploymentId, List(overdueObligation, openObligation))

}
