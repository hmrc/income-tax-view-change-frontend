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

import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.viewmodels.{CeasePropertyDetailsViewModel, PropertyDetailsViewModel, ViewPropertyDetailsViewModel}
import models.incomeSourceDetails.{LatencyDetails, PropertyDetailsModel}
import models.nextUpdates.NextUpdateModel
import testConstants.BaseTestConstants.{testPropertyIncomeId, testSelfEmploymentId}
import testConstants.NextUpdatesTestConstants.fakeNextUpdatesModel

import java.time.LocalDate

object PropertyDetailsTestConstants {

  val year2018: Int = 2018
  val year2019: Int = 2019
  val year2022: Int = 2022
  val year2023: Int = 2023
  val year2024: Int = 2024

  val testPropertyAccountingPeriod = AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))

  val testCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)), Some("It was a stupid idea anyway"))

  val testStartDate = LocalDate.parse("2022-01-01")
  val testStartDate2 = LocalDate.parse("2021-01-01")
  val testStartDateOption: Option[LocalDate] = Some(LocalDate.parse("2022-01-01"))
  val testIncomeType = "property-unspecified"
  val ukIncomeType = "uk-property"
  val foreignIncomeType = "foreign-property"

  val propertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(testIncomeType),
    tradingStartDate = Some(testStartDate),
    None
  )

  val ukPropertyDetailsViewModel = PropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate)
  )

  val ceaseUkPropertyDetailsViewModel = CeasePropertyDetailsViewModel(
    tradingStartDate = testStartDate
  )

  val viewUkPropertyDetailsViewModel = ViewPropertyDetailsViewModel(
    tradingStartDate = testStartDateOption
  )

  val foreignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    None
  )

  val foreignPropertyDetailsNoStartDate = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = None,
    None
  )

  val foreignPropertyDetailsViewModel = PropertyDetailsViewModel(
    tradingStartDate = Some(testStartDate2)
  )

  val ceaseForeignPropertyDetailsViewModel = CeasePropertyDetailsViewModel(
    tradingStartDate = testStartDate2
  )

  val ceasedPropertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(testIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = Some(testCessation)
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

  val ukPropertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails)
  )

  val ukPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails1)
  )

  val ukPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails2)
  )

  val foreignPropertyWithLatencyDetails1 = PropertyDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails1)
  )

  val foreignPropertyWithLatencyDetails2 = PropertyDetailsModel(
    incomeSourceId = Some(testSelfEmploymentId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    cessation = None,
    latencyDetails = Some(testLatencyDetails2)
  )
}
