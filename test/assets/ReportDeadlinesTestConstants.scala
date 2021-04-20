/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId, testSelfEmploymentId2}
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel}
import play.api.libs.json.{JsValue, Json}

object ReportDeadlinesTestConstants {

  def fakeReportDeadlinesModel(m: ReportDeadlineModel): ReportDeadlineModel = new ReportDeadlineModel(m.start, m.end, m.due, m.obligationType, m.dateReceived, m.periodKey) {
    override def currentTime() = LocalDate.of(2017, 10, 31)
  }

  val testStartDate = LocalDate.of(2017, 7, 1)

  val quarterlyBusinessObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2019, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val overdueObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val openObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#003"
  ))


  val secondQuarterlyObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 10, 1),
    end = LocalDate.of(2017, 10, 31),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val crystallisedObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 10, 1),
    end = LocalDate.of(2018, 10, 30),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))

  val crystallisedObligationTwo = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2018, 10, 1),
    end = LocalDate.of(2019, 10, 30),
    due = LocalDate.of(2020, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))


  val quarterlyObligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(secondQuarterlyObligation, openObligation))

  val reportDeadlinesDataSelfEmploymentSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testSelfEmploymentId, List(overdueObligation, openObligation))

  val reportDeadlinesDataPropertySuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(overdueObligation, openObligation))

  val obligationsDataSelfEmploymentOnlySuccessModel: ObligationsModel = ObligationsModel(List(reportDeadlinesDataSelfEmploymentSuccessModel))

  val obligationsDataPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(List(reportDeadlinesDataPropertySuccessModel))

  val previousObligationOne: ReportDeadlineModel = ReportDeadlineModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 5, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 4, 1)),
    "#001"
  )

  val previousObligationTwo: ReportDeadlineModel = ReportDeadlineModel(
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 7, 1),
    LocalDate.of(2017, 8, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 7, 1)),
    "#002"
  )

  val previousObligationThree: ReportDeadlineModel = ReportDeadlineModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 1, 1),
    "EOPS",
    Some(LocalDate.of(2018, 1, 30)),
    "EOPS"
  )

  val previousObligationFour: ReportDeadlineModel = ReportDeadlineModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 30),
    "EOPS",
    Some(LocalDate.of(2019, 1, 30)),
    "EOPS"
  )


  val previousObligationFive: ReportDeadlineModel = ReportDeadlineModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 31),
    "Crystallised",
    Some(LocalDate.of(2018, 1, 31)),
    "Crystallised "
  )


  val previousObligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(previousObligationTwo, previousObligationOne))
  val previousObligationsEOPSDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(previousObligationThree, previousObligationFour))
  val previousObligationsCrystallisedSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(previousObligationFive))

  val reportDeadlineOverdueJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-30",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#002"
  )
  val reportDeadlineOpenJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-31",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#003"
  )
  val obligationsDataSuccessJson = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      reportDeadlineOverdueJson,
      reportDeadlineOpenJson
    )
  )

  val reportDeadlinesDataFromJson: JsValue = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      reportDeadlineOverdueJson,
      reportDeadlineOpenJson
    )
  )

  val obligationsDataFromJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      reportDeadlinesDataFromJson
    )
  )

  val overdueEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 1),
    obligationType = "EOPS",
    dateReceived = None,
    periodKey = "#002"
  ))
  val openEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "EOPS",
    dateReceived = None,
    periodKey = "#003"
  ))

  val openCrystObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2019, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))


  val obligationsEOPSDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(overdueEOPSObligation, openEOPSObligation))

  val obligationsCrystallisedSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testMtditid, List(crystallisedObligation))

  val obligationsAllDeadlinesSuccessModel: ObligationsModel = ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel,
    obligationsEOPSDataSuccessModel, obligationsCrystallisedSuccessModel))

  val obligationsCrystallisedEmptySuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testNino, List())

  val obligationsPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsEOPSDataSuccessModel, obligationsCrystallisedEmptySuccessModel))

  val obligationsCrystallisedOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsCrystallisedSuccessModel))

  val emptyObligationsSuccessModel: ObligationsModel = ObligationsModel(Seq())

  val reportDeadlineEOPSOverdueJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end" -> "2018-04-05",
    "due" -> "2017-07-31",
    "periodKey" -> "#002"
  )
  val reportDeadlineEOPSOpenJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end" -> "2018-04-05",
    "due" -> "2018-05-01",
    "periodKey" -> "#003"
  )
  val obligationsEOPSDataSuccessJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      reportDeadlineEOPSOverdueJson,
      reportDeadlineEOPSOpenJson
    )
  )


  val twoObligationsSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(testPropertyIncomeId, List(overdueObligation, openEOPSObligation))

  val crystallisedDeadlineSuccess: ReportDeadlinesModel = ReportDeadlinesModel(testMtditid, List(openCrystObligation))

  val obligationsDataErrorModel = ReportDeadlinesErrorModel(testErrorStatus, testErrorMessage)
  val obligations4xxDataErrorModel = ReportDeadlinesErrorModel(404, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val obligationsDataAllMinusCrystallisedSuccessModel: ObligationsModel = ObligationsModel(List(
    reportDeadlinesDataPropertySuccessModel,
    reportDeadlinesDataSelfEmploymentSuccessModel,
    ReportDeadlinesModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))

  val obligationsDataAllDataSuccessModel: ObligationsModel = ObligationsModel(List(
    reportDeadlinesDataPropertySuccessModel,
    reportDeadlinesDataSelfEmploymentSuccessModel,
    crystallisedDeadlineSuccess,
    ReportDeadlinesModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))
}
