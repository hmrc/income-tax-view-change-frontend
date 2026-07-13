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

package obligations.testConstants

import common.models.core.*
import common.models.incomeSourceDetails.{BusinessDetailsModel, LatencyDetails, QuarterTypeElection}
import common.models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import common.testConstants.BaseTestConstants.*
import shared.testConstants.NextUpdatesTestConstants.fakeNextUpdatesModel

import java.time.{LocalDate, Month}

object BusinessDetailsTestConstants {

  val year2017: Int = 2017
  val year2018: Int = 2018
  val year2019: Int = 2019

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2017, Month.JUNE, 1), end = LocalDate.of(year2018, Month.MAY, 30))
  val testTradeName = "nextUpdates.business"
  val testIncomeSource = "Fruit Ltd"

  val testStartDate = LocalDate.parse("2022-01-01")

  val testLatencyDetails = LatencyDetails(
    latencyEndDate = LocalDate.of(year2019, 1, 1),
    taxYear1 = year2018.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2019.toString,
    latencyIndicator2 = "Q")

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

  val businessNotValidObligationType = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "notValidObligationType",
    dateReceived = None,
    periodKey = "#002",
    StatusFulfilled
  ))

  val obligationsAllDeadlinesSuccessNotValidObligationType: ObligationsModel = ObligationsModel(
    Seq(GroupedObligationsModel(testSelfEmploymentId, List(businessNotValidObligationType))))

}
