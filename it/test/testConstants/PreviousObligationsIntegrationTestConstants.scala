/*
 * Copyright 2017 HM Revenue & Customs
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

import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId, testSelfEmploymentId}

import java.time.LocalDate

object PreviousObligationsIntegrationTestConstants {

  private val date: LocalDate = LocalDate.of(2017, 1, 1)

  def previousQuarterlyObligation(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(
    incomeId,
    List(
      SingleObligationModel(
        date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001", status = StatusFulfilled
      )
    )
  )

  def previousEOPSObligation(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(
    incomeId,
    List(
      SingleObligationModel(
        date.plusMonths(2), date.plusMonths(3), date.plusMonths(4), "EOPS", Some(date.plusMonths(3)), "EOPS", status = StatusFulfilled
      )
    )
  )

  val previousCrystallisationObligation: GroupedObligationsModel = GroupedObligationsModel(
    testMtditid,
    List(
      SingleObligationModel(
        date.plusMonths(4), date.plusMonths(5), date.plusMonths(6), "Crystallisation", Some(date.plusMonths(5)), "Crystallisation", status = StatusFulfilled
      )
    )
  )

  val previousObligationsModel = ObligationsModel(Seq(
    previousQuarterlyObligation(testSelfEmploymentId),
    previousEOPSObligation(testPropertyIncomeId),
    previousCrystallisationObligation
  ))

}
