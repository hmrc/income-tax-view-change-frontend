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
import models.obligations.{SingleObligationModel, StatusFulfilled}
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

  val testPropertyCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)))
  val testPropertyCessation2 = CessationModel(Some(LocalDate.of(2023, 6, 6)))
  val testPropertyCessation3 = CessationModel(Some(LocalDate.of(2020, 2, 2)))
  val testCeaseDate = Some(LocalDate.of(2022, 1, 1))
  val testStartDate = LocalDate.of(2022, 1, 1)
  val testStartDate2 = LocalDate.of(2021, 1, 1)
  val testStartDateBeforeEarliestStartDate = LocalDate.of(2013, 1, 1)
  val testContextualTaxYear = "2024"
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
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(true)
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
    contextualTaxYear = None,
    cessation = None,
    quarterTypeElection = Some(quarterTypeElectionCalendar),
    cashOrAccruals = Some(true)
  )

  val foreignPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId2,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(true)
  )

  val foreignPropertyDetailsNoStartDate = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(true)
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
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(true)
  )

  val uKPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(true)
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
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation),
    cashOrAccruals = Some(true)
  )

  val ceasedUKPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation),
    cashOrAccruals = Some(true)
  )

  val ceasedUKPropertyDetailsCessation2020 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation3),
    cashOrAccruals = Some(true)
  )

  val ceasedForeignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation),
    cashOrAccruals = Some(true)
  )

  val ceasedForeignPropertyDetailsCessation2023 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate2),
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation2),
    cashOrAccruals = Some(true)
  )

  val ceasedForeignPropertyDetailsNoIncomeSourceType = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = None,
    tradingStartDate = Some(testStartDate2),
    contextualTaxYear = None,
    cessation = Some(testPropertyCessation2),
    cashOrAccruals = Some(true)
  )

  val openCrystallised: SingleObligationModel = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 31),
    periodKey = "#003",
    dateReceived = None,
    obligationType = "Crystallisation",
    status = StatusFulfilled
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
      contextualTaxYear = None,
      cessation = Some(CessationModel(date = testCeaseDate)),
      latencyDetails = None,
      quarterTypeElection = Some(quarterTypeElectionStandard),
      cashOrAccruals = Some(true)
    )
  }

  val ukPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    quarterTypeElection = Some(quarterTypeElectionStandard),
    cashOrAccruals = Some(true)
  )

  val ukPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = Some(true)
  )

  val ukPropertyDetails3BeforeEarliest = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDateBeforeEarliestStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = Some(true)
  )

  val ukPropertyDetailsBeforeContextualTaxYear = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = None,
    contextualTaxYear = Some(testContextualTaxYear),
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = Some(true)
  )

  val ukPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    cashOrAccruals = Some(true)
  )

  val ukPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails4),
    quarterTypeElection = Some(quarterTypeElectionCalendar),
    cashOrAccruals = Some(true)
  )

  val ukPropertyWithLatencyDetailsAndUnknowns = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    cashOrAccruals = Some(true)
  )

  val foreignPropertyDetailsBeforeEarliest = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDateBeforeEarliestStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = Some(true)
  )

  val foreignPropertyDetailsBeforeContextualTaxYear = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    contextualTaxYear = Some(testContextualTaxYear),
    cessation = None,
    latencyDetails = None,
    cashOrAccruals = Some(true)
  )

  val foreignPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails1),
    cashOrAccruals = Some(true)
  )

  val foreignPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails5),
    quarterTypeElection = Some(quarterTypeElectionStandard),
    cashOrAccruals = Some(true)
  )

  val foreignPropertyWithLatencyDetailsAndUnknowns = PropertyDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails2),
    cashOrAccruals = Some(true)
  )
}
