/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants._
import implicits.ImplicitDateFormatter
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.libs.json.{JsValue, Json}

object ReportDeadlinesTestConstants extends ImplicitDateFormatter {

  def fakeReportDeadlinesModel(m: ReportDeadlineModel): ReportDeadlineModel = new ReportDeadlineModel(m.start,m.end,m.due, m.obligationType, m.periodKey) {
    override def currentTime() = "2017-10-31"
  }

  val quarterlyBusinessObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-7-1",
    end = "2017-9-30",
    due = "2019-10-30",
    obligationType = "Quarterly",
    periodKey = "#002"
  ))

  val overdueObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-7-1",
    end = "2017-9-30",
    due = "2017-10-30",
    obligationType = "Quarterly",
    periodKey = "#002"
  ))

  val openObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-7-1",
    end = "2017-9-30",
    due = "2017-10-31",
    obligationType = "Quarterly",
    periodKey = "#003"
  ))

  val secondQuarterlyObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-10-1",
    end = "2017-12-30",
    due = "2017-10-31",
    obligationType = "Quarterly",
    periodKey = "#002"
  ))


  val quarterlyObligationsDataSuccessModel : ReportDeadlinesModel = ReportDeadlinesModel(List(secondQuarterlyObligation, openObligation))

  val obligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(overdueObligation, openObligation))

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
    "obligations" -> Json.arr(
      reportDeadlineOverdueJson,
      reportDeadlineOpenJson
    )
  )

  val obligationsDataFromJson: JsValue = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      reportDeadlineOverdueJson,
      reportDeadlineOpenJson
    )
  )

  val overdueEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2017-10-01",
    obligationType = "EOPS",
    periodKey = "#002"
  ))
  val openEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2017-10-31",
    obligationType = "EOPS",
    periodKey = "#003"
  ))

  val openCrystObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2019-10-31",
    periodKey = ""
  ))


  val obligationsEOPSDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(overdueEOPSObligation, openEOPSObligation))

  val reportDeadlineEOPSOverdueJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end"  -> "2018-04-05",
    "due" -> "2017-07-31",
    "periodKey" -> "#002"
  )
  val reportDeadlineEOPSOpenJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end"  -> "2018-04-05",
    "due" -> "2018-05-01",
    "periodKey" -> "#003"
  )
  val obligationsEOPSDataSuccessJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      reportDeadlineEOPSOverdueJson,
      reportDeadlineEOPSOpenJson
    )
  )



  val twoObligationsSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(overdueObligation, openEOPSObligation))

  val crystallisedDeadlineSuccess: ReportDeadlinesModel = ReportDeadlinesModel(List(openCrystObligation))

  val obligationsDataErrorModel = ReportDeadlinesErrorModel(testErrorStatus, testErrorMessage)
  val obligations4xxDataErrorModel = ReportDeadlinesErrorModel(404, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

}



