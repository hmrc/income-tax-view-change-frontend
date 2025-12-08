/*
 * Copyright 2024 HM Revenue & Customs
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

package helpers

import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{AddIncomeSourceData, Address}
import testConstants.BaseIntegrationTestConstants.testSelfEmploymentId

import java.time.LocalDate

object IncomeSourceCheckDetailsConstants {

  val testBusinessId: String = testSelfEmploymentId
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessCountryCode: String = "United Kingdom"

  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))
  val testErrorReason: String =
    "Failed to create incomeSources: CreateIncomeSourceErrorResponse(500,Error creating incomeSource: [{\"status\":500,\"reason\":\"INTERNAL_SERVER_ERROR\"}])"


  val testSEViewModel: CheckDetailsViewModel =
    CheckBusinessDetailsViewModel(
      businessName = Some(testBusinessName),
      businessStartDate = Some(testBusinessStartDate),
      accountingPeriodEndDate = (testAccountingPeriodEndDate),
      businessTrade = testBusinessTrade,
      businessAddressLine1 = testBusinessAddressLine1,
      businessAddressLine2 = None,
      businessAddressLine3 = None,
      businessAddressLine4 = None,
      businessPostalCode = Some(testBusinessPostCode),
      businessCountryCode = Some(testCountryCode)
    )

  val testUKPropertyViewModel =
    CheckPropertyViewModel(
      tradingStartDate = testBusinessStartDate,
      incomeSourceType = UkProperty
    )

  val testForeignPropertyViewModel =
    CheckPropertyViewModel(
      tradingStartDate = testBusinessStartDate,
      incomeSourceType = ForeignProperty
    )


  val testAddBusinessData: AddIncomeSourceData =
    AddIncomeSourceData(
      businessName = Some(testBusinessName),
      businessTrade = Some(testBusinessTrade),
      dateStarted = Some(testBusinessStartDate),
      incomeSourceId = Some(testBusinessId),
      address = Some(testBusinessAddress),
      countryCode = Some(testCountryCode),
      accountingPeriodEndDate = Some(testAccountingPeriodEndDate)
    )

  val testAddBusinessDataError: AddIncomeSourceData =
    AddIncomeSourceData(
      businessName = Some(testBusinessName),
      businessTrade = Some(testBusinessTrade),
      dateStarted = None,
      incomeSourceId = Some(testBusinessId),
      address = Some(testBusinessAddress),
      countryCode = Some(testCountryCode),
      accountingPeriodEndDate = Some(testAccountingPeriodEndDate)
    )

  val testAddPropertyData: AddIncomeSourceData =
    AddIncomeSourceData(dateStarted = Some(testBusinessStartDate))
}
