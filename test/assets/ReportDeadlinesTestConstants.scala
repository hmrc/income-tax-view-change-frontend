/*
 * Copyright 2018 HM Revenue & Customs
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

import models.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel}
import play.api.libs.json.{JsValue, Json}
import utils.ImplicitDateFormatter
import assets.BaseTestConstants._

object ReportDeadlinesTestConstants extends ImplicitDateFormatter {

  def fakeReportDeadlinesModel(m: ReportDeadlineModel): ReportDeadlineModel = new ReportDeadlineModel(m.start,m.end,m.due,m.met) {
    override def currentTime() = "2017-10-31"
  }

  val receivedObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-01",
    end = "2017-6-30",
    due = "2017-7-31",
    met = true
  ))

  val overdueObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-7-1",
    end = "2017-9-30",
    due = "2017-10-30",
    met = false
  ))

  val openObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-7-1",
    end = "2017-9-30",
    due = "2017-10-31",
    met = false
  ))

  val obligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(receivedObligation, overdueObligation, openObligation))
  val obligationsDataSuccessJson = Json.obj(
    "obligations" -> Json.arr(
      Json.obj(
        "start" -> "2017-04-01",
        "end" -> "2017-06-30",
        "due" -> "2017-07-31",
        "met" -> true
      ),
      Json.obj(
        "start" -> "2017-07-01",
        "end" -> "2017-09-30",
        "due" -> "2017-10-30",
        "met" -> false
      ),
      Json.obj(
        "start" -> "2017-07-01",
        "end" -> "2017-09-30",
        "due" -> "2017-10-31",
        "met" -> false
      )
    )
  )


  val receivedEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2018-05-01",
    met = true
  ))

  val overdueEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2017-10-01",
    met = false
  ))

  val openEOPSObligation: ReportDeadlineModel = fakeReportDeadlinesModel(ReportDeadlineModel(
    start = "2017-04-06",
    end = "2018-04-05",
    due = "2017-10-31",
    met = false
  ))

  val obligationsEOPSDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(receivedEOPSObligation, overdueEOPSObligation, openEOPSObligation))
  val obligationsEOPSDataSuccessJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      Json.obj(
        "start" -> "2017-04-06",
        "end"  -> "2018-04-05",
        "due" -> "2018-05-01",
        "met" -> true
      ),
      Json.obj(
        "start" -> "2017-04-06",
        "end"  -> "2018-04-05",
        "due" -> "2017-07-01",
        "met" -> false
      ),
      Json.obj(
        "start" -> "2017-04-06",
        "end"  -> "2018-04-05",
        "due" -> "2017-07-31",
        "met" -> false
      )
    )
  )

  val obligationsDataErrorModel = ReportDeadlinesErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val reportDeadlineReceivedJson = Json.obj(
    "start" -> "2017-04-01",
    "end" -> "2017-06-30",
    "due" -> "2017-07-31",
    "met" -> true
  )

}
