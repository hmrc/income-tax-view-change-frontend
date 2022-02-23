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

import BaseIntegrationTestConstants._
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel}
import play.api.libs.json.{JsValue, Json}

object NextUpdatesIntegrationTestConstants {
  def successResponse(obligationsModel: NextUpdatesModel): JsValue = {
    Json.toJson(obligationsModel)
  }

  def emptyResponse(): JsValue = Json.arr()

  def failureResponse(code: String, reason: String): JsValue = Json.obj(
    "code" -> code,
    "reason" -> reason
  )

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

  def multipleNextUpdatesDataSuccessModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      start = deadlineStart1,
      end = deadlineEnd1,
      due = LocalDate.now().minusDays(128),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), NextUpdateModel(
      start = deadlineStart2,
      end = deadlineEnd2,
      due = LocalDate.now().minusDays(36),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), NextUpdateModel(
      start = deadlineStart3,
      end = deadlineEnd3,
      due = LocalDate.now().minusDays(36),
      obligationType = "EOPS",
      dateReceived = None,
      periodKey = "periodKey"
    ), NextUpdateModel(
      start = deadlineStart4,
      end = deadlineEnd4,
      due = LocalDate.now().plusDays(30),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), NextUpdateModel(
      start = deadlineStart5,
      end = deadlineEnd5,
      due = LocalDate.now().plusDays(146),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), NextUpdateModel(
      start = deadlineStart6,
      end = deadlineEnd6,
      due = LocalDate.now().plusDays(174),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val singleObligationStart = LocalDate.of(2017, 4, 6)
  val singleObligationEndQuarter = LocalDate.of(2017, 5, 5)
  val singleObligationEnd = LocalDate.of(2017, 7, 5)
  val singleObligationDue = LocalDate.of(2018, 1, 1)

  val overdueDate: LocalDate = LocalDate.now().minusDays(1)

  def singleObligationQuarterlyReturnModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      due = singleObligationDue,
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val veryOverdueDate: LocalDate = LocalDate.of(2017, 5, 5)
  val veryOverDueLongDate = "5 May 2017"

  val singleObligationCrystallisationModel: NextUpdatesModel = NextUpdatesModel(testMtditid, List(
    NextUpdateModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      obligationType = "Crystallised",
      due = veryOverdueDate,
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  def singleObligationOverdueModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      start = singleObligationStart,
      end = singleObligationEnd,
      obligationType = "Quarterly",
      due = overdueDate,
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val singleObligationStartEOPs = LocalDate.of(2017, 4, 6)
  val singleObligationEndEOPs = LocalDate.of(2018, 7, 5)
  val singleObligationDueEOPs = LocalDate.of(2018, 1, 1)

  val singleObligationEOPSPropertyModel = NextUpdatesModel(testPropertyId, List(
    NextUpdateModel(
      singleObligationStartEOPs,
      singleObligationEndEOPs,
      singleObligationDueEOPs,
      "EOPS",
      dateReceived = None,
      "EOPS"
    )
  ))

  def noObligationsModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(

  ))

  def singleObligationQuarterlyModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      singleObligationStart,
      singleObligationEnd,
      singleObligationDue,
      "Quarterly",
      dateReceived = None,
      "#001"
    )
  ))

  def singleObligationPlusYearOpenModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2017, 7, 5),
      due = LocalDate.now().plusYears(1),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))


  def SEIncomeSourceEOPSModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List(
    NextUpdateModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2018, 1, 31),
      "EOPS",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  val crystallisedEOPSModel = NextUpdatesModel(testMtditid, List(
    NextUpdateModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2019, 1, 31),
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  val crystallisedEOPSModelMulti = NextUpdatesModel(testMtditid, List(
    NextUpdateModel(
      start = LocalDate.of(2018, 4, 6),
      end = LocalDate.of(2019, 4, 5),
      due = LocalDate.of(2020, 1, 31),
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    ),
    NextUpdateModel(
      start = LocalDate.of(2017, 4, 6),
      end = LocalDate.of(2018, 4, 5),
      due = LocalDate.of(2019, 1, 31),
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  def emptyModel(incomeId: String): NextUpdatesModel = NextUpdatesModel(incomeId, List())
}