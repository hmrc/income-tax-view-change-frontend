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

import testConstants.BaseTestConstants.testPropertyIncomeId
import testConstants.NextUpdatesTestConstants.fakeNextUpdatesModel
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.viewmodels.PropertyDetailsViewModel
import models.incomeSourceDetails.{PropertiesRentedModel, PropertyDetailsModel}
import models.nextUpdates.NextUpdateModel

import java.time.LocalDate

object PropertyDetailsTestConstants {

  val testPropertyAccountingPeriod = AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))

  val testCessation = CessationModel(Some(LocalDate.of(2018, 1, 1)), Some("It was a stupid idea anyway"))

  val testStartDate = LocalDate.parse("2022-01-01")
  val testStartDate2 = LocalDate.parse("2021-01-01")
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

  val ukPropertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(ukIncomeType),
    tradingStartDate = Some(testStartDate),
    None
  )

  val ukPropertyDetailsViewModel = PropertyDetailsViewModel(
    tradingStartDate = testStartDate
  )

  val foreignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = Some(testPropertyIncomeId),
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    None
  )

  val foreignPropertyDetailsViewModel = PropertyDetailsViewModel(
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
}
