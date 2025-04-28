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

import models.obligations.{GroupedObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseIntegrationTestConstants._

import java.time.LocalDate

object NextUpdatesIntegrationTestConstants {
  def successResponse(obligationsModel: GroupedObligationsModel): JsValue = {
    Json.toJson(obligationsModel)
  }

  def emptyResponse(): JsValue = Json.arr()

  def failureResponse(code: String, reason: String): JsValue = Json.obj(
    "code" -> code,
    "reason" -> reason
  )

  val currentDate =  LocalDate.of(2023, 4, 5)

  val deadlineStart1 = LocalDate.of(2017, 1, 1)
  val deadlineEnd1 = LocalDate.of(2017, 3, 31)
  val deadlineStart2 = LocalDate.of(2017, 4, 1)
  val deadlineEnd2 = LocalDate.of(2017, 6, 30)
  val deadlineStart3 = LocalDate.of(2016, 6, 1)
  val deadlineEnd3 = LocalDate.of(2017, 6, 30)
  val deadlineStart4 = LocalDate.of(2017, 7, 1)
  val deadlineEnd4 = LocalDate.of(2017, 9, 30)
  val deadlineStart5 = LocalDate.of(2017, 10, 1)
  val deadlineEnd5 = LocalDate.of(2018, 1, 31)
  val deadlineStart6 = LocalDate.of(2017, 11, 1)
  val deadlineEnd6 = LocalDate.of(2018, 2, 1)

  val testPropertyId = "1234"

  def multipleNextUpdatesDataSuccessModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      start = deadlineStart1,
      end = deadlineEnd1,
      due = currentDate.minusDays(128),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    ), SingleObligationModel(
      start = deadlineStart2,
      end = deadlineEnd2,
      due = currentDate.minusDays(36),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    ), SingleObligationModel(
      start = deadlineStart3,
      end = deadlineEnd3,
      due = currentDate.minusDays(36),
      obligationType = "EOPS",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    ), SingleObligationModel(
      start = deadlineStart4,
      end = deadlineEnd4,
      due = currentDate.plusDays(30),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    ), SingleObligationModel(
      start = deadlineStart5,
      end = deadlineEnd5,
      due = currentDate.plusDays(146),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    ), SingleObligationModel(
      start = deadlineStart6,
      end = deadlineEnd6,
      due = currentDate.plusDays(174),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    )
  ))

  val singleObligationStart = LocalDate.of(2017, 4, 6)
  val singleObligationEndQuarter = LocalDate.of(2017, 5, 5)
  val singleObligationEnd = LocalDate.of(2017, 7, 5)
  val singleObligationDue = LocalDate.of(2018, 1, 1)

  val overdueDate: LocalDate = currentDate.minusDays(1)

  def singleObligationQuarterlyReturnModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      due = singleObligationDue,
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", StatusFulfilled
    )
  ))

  val veryOverdueDate: LocalDate = LocalDate.of(2017, 5, 5)
  val veryOverDueLongDate = "5 May 2017"

  val singleObligationCrystallisationModel: GroupedObligationsModel = GroupedObligationsModel(testMtditid, List(
    SingleObligationModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      obligationType = "Crystallisation",
      due = veryOverdueDate,
      dateReceived = None,
      periodKey = "periodKey", status = StatusFulfilled
    )
  ))

  def singleObligationOverdueModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      start = singleObligationStart,
      end = singleObligationEnd,
      obligationType = "Quarterly",
      due = overdueDate,
      dateReceived = None,
      periodKey = "periodKey", status = StatusFulfilled
    )
  ))

  val singleObligationStartEOPs = LocalDate.of(2017, 4, 6)
  val singleObligationEndEOPs = LocalDate.of(2018, 7, 5)
  val singleObligationDueEOPs = LocalDate.of(2018, 1, 1)

  val singleObligationEOPSPropertyModel = GroupedObligationsModel(testPropertyId, List(
    SingleObligationModel(
      singleObligationStartEOPs,
      singleObligationEndEOPs,
      singleObligationDueEOPs,
      "EOPS",
      dateReceived = None,
      "EOPS", status = StatusFulfilled
    )
  ))

  val multipleObligationPropertyModel = GroupedObligationsModel(testPropertyId, List(
    SingleObligationModel(
      singleObligationStartEOPs,
      singleObligationEndEOPs,
      singleObligationDueEOPs,
      "EOPS",
      dateReceived = None,
      "EOPS", status = StatusFulfilled
    )
  ))

  def noObligationsModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(

  ))

  def singleObligationQuarterlyModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      singleObligationStart,
      singleObligationEnd,
      singleObligationDue,
      "Quarterly",
      dateReceived = None,
      "#001", status = StatusFulfilled
    )
  ))

  def singleObligationPlusYearOpenModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2017, 7, 5),
      due = currentDate.plusYears(1),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey", status = StatusFulfilled
    )
  ))


  def SEIncomeSourceEOPSModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List(
    SingleObligationModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2018, 1, 31),
      "EOPS",
      dateReceived = None,
      periodKey = "#003", status = StatusFulfilled
    )
  ))

  val crystallisedEOPSModel = GroupedObligationsModel(testMtditid, List(
    SingleObligationModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2019, 1, 31),
      "Crystallisation",
      dateReceived = None,
      periodKey = "#003", status = StatusFulfilled
    )
  ))

  val CrystallisationEOPSModelMulti = GroupedObligationsModel(testMtditid, List(
    SingleObligationModel(
      start = LocalDate.of(2018, 4, 6),
      end = LocalDate.of(2019, 4, 5),
      due = LocalDate.of(2020, 1, 31),
      "Crystallisation",
      dateReceived = None,
      periodKey = "#003", status = StatusFulfilled
    ),
    SingleObligationModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2019, 1, 31),
      "Crystallisation",
      dateReceived = None,
      periodKey = "#003", status = StatusFulfilled
    )
  ))

  def emptyModel(incomeId: String): GroupedObligationsModel = GroupedObligationsModel(incomeId, List())
}