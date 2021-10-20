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

import java.time.LocalDate

import models.nextUpdates.{ObligationsModel, NextUpdateModel, NextUpdatesModel}
import BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId, testSelfEmploymentId}

object PreviousObligationsIntegrationTestConstants {

  private val date: LocalDate = LocalDate.of(2017, 1, 1)

  def previousQuarterlyObligation(incomeId: String): NextUpdatesModel = NextUpdatesModel(
    incomeId,
    List(
      NextUpdateModel(
        date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001"
      )
    )
  )

  def previousEOPSObligation(incomeId: String): NextUpdatesModel = NextUpdatesModel(
    incomeId,
    List(
      NextUpdateModel(
        date.plusMonths(2), date.plusMonths(3), date.plusMonths(4), "EOPS", Some(date.plusMonths(3)), "EOPS"
      )
    )
  )

  val previousCrystallisationObligation: NextUpdatesModel = NextUpdatesModel(
    testMtditid,
    List(
      NextUpdateModel(
        date.plusMonths(4), date.plusMonths(5), date.plusMonths(6), "Crystallised", Some(date.plusMonths(5)), "Crystallised"
      )
    )
  )

  val previousObligationsModel = ObligationsModel(Seq(
    previousQuarterlyObligation(testSelfEmploymentId),
    previousEOPSObligation(testPropertyIncomeId),
    previousCrystallisationObligation
  ))

}
