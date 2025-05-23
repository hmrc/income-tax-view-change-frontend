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

import play.api.libs.json.{JsValue, Json}
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditidAgent, testNinoAgent, testSaUtr}
import testUtils.TestSupport

class EnterClientUTRAuditModelSpec extends TestSupport {

  val transactionName = enums.TransactionName.EnterClientUTR.name
  val auditType = enums.AuditType.EnterClientUTR.name

  def getEnterClientUTRAuditModel(isSuccessful: Boolean, isSupportingAgent: Option[Boolean]): EnterClientUTRAuditModel = {
    EnterClientUTRAuditModel(isSuccessful = isSuccessful, nino = testNinoAgent, mtditid = testMtditidAgent,
      arn = Some(testArn), saUtr = testSaUtr, credId = Some(testCredId), isSupportingAgent = isSupportingAgent)
  }


  val detailsAuditDataSuccess: Boolean => JsValue = isSupportingAgent => Json.parse(
    s"""{
      |    "nino": "AA111111A",
      |    "mtditid": "XAIT00000000015",
      |    "agentReferenceNumber": "XAIT0000123456",
      |    "saUtr": "1234567890",
      |    "credId": "testCredId",
      |    "outcome": {
      |        "isSuccessful": true
      |    },
      |    "userType": "Agent",
      |    "isSupportingAgent": ${isSupportingAgent.toString}
      |}""".stripMargin)

  val detailsAuditDataFailure = Json.parse(
    """{
      |    "nino": "AA111111A",
      |    "mtditid": "XAIT00000000015",
      |    "agentReferenceNumber": "XAIT0000123456",
      |    "saUtr": "1234567890",
      |    "credId": "testCredId",
      |    "outcome": {
      |        "isSuccessful": false,
      |        "failureCategory": "API_FAILURE",
      |        "failureReason": "API returned error - unable to login agent"
      |    },
      |    "userType": "Agent"
      |}""".stripMargin)


  "EnterClientUTRAuditModel" should {
    s"have the correct transaction name of - $transactionName on successful" in {
      getEnterClientUTRAuditModel(true, Some(false)).transactionName shouldBe transactionName
    }

    s"have the correct transaction name of - $transactionName on failure" in {
      getEnterClientUTRAuditModel(false, Some(false)).transactionName shouldBe transactionName
    }

    s"have the correct audit event type of - $auditType on successful" in {
      getEnterClientUTRAuditModel(true, Some(false)).auditType shouldBe auditType
    }

    s"have the correct audit event type of - $auditType on failure" in {
      getEnterClientUTRAuditModel(false, Some(false)).auditType shouldBe auditType
    }

  }

  "have the correct detail for the audit event" when {
    "user is an primary agent and api returns success" in {
      getEnterClientUTRAuditModel(true, Some(false)).detail shouldBe detailsAuditDataSuccess(false)
    }

    "user is an supporting agent and api returns success" in {
      getEnterClientUTRAuditModel(true, Some(true)).detail shouldBe detailsAuditDataSuccess(true)
    }

    "user is an agent and api returns failure" in {
      getEnterClientUTRAuditModel(false, None).detail shouldBe detailsAuditDataFailure
    }
  }
}