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
package assets

import java.time.LocalDate

import implicits.ImplicitDateFormatter._
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesModel}
import play.api.libs.json.{JsValue, Json}
import BaseIntegrationTestConstants._

object ReportDeadlinesIntegrationTestConstants {
  def successResponse(obligationsModel: ReportDeadlinesModel): JsValue = {
    Json.toJson(obligationsModel)
  }

  def emptyResponse(): JsValue = Json.arr()

  def failureResponse(code: String, reason: String): JsValue = Json.obj(
    "code" -> code,
    "reason" -> reason
  )

  val deadlineStart1 = "2017-01-01"
  val deadlineEnd1 = "2017-03-31"
  val deadlineStart2 = "2017-04-01"
  val deadlineEnd2 = "2017-06-30"
  val deadlineStart3 = "2016-06-01"
  val deadlineEnd3 = "2017-06-30"
  val deadlineStart4 = "2017-07-01"
  val deadlineEnd4 = "2017-09-30"
  val deadlineStart5 = "2017-10-01"
  val deadlineEnd5 = "2018-01-31"
  val deadlineStart6 = "2017-11-01"
  val deadlineEnd6 = "2018-02-01"

  val testPropertyId = "1234"

  def multipleReportDeadlinesDataSuccessModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      start = deadlineStart1,
      end = deadlineEnd1,
      due = LocalDate.now().minusDays(128),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), ReportDeadlineModel(
      start = deadlineStart2,
      end = deadlineEnd2,
      due = LocalDate.now().minusDays(36),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), ReportDeadlineModel(
      start = deadlineStart3,
      end = deadlineEnd3,
      due = LocalDate.now().minusDays(36),
      obligationType = "EOPS",
      dateReceived = None,
      periodKey = "periodKey"
    ), ReportDeadlineModel(
      start = deadlineStart4,
      end = deadlineEnd4,
      due = LocalDate.now().plusDays(30),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ),ReportDeadlineModel(
      start = deadlineStart5,
      end = deadlineEnd5,
      due = LocalDate.now().plusDays(146),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    ), ReportDeadlineModel(
      start = deadlineStart6,
      end = deadlineEnd6,
      due = LocalDate.now().plusDays(174),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val singleObligationStart = "2017-04-06"
  val singleObligationEndQuarter = "2017-05-05"
  val singleObligationEnd = "2017-07-05"
  val singleObligationDue = "2018-01-01"

  val overdueDate: LocalDate = LocalDate.now().minusDays(1)

  def singleObligationQuarterlyReturnModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      due = singleObligationDue,
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val veryOverdueDate: LocalDate = LocalDate.of(2017, 5, 5)

  val singleObligationCrystallisationModel: ReportDeadlinesModel = ReportDeadlinesModel(testMtditid, List(
    ReportDeadlineModel(
      start = singleObligationStart,
      end = singleObligationEndQuarter,
      obligationType = "Crystallised",
      due = veryOverdueDate,
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  def singleObligationOverdueModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      start = singleObligationStart,
      end = singleObligationEnd,
      obligationType = "Quarterly",
      due = overdueDate,
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))

  val singleObligationStartEOPs = "2017-04-06"
  val singleObligationEndEOPs = "2018-07-05"
  val singleObligationDueEOPs = "2018-01-01"

  val singleObligationEOPSPropertyModel = ReportDeadlinesModel(testPropertyId, List(
    ReportDeadlineModel(
      singleObligationStartEOPs,
      singleObligationEndEOPs,
      singleObligationDueEOPs,
      "EOPS",
      dateReceived = None,
      "EOPS"
    )
  ))

  def noObligationsModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(

  ))

  def singleObligationQuarterlyModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      singleObligationStart,
      singleObligationEnd,
      singleObligationDue,
      "Quarterly",
      dateReceived = None,
      "#001"
    )
  ))

  def singleObligationPlusYearOpenModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      start = "2017-04-06",
      end = "2017-07-05",
      due = LocalDate.now().plusYears(1),
      obligationType = "Quarterly",
      dateReceived = None,
      periodKey = "periodKey"
    )
  ))


  def SEIncomeSourceEOPSModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List(
    ReportDeadlineModel(
      start = "2017-04-06",
      end = "2018-04-05",
      due = "2018-01-31",
      "EOPS",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  val crystallisedEOPSModel = ReportDeadlinesModel(testMtditid, List(
    ReportDeadlineModel(
      start = "2017-04-06",
      end = "2018-04-05",
      due = "2019-01-31",
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  val crystallisedEOPSModelMulti = ReportDeadlinesModel(testMtditid, List(
    ReportDeadlineModel(
      start = "2018-04-06",
      end = "2019-04-05",
      due = "2020-01-31",
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    ),
    ReportDeadlineModel(
      start = "2017-04-06",
      end = "2018-04-05",
      due = "2019-01-31",
      "Crystallised",
      dateReceived = None,
      periodKey = "#003"
    )
  ))

  def emptyModel(incomeId: String): ReportDeadlinesModel = ReportDeadlinesModel(incomeId, List())
}