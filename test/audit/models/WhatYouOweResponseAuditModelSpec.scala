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

package audit.models

import assets.ChargeListTestConstants._
import assets.BaseTestConstants.{testCredId, testMtditid, testNino, testSaUtr, testUserType}
import play.api.libs.json.Json
import testUtils.TestSupport

class WhatYouOweResponseAuditModelSpec extends TestSupport{

  val transactionName = "what-you-owe"
  val auditEvent = "WhatYouOwe"

  "The WhatYouOweResponseAuditModel" should {

    lazy val testWhatYouOweResponseAuditModel = WhatYouOweResponseAuditModel(None, Some(testUserType),Some(testSaUtr), testNino,
       Some(testCredId), testMtditid, whatYouOweAllData )

    s"Have the correct transaction name of '$transactionName'" in {
      testWhatYouOweResponseAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testWhatYouOweResponseAuditModel.auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testWhatYouOweResponseAuditModel.detail shouldBe Json.obj(
        "agentReferenceNumber" -> "",
        "userType" -> testUserType,
        "saUtr" -> testSaUtr,
        "nationalInsuranceNumber" -> testNino,
        "credId" -> testCredId,
        "mtditid" -> testMtditid,
        "charges" -> Json.arr(
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
}
