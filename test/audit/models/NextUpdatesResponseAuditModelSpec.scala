/*
 * Copyright 2023 HM Revenue & Customs
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

import authV2.AuthActionsTestData.getMinimalMTDITUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json.Json
import testConstants.BaseTestConstants._
import testConstants.NextUpdatesTestConstants._
import testUtils.TestSupport

class NextUpdatesResponseAuditModelSpec extends TestSupport {

  val transactionName: String = enums.TransactionName.ViewObligationsResponse
  val auditEvent: String = enums.AuditType.ViewObligationsResponse

  "The NextUpdatesResponseAuditModel" when {

    "Supplied with Multiple Obligations" should {

      val deadlines = List(openObligation, openObligation, overdueObligation)
      val testNextUpdatesResponseAuditModel = NextUpdatesResponseAuditModel(
        testMtdItAgentUser,
        testSelfEmploymentId,
        deadlines
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testNextUpdatesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testNextUpdatesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        assertJsonEquals(testNextUpdatesResponseAuditModel.detail, Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "saUtr" -> testSaUtr,
          "credId" -> testCredId,
          "userType" -> "Agent",
          "agentReferenceNumber" -> testArn,
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
            ))
          )
        )
      }
    }

    "Supplied with a Single Obligation" should {

      val testNextUpdatesResponseAuditModel = NextUpdatesResponseAuditModel(
        testMtdItAgentUser,
        testSelfEmploymentId,
        List(openObligation)
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testNextUpdatesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testNextUpdatesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        assertJsonEquals(testNextUpdatesResponseAuditModel.detail, Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "saUtr" -> testSaUtr,
          "credId" -> testCredId,
          "userType" -> "Agent",
          "agentReferenceNumber" -> testArn,
          "reportDeadlines" -> Json.arr(
            Json.obj(
              "startDate" -> "2017-07-01",
              "endDate" -> "2017-09-30",
              "dueDate" -> "2017-10-31",
              "obligationType" -> "Quarterly",
              "periodKey" -> "#003"
            )
          ))
        )
      }
    }

    "Supplied with no Obligations and optional fields" should {

      val testNextUpdatesResponseAuditModel = NextUpdatesResponseAuditModel(
        getMinimalMTDITUser(None, IncomeSourceDetailsModel(testNino ,testMtditid, None, Nil, Nil)),
        testSelfEmploymentId,
        List()
      )

      s"Have the correct transaction name of '$transactionName'" in {
        testNextUpdatesResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testNextUpdatesResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        assertJsonEquals(testNextUpdatesResponseAuditModel.detail, Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "reportDeadlines" -> Json.arr()
        ))
      }
    }
  }
}
