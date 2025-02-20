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

import authV2.AuthActionsTestData.defaultAuthorisedAndEnrolledRequest
import enums.MTDSupportingAgent
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testArn, testClientNameString, testCredId, testMtditid, testNino, testSaUtr}
import testUtils.TestSupport

class AccessDeniedForSupportingAgentsModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.AccessDeniedForSupportingAgent.name
  val auditType = enums.AuditType.AccessDeniedForSupportingAgent.name


  val expectedAuditDetails: JsValue = Json.obj("mtditid" -> testMtditid,
    "agentReferenceNumber" -> testArn,
    "saUtr" -> testSaUtr,
    "userType" -> "Agent",
    "isSupportingAgent" -> true,
    "credId" -> testCredId,
    "nino" -> testNino,
    "clientName" -> testClientNameString)


  "AccessDeniedForSupportingAgentAuditModel" should {
    val user = defaultAuthorisedAndEnrolledRequest(MTDSupportingAgent, FakeRequest())
    val audit = AccessDeniedForSupportingAgentAuditModel(user)
    s"have the correct transaction name of - $transactionName on successful" in {
      audit.transactionName shouldBe transactionName
    }

    s"have the correct audit event type of - $auditType on successful" in {
      audit.auditType shouldBe auditType
    }

  "have the correct detail for the audit event" in {
      audit.detail shouldBe expectedAuditDetails
    }
  }
}