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

package obligations.models.audit

import common.auth.actions.AuthActionsTestData.getMinimalMTDITUser
import common.implicits.Json.*
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import shared.testConstants.NextUpdatesTestConstants.*
import play.api.libs.json.Json
import common.testConstants.BaseTestConstants.*
import common.testUtils.TestSupport
import shared.models.audit.NextUpdatesResponseAuditModel

class NextUpdatesResponseAuditModelSpec extends TestSupport {

  val auditType = "ViewObligationsResponse"

  "The NextUpdatesResponseAuditModel" when {
    val multipleDeadlines = List(openObligation, openObligation, overdueObligation, quarterlyObligation2016)
    val modelWithMultipleDeadlines = 
      NextUpdatesResponseAuditModel(
        testMtdItAgentUser,
        testSelfEmploymentId,
        multipleDeadlines
      )

    "constructed" should {
      s"have the correct audit type of '$auditType'" in {
        modelWithMultipleDeadlines.auditType shouldBe auditType
      }
    }

    "Supplied with Multiple Obligations" should {
      "Have the correct details for the audit event" in {
        modelWithMultipleDeadlines.toJson shouldEqual Json.obj(
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
            ),
            Json.obj(
              "startDate" -> "2016-05-01",
              "endDate" -> "2016-07-30",
              "dueDate" -> "2016-07-30",
              "obligationType" -> "Quarterly",
              "dateReceived" -> "2016-07-30",
              "periodKey" -> "#001"
            ))
          )
      }
    }

    "Supplied with a Single Obligation" should {
      val modelWithOneDeadline = NextUpdatesResponseAuditModel(
        testMtdItAgentUser,
        testSelfEmploymentId,
        List(openObligation)
      )

      "Have the correct details for the audit event" in {
        modelWithOneDeadline.toJson shouldEqual Json.obj(
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
          )
        )
      }
    }

    "Supplied with no Obligations and optional fields" should {

      val modelWithNoDeadlines = NextUpdatesResponseAuditModel(
        getMinimalMTDITUser(None, IncomeSourceDetailsModel(testNino ,testMtditid, None, Nil, Nil, "1")),
        testSelfEmploymentId,
        Nil
      )

      "Have the correct details for the audit event" in {
        modelWithNoDeadlines.toJson shouldEqual Json.obj(
          "mtditid" -> testMtditid,
          "nino" -> testNino,
          "incomeSourceId" -> testSelfEmploymentId,
          "reportDeadlines" -> Json.arr()
        )
      }
    }
  }
}
