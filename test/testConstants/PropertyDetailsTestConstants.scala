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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, UkProperty}
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.viewmodels.{CeasePropertyDetailsViewModel, PropertyDetailsViewModel, ViewPropertyDetailsViewModel}
import models.incomeSourceDetails.{LatencyDetails, PropertyDetailsModel}
import models.nextUpdates.NextUpdateModel
import testConstants.BaseTestConstants.{testPropertyIncomeId, testPropertyIncomeId2, testSelfEmploymentId, testSelfEmploymentId2}
import testConstants.BusinessDetailsTestConstants.{quarterTypeElectionCalendar, quarterTypeElectionStandard, testLatencyDetails4, testLatencyDetails5}
import testConstants.NextUpdatesTestConstants.fakeNextUpdatesModel

import java.time.LocalDate

object PropertyDetailsTestConstants {

  val year2018: Int = 2018
  val year2019: Int = 2019
  val year2022: Int = 2022
  val year2023: Int = 2023
  val year2024: Int = 2024

  val testPropertyAccountingPeriod = AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))

  val testPropertyCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)), Some("It was a stupid idea anyway"))
  val testPropertyCessation2 = CessationModel(Some(LocalDate.of(2023, 6, 6)), Some("It was a stupid idea anyway"))
  val testPropertyCessation3 = CessationModel(Some(LocalDate.of(2020, 2, 2)), Some("It was a stupid idea anyway"))
  val testCeaseDate = Some(LocalDate.of(2022, 1, 1))
  val testStartDate = LocalDate.of(2022, 1, 1)
  val testStartDate2 = LocalDate.of(2021, 1, 1)
  val testPropertyStartDateOption: Option[LocalDate] = Some(LocalDate.of(2022, 1, 1))
  val testPropertyStartDateOption2: Option[LocalDate] = Some(LocalDate.of(2021, 1, 1))
  val testIncomeType = "property-unspecified"
  val ukIncomeType = "uk-property"
  val foreignIncomeType = "foreign-property"

  val propertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(testIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    cashOrAccruals = true
  )

  val ukPropertyDetailsViewModel = PropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate)
  )

  val ceaseUkPropertyDetailsViewModel = CeasePropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate)
  )

  val viewUkPropertyDetailsViewModel = ViewPropertyDetailsViewModel(
    tradingStartDate = testPropertyStartDateOption
  )

  val foreignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    quarterTypeElection = Some(quarterTypeElectionCalendar),
    cashOrAccruals = true
  )

  val foreignPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId2,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    cashOrAccruals = true
  )

  val foreignPropertyDetailsNoStartDate = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    cessation = None,
    cashOrAccruals = true
  )

  val foreignPropertyDetailsViewModel = PropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate2)
  )

  val uKPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    cashOrAccruals = true
  )

  val uKPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    cashOrAccruals = true
  )

  val ceaseForeignPropertyDetailsViewModel = CeasePropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate2)
  )

  val ceasedPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(testIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = Some(testPropertyCessation),
    cashOrAccruals = true
  )

  val ceasedUKPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = Some(testPropertyCessation),
    cashOrAccruals = true
  )

  val ceasedUKPropertyDetailsCessation2020 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = Some(testPropertyCessation3),
    cashOrAccruals = true
  )

  val ceasedForeignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = Some(testPropertyCessation),
    cashOrAccruals = true
  )

  val ceasedForeignPropertyDetailsCessation2023 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate2),
    cessation = Some(testPropertyCessation2),
    cashOrAccruals = true
  )

  val ceasedForeignPropertyDetailsNoIncomeSourceType = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = None,
    tradingStartDate = Some(testStartDate2),
    cessation = Some(testPropertyCessation2),
    cashOrAccruals = true
  )

  val openCrystallised: NextUpdateModel = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 31),
    periodKey = "#003",
    dateReceived = None,
    obligationType = "Crystallised"
  ))

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

  val ceasedPropertyDetailsModel: IncomeSourceType => PropertyDetailsModel = (incomeSourceType: IncomeSourceType) => {
    require(incomeSourceType == UkProperty || incomeSourceType == ForeignProperty)
    PropertyDetailsModel(
      incomeSourceId = testPropertyIncomeId,
      accountingPeriod = Some(testPropertyAccountingPeriod),
      firstAccountingPeriodEndDate = None,
      incomeSourceType = if (incomeSourceType == UkProperty) Some(ukIncomeType) else Some(foreignIncomeType),
      tradingStartDate = Some(testStartDate),
      cessation = Some(CessationModel(date = testCeaseDate, reason = Some("01"))),
      latencyDetails = None,
      quarterTypeElection = Some(quarterTypeElectionStandard)
    )
  }

  val ukPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = None,
    quarterTypeElection = Some(quarterTypeElectionStandard),
    cashOrAccruals = true
  )

  val ukPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = true
  )

  val ukPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    cashOrAccruals = true
  )

  val ukPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails4),
    quarterTypeElection = Some(quarterTypeElectionCalendar),
    cashOrAccruals = true
  )

  val ukPropertyWithLatencyDetailsAndUnknowns = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    cashOrAccruals = true
  )

  val foreignPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    cashOrAccruals = true
  )

  val foreignPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails5),
    quarterTypeElection = Some(quarterTypeElectionStandard),
    cashOrAccruals = true
  )

  val foreignPropertyWithLatencyDetailsAndUnknowns = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    cashOrAccruals = true
  )
}
