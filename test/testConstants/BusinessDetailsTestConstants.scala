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

import enums.IncomeSourceJourney.SelfEmployment
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core._
import models.incomeSourceDetails.viewmodels._
import models.incomeSourceDetails.{BusinessDetailsModel, LatencyDetails, QuarterTypeElection}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import testConstants.BaseTestConstants._
import testConstants.NextUpdatesTestConstants.{fakeNextUpdatesModel, openObligation, overdueObligation}

import java.time.{LocalDate, Month}

object BusinessDetailsTestConstants {

  val year2017: Int = 2017
  val year2018: Int = 2018
  val year2019: Int = 2019
  val year2022: Int = 2022
  val year2023: Int = 2023
  val year2024: Int = 2024
  val year2025: Int = 2025

  val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)
  val address = AddressModel(Some("8 Test"), Some("New Court"), Some("New Town"), Some("New City"), Some("NE12 6CI"), Some("United Kingdom"))
  val businessIncomeSourceId = "1234"

  val getCurrentTaxYearEnd: LocalDate = {
    if (fixedDate.isBefore(LocalDate.of(fixedDate.getYear, Month.APRIL, 6))) LocalDate.of(fixedDate.getYear, Month.APRIL, 5)
    else LocalDate.of(fixedDate.getYear + 1, Month.APRIL, 5)
  }

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2017, Month.JUNE, 1), end = LocalDate.of(year2018, Month.MAY, 30))
  val test2019BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2018, Month.MARCH, 5), end = LocalDate.of(year2019, Month.MARCH, 6))
  val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2019, Month.MARCH, 6), end = LocalDate.of(year2018, Month.MARCH, 6))
  val columnOneUkProperty = "UK property"
  val columnOneForeignProperty = "Foreign property"
  val testTradeName = "nextUpdates.business"
  val testIncomeSource = "Fruit Ltd"
  val testTradeName2 = "nextUpdates.business2"
  val testTradeNameOption: Option[String] = Some("nextUpdates.business")
  val testTradeNameOption2: Option[String] = Some("nextUpdates.business2")
  val testBizAddress = AddressModel(
    addressLine1 = Some("64 Zoo Lane"),
    addressLine2 = Some("Happy Place"),
    addressLine3 = Some("Magical Land"),
    addressLine4 = Some("England"),
    postCode = Some("ZL1 064"),
    countryCode = Some("UK")
  )
  val testContactDetails = ContactDetailsModel(Some("123456789"), Some("0123456789"), Some("8008135"), Some("google@chuckNorris.com"))
  val testCessation = CessationModel(Some(LocalDate.of(year2018, Month.JANUARY, 1)))
  val testCessation2 = CessationModel(Some(LocalDate.of(year2019, Month.JANUARY, 1)))
  val testCessation3 = CessationModel(Some(LocalDate.of(year2022, Month.JANUARY, 1)))
  val testStartDate = LocalDate.parse("2022-01-01")
  val testStartDate2005 = LocalDate.parse("2005-01-01")
  val testStartDate2 = LocalDate.parse("2021-01-01")
  val testStartDate3 = LocalDate.parse("2013-01-01")
  val testContextualTaxYear = "2024"
  val testStartDateFormatted = "1 January 2022"
  val testStartDate2Formatted = "1 January 2021"
  val testStartDate3Formatted = "1 January 2013"
  val testStartDateOption: Option[LocalDate] = Some(LocalDate.parse("2022-01-01"))
  val testStartDateOption2: Option[LocalDate] = Some(LocalDate.parse("2021-01-01"))
  val testStartDateOption3: Option[LocalDate] = Some(LocalDate.parse("2013-01-01"))
  val testCessationDate: String = "1 January 2018"
  val testCessationDate2: String = "1 January 2019"
  val testCessationDate3: String = "1 January 2022"
  val testEndDate = LocalDate.parse("2023-01-01")
  val testEndDateString: String = "2022-10-10"
  val testUnknownValue = "Unknown"
  val testUnknownSoleTraderBusinessValue = "Sole trader business"

  val testLatencyDetails = LatencyDetails(
    latencyEndDate = LocalDate.of(year2019, 1, 1),
    taxYear1 = year2018.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2019.toString,
    latencyIndicator2 = "Q")

  val testLatencyDetails1 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2022.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2023.toString,
    latencyIndicator2 = "Q")

  val testLatencyDetails2 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "Q")

  val testLatencyDetails3 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "Q")

  val testLatencyDetails4 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "Q",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "A")

  val testLatencyDetails5 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "A")

  val testLatencyDetailsWithOneInLatency = LatencyDetails(
    latencyEndDate = LocalDate.of(year2024, 12, 31),
    taxYear1 = year2022.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2023.toString,
    latencyIndicator2 = "A")

  val testLatencyDetailsWithOneInLatency2023 = LatencyDetails(
    latencyEndDate = LocalDate.of(year2024, 12, 31),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "A")

  val testLatencyDetailsWithBothYearsInLatency = LatencyDetails(
    latencyEndDate = LocalDate.of(year2025, 12, 31),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "A")

  val testLatencyDetailsCYUnknown = LatencyDetails(
    latencyEndDate = LocalDate.of(year2023, 1, 1),
    taxYear1 = year2023.toString,
    latencyIndicator1 = "Q",
    taxYear2 = year2024.toString,
    latencyIndicator2 = "A")


  val quarterTypeElectionStandard = QuarterTypeElection("STANDARD", "2021")
  val quarterTypeElectionCalendar = QuarterTypeElection("CALENDAR", "2021")

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails),
    address = Some(address),
  )


  val business1NoLatency = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(testBizAddress),
  )

  val business1Address2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(testBizAddress),
  )

  val businessWithLatency1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    address = Some(address),
  )

  val businessWithLatency2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    address = Some(address),
    quarterTypeElection = Some(quarterTypeElectionStandard),
  )

  val businessWithLatency3 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    address = Some(testBizAddress),
  )

  val businessWithLatency4 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    address = Some(testBizAddress),
  )

  val businessWithOneYearInLatency = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetailsWithOneInLatency),
    address = Some(testBizAddress),
  )

  val businessWithOneYearInLatency2023 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetailsWithOneInLatency2023),
    address = Some(testBizAddress),
  )

  val businessWithBothYearsInLatency = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetailsWithBothYearsInLatency),
    address = Some(testBizAddress),
  )

  val businessWithLatencyAndUnknowns = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    address = None,
  )

  val businessWithLatency2019 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails),
    address = Some(testBizAddress),
  )

  val soleTraderBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(address),
    quarterTypeElection = Some(quarterTypeElectionCalendar)
  )

  val soleTraderBusinessWithStartDate2005 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate2005),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(address),
    quarterTypeElection = Some(quarterTypeElectionCalendar)
  )

  val soleTraderBusiness2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate3),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(address)
  )

  val soleTraderBusiness3 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate3),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address),
  )

  val soleTraderBusiness4 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = None,
    contextualTaxYear = Some(testContextualTaxYear),
    cessation = None,
    address = Some(address),
  )

  val soleTraderBusinessNoTradingName = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = None,
  )

  val soleTraderBusinessNoTradingNameNoStartDate = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = None,
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(address),
  )

  val soleTraderBusinessNoTradingStartDate = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    address = Some(address),
  )

  val businessDetailsViewModel = BusinessDetailsViewModel(
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate)
  )

  val businessDetailsViewModel2 = BusinessDetailsViewModel(
    tradingName = Some(testTradeName2),
    tradingStartDate = Some(testStartDate2)
  )

  val ceaseBusinessDetailsViewModel = CeaseBusinessDetailsViewModel(
    incomeSourceId = mkIncomeSourceId("1234"),
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate)
  )

  val ceaseBusinessDetailsViewModel2 = CeaseBusinessDetailsViewModel(
    incomeSourceId = mkIncomeSourceId("1234"),
    tradingName = Some(testTradeName2),
    tradingStartDate = Some(testStartDate2)
  )

  val ceasedUkPropertyDetailsViewModel = CeasedBusinessDetailsViewModel(
    tradingName = Some(testTradeName),
    incomeSourceType = SelfEmployment,
    tradingStartDate = Some(testStartDate),
    cessationDate = testEndDate
  )

  val ceasedForeignPropertyDetailsViewModel = CeasedBusinessDetailsViewModel(
    tradingName = Some(testTradeName),
    incomeSourceType = SelfEmployment,
    tradingStartDate = Some(testStartDate),
    cessationDate = testEndDate
  )

  val ceasedBusinessDetailsViewModel = CeasedBusinessDetailsViewModel(
    tradingName = Some(testTradeName),
    incomeSourceType = SelfEmployment,
    tradingStartDate = Some(testStartDate),
    cessationDate = testEndDate
  )

  val viewBusinessDetailsViewModel = ViewBusinessDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = Some(testIncomeSource),
    tradingName = testTradeNameOption,
    tradingStartDate = testStartDateOption
  )

  val viewBusinessDetailsViewModel2 = ViewBusinessDetailsViewModel(
    incomeSourceId = mkIncomeSourceId(testSelfEmploymentId),
    incomeSource = Some(testIncomeSource),
    tradingName = testTradeNameOption,
    tradingStartDate = testStartDateOption
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    contextualTaxYear = None,
    firstAccountingPeriodEndDate = None,
    cessation = None,
    tradingStartDate = Some(testStartDate2),
    address = Some(address),
  )

  val businessWithNoCashOrAccrualsFlag = business2.copy()

  val business2018 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(test2018BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    contextualTaxYear = None,
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate),
    cessation = None,
    address = Some(address),
  )

  val business2019 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(test2019BusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    contextualTaxYear = None,
    firstAccountingPeriodEndDate = None,
    cessation = None,
    tradingStartDate = Some(testStartDate),
    address = Some(address),
  )

  val businessNotValidObligationType = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "notValidObligationType",
    dateReceived = None,
    periodKey = "#002",
    StatusFulfilled
  ))

  val alignedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    cessation = None,
    address = Some(address),
  )

  val ceasedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = Some(testCessation),
    address = Some(address),
  )

  val ceasedBusiness2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(testStartDate3),
    contextualTaxYear = None,
    cessation = Some(testCessation2),
    address = Some(address),
  )

  val oldUseralignedBusiness = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(year2017, Month.APRIL, 6), end = LocalDate.of(year2018, Month.APRIL, 5))),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address),
  )

  val businessErrorModel = ErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testSelfEmploymentId, List(overdueObligation, openObligation))
  val obligationsAllDeadlinesSuccessNotValidObligationType: ObligationsModel = ObligationsModel(
    Seq(GroupedObligationsModel(testSelfEmploymentId, List(businessNotValidObligationType))))

}
