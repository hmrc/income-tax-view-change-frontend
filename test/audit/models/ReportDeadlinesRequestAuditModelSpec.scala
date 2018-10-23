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

package audit.models

import assets.BaseTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testUtils.TestSupport

class ReportDeadlinesRequestAuditModelSpec extends TestSupport {

  val transactionName = "report-deadlines-request"
  val auditEvent = "reportDeadlinesRequest"

  "The ReportDeadlinesRequestAuditModel" should {

    lazy val testReportDeadlinesRequestAuditModel = ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId)

    s"Have the correct transaction name of '$transactionName'" in {
      testReportDeadlinesRequestAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testReportDeadlinesRequestAuditModel.auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testReportDeadlinesRequestAuditModel.detail shouldBe Seq(
        "mtditid" -> testMtditid,
        "nino" -> testNino,
        "incomeSourceId" -> testSelfEmploymentId
      )
    }
  }
}
