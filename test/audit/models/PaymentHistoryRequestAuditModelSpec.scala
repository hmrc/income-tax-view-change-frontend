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
import auth.MtdItUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json.Json
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

class PaymentHistoryRequestAuditModelSpec extends TestSupport {

  val transactionName = "payment-history-request"
  val auditEvent = "PaymentHistoryRequest"

  def paymentHistoryAuditFull(userType: Option[String] = Some("Agent")): PaymentHistoryRequestAuditModel = PaymentHistoryRequestAuditModel(
    mtdItUser = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = userType,
      arn = if (userType.contains("Agent")) Some(testArn) else None
    )
  )

  val paymentHistoryAuditMin: PaymentHistoryRequestAuditModel = PaymentHistoryRequestAuditModel(
    mtdItUser = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = None,
      incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    )
  )

  "The PaymentHistoryRequestAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentHistoryAuditFull().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentHistoryAuditFull().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" when {
      "the audit is full" when {
        "the user is an individual" in {
          paymentHistoryAuditFull(userType = Some("Individual")).detail shouldBe Json.obj(
            "mtditid" -> testMtditid,
            "nationalInsuranceNumber" -> testNino,
            "saUtr" -> testSaUtr,
            "credId" -> testCredId,
            "userType" -> "Individual"
          )
        }
        "the user is an agent" in {
          paymentHistoryAuditFull(userType = Some("Agent")).detail shouldBe Json.obj(
            "mtditid" -> testMtditid,
            "nationalInsuranceNumber" -> testNino,
            "saUtr" -> testSaUtr,
            "credId" -> testCredId,
            "userType" -> "Agent",
            "agentReferenceNumber" -> testArn
          )
        }
      }
      "the audit is empty" in {
        paymentHistoryAuditMin.detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nationalInsuranceNumber" -> testNino,
        )
      }
    }
  }
}

