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

package testConstants

import java.time.{LocalDate, Month}
import testConstants.BaseTestConstants._
import testConstants.NextUpdatesTestConstants.{fakeNextUpdatesModel, openObligation, overdueObligation}
import models.core._
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourceDetails.viewmodels.{BusinessDetailsViewModel, CeaseBusinessDetailsViewModel, ViewBusinessDetailsViewModel}
import models.incomeSourceDetails.viewmodels.{BusinessDetailsViewModel, CeasedBusinessDetailsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}

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
  val testTradeName = "nextUpdates.business"
  val testTradeName2 = "nextUpdates.business2"
  val testTradeNameOption: Option[String] = Some("nextUpdates.business")
  val testTradeNameOption2: Option[String] = Some("nextUpdates.business2")
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
  val testCessation2 = CessationModel(Some(LocalDate.of(year2019, Month.JANUARY, 1)), Some("It was a stupid idea anyway"))
  val testMtdItId = "XIAT0000000000A"
  val testStartDate = LocalDate.parse("2022-01-01")
  val testStartDate2 = LocalDate.parse("2021-01-01")
  val testStartDateOption: Option[LocalDate] = Some(LocalDate.parse("2022-01-01"))
  val testStartDateOption2: Option[LocalDate] = Some(LocalDate.parse("2021-01-01"))
  val testEndDate = LocalDate.parse("2023-01-01")


  val business1 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    cessation = None
  )

  val soleTraderBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    cessation = None
  )

  val soleTraderBusinessNoTradingName = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    cessation = None
  )

  val soleTraderBusinessNoTradingStartDate = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = None,
    cessation = None
  )

  val businessDetailsViewModel = BusinessDetailsViewModel(
    tradingName = testTradeName,
    tradingStartDate = testStartDate
  )

  val businessDetailsViewModel2 = BusinessDetailsViewModel(
    tradingName = testTradeName2,
    tradingStartDate = testStartDate2
  )

  val ceaseBusinessDetailsViewModel = CeaseBusinessDetailsViewModel(
    incomeSourceId = "1234",
    tradingName = testTradeName,
    tradingStartDate = testStartDate
  )

  val ceaseBusinessDetailsViewModel2 = CeaseBusinessDetailsViewModel(
    incomeSourceId = "1234",
    tradingName = testTradeName2,
    tradingStartDate = testStartDate2
  )

  val ceasedBusinessDetailsViewModel = CeasedBusinessDetailsViewModel(
    tradingName = testTradeName,
    tradingStartDate = testStartDate,
    cessationDate = testEndDate
  )

  val viewBusinessDetailsViewModel = ViewBusinessDetailsViewModel(
    tradingName = testTradeNameOption,
    tradingStartDate = testStartDateOption
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId2),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    firstAccountingPeriodEndDate = None,
    cessation = None,
    tradingStartDate = Some(testStartDate2)
  )

  val business2018 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(test2018BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate),
    cessation = None
  )

  val business2019 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(test2019BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None,
    cessation = None,
    tradingStartDate = Some(testStartDate)
  )

  val businessNotValidObligationType = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "notValidObligationType",
    dateReceived = None,
    periodKey = "#002"
  ))

  val alignedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    cessation = None
  )

  val ceasedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate),
    cessation = Some(testCessation)
  )

  val ceasedBusiness2 = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate2),
    cessation = Some(testCessation2)
  )

  val oldUseralignedBusiness = BusinessDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    tradingStartDate = Some(testStartDate),
    cessation = None
  )

  val businessErrorModel = ErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testSelfEmploymentId, List(overdueObligation, openObligation))
  val obligationsAllDeadlinesSuccessNotValidObligationType: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel(testSelfEmploymentId, List(businessNotValidObligationType))))

}
