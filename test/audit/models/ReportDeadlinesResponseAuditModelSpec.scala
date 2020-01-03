/*
 * Copyright 2020 HM Revenue & Customs
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

package audit.models

import assets.BaseTestConstants._
import assets.ReportDeadlinesTestConstants._
import models.reportDeadlines.ReportDeadlineModel
import play.api.libs.json.{Json, Writes}
import testUtils.TestSupport

class ReportDeadlinesResponseAuditModelSpec extends TestSupport {

  val transactionName = "report-deadlines-response"
  val auditEvent = "reportDeadlinesResponse"

  "The ReportDeadlinesResponseAuditModel" when {

    "Supplied with Multiple Obligations" should {

      val deadlines = List(openObligation, openObligation, overdueObligation)
      val testReportDeadlinesResponseAuditModel = ReportDeadlinesResponseAuditModel(
        testMtditid,
        testNino,
        testSelfEmploymentId,
        deadlines
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testReportDeadlinesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testReportDeadlinesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testReportDeadlinesResponseAuditModel.detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "reportDeadlines" -> Json.arr(
            Json.obj(
              "startDate" -> "2017-07-01",
              "endDate" -> "2017-09-30",
              "dueDate" -> "2017-10-31",
              "obligationType" -> "Quarterly",
              "periodKey" -> "#003"
            ),
            Json.obj(
              "startDate" -> "2017-07-01",
              "endDate" -> "2017-09-30",
              "dueDate" -> "2017-10-31",
              "obligationType" -> "Quarterly",
              "periodKey" -> "#003"
            ),
            Json.obj(
              "startDate" -> "2017-07-01",
              "endDate" -> "2017-09-30",
              "dueDate" -> "2017-10-30",
              "obligationType" -> "Quarterly",
              "periodKey" -> "#002"
            )
          )
        )
      }
    }

    "Supplied with a Single Obligation" should {

      val testReportDeadlinesResponseAuditModel = ReportDeadlinesResponseAuditModel(
        testMtditid,
        testNino,
        testSelfEmploymentId,
        List(openObligation)
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testReportDeadlinesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testReportDeadlinesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testReportDeadlinesResponseAuditModel.detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "reportDeadlines" -> Json.arr(
            Json.obj(
              "startDate" -> "2017-07-01",
              "endDate" -> "2017-09-30",
              "dueDate" -> "2017-10-31",
              "obligationType" -> "Quarterly",
              "periodKey" -> "#003"
            )
          )
        )
      }
    }

    "Supplied with no Obligations" should {

      val testReportDeadlinesResponseAuditModel = ReportDeadlinesResponseAuditModel(
        testMtditid,
        testNino,
        testSelfEmploymentId,
        List()
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testReportDeadlinesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testReportDeadlinesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testReportDeadlinesResponseAuditModel.detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "reportDeadlines" -> Json.arr()
        )
      }
    }
  }
}
