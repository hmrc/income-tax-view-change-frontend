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

import assets.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr, testUserType}
import play.api.libs.json.Json
import testUtils.TestSupport

class WhatYouOweRequestAuditModelSpec extends TestSupport{


  val transactionName = "what-you-owe-request"
  val auditEvent = "WhatYouOweRequest"

  "The WhatYouOweRequestAuditModel" when {
    "There is an individual" should {

      lazy val testWhatYouOweRequestAuditModel = WhatYouOweRequestAuditModel(None, Some(testUserType), Some(testSaUtr), testNino,
        Some(testCredId), testMtditid)

      s"Have the correct transaction name of '$transactionName'" in {
        testWhatYouOweRequestAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testWhatYouOweRequestAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testWhatYouOweRequestAuditModel.detail shouldBe Json.obj(
          "saUtr" -> testSaUtr,
          "nationalInsuranceNumber" -> testNino,
          "userType" -> testUserType,
          "credId" -> testCredId,
          "mtditid" -> testMtditid
        )
      }
    }
    "There is an agent" should {

      lazy val testWhatYouOweRequestAuditModel = WhatYouOweRequestAuditModel(Some(testArn), Some(testUserType), Some(testSaUtr),
        testNino, Some(testCredId), testMtditid)

      s"Have the correct transaction name of '$transactionName'" in {
        testWhatYouOweRequestAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testWhatYouOweRequestAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testWhatYouOweRequestAuditModel.detail shouldBe Json.obj(
          "agentReferenceNumber" -> testArn,
          "saUtr" -> testSaUtr,
          "nationalInsuranceNumber" -> testNino,
          "userType" -> testUserType,
          "credId" -> testCredId,
          "mtditid" -> testMtditid
        )
      }

    }
  }
}
