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

package testConstants

import java.time.{LocalDate, Month}
import testConstants.BaseTestConstants._
import testConstants.NextUpdatesTestConstants.{openObligation, overdueObligation}
import models.core._
import models.incomeSourceDetails.BusinessDetailsModel
import models.nextUpdates.NextUpdatesModel

object BusinessDetailsTestConstants {

  val year2017: Int = 2017
  val year2018: Int = 2018
  val year2019: Int = 2019

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) LocalDate.of(currentDate.getYear, Month.APRIL, 5)
    else LocalDate.of(currentDate.getYear + 1, Month.APRIL, 5)
  }

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2017, Month.JUNE, 1), end = LocalDate.of(year2018, Month.MAY, 30))
  val test2019BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2018, Month.MARCH, 5), end = LocalDate.of(year2019, Month.MARCH, 6))
  val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2019, Month.MARCH, 6), end = LocalDate.of(year2018, Month.MARCH, 6))
  val testTradeName = "Business income"
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
  val testCessation = CessationModel(Some(LocalDate.of(year2018, Month.JANUARY, 1)), Some("It was a stupid idea anyway"))
  val testMtdItId = "XIAT0000000000A"
  val business1 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5))
  )


  val business2 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId2),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    firstAccountingPeriodEndDate = None
  )

  val business2018 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(test2018BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None
  )

  val business2019 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(test2019BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None
  )

  val alignedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  val ceasedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None
  )

  val oldUseralignedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  val businessErrorModel = ErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testSelfEmploymentId, List(overdueObligation, openObligation))

}
