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

import assets.BaseTestConstants.{testCredId, testMtditid, testNino, testSaUtr, testUserType}
import play.api.libs.json.Json
import testUtils.TestSupport

class AllowanceAndDeductionsRequestAuditModelISpec extends TestSupport {

  val transactionName = "allowances-deductions-details-request"
  val auditEvent = "AllowancesDeductionsDetailsRequest"

  "The AllowanceAndDeductionsRequestAuditModel" should {

    lazy val testAllowanceAndDeductionsRequestAuditModel = AllowanceAndDeductionsRequestAuditModel(testMtditid, testNino,
      Some(testSaUtr), Some(testCredId), Some(testUserType))

    s"Have the correct transaction name of '$transactionName'" in {
      testAllowanceAndDeductionsRequestAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testAllowanceAndDeductionsRequestAuditModel.auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testAllowanceAndDeductionsRequestAuditModel.detail shouldBe Json.obj(
        "mtditid" -> testMtditid,
        "nationalInsuranceNumber" -> testNino,
        "saUtr" -> testSaUtr,
        "credId" -> testCredId,
        "userType" -> testUserType
      )
    }
  }
}
