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

import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditidAgent, testNinoAgent, testSaUtrId}
import testUtils.TestSupport

class ConfirmClientDetailsAuditModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.ClientDetailsConfirmed.name
  val auditType = enums.AuditType.ClientDetailsConfirmed.name

  def getConfirmClientDetailsAuditModel(): ConfirmClientDetailsAuditModel = {
    ConfirmClientDetailsAuditModel(clientName = "Test User", nino = testNinoAgent, mtditid = testMtditidAgent, arn = testArn, saUtr = testSaUtrId, credId = Some(testCredId))
  }


  val detailsAuditData = Json.parse(
    """{
      |    "nino": "AA111111A",
      |    "mtditid": "XAIT00000000015",
      |    "agentReferenceNumber": "XAIT0000123456",
      |    "saUtr": "1234567890",
      |    "credId": "testCredId",
      |    "userType": "Agent",
      |    "clientName": "Test User"
      |}""".stripMargin)


  "ConfirmClientDetailsAuditModel" should {
    s"have the correct transaction name of - $transactionName" in {
      getConfirmClientDetailsAuditModel().transactionName shouldBe transactionName
    }

    s"have the correct audit event type of - $auditType" in {
      getConfirmClientDetailsAuditModel().auditType shouldBe auditType
    }
  }

  "have the correct detail for the audit event" when {
    "user is an agent" in {
      getConfirmClientDetailsAuditModel().detail shouldBe detailsAuditData
    }
  }
}