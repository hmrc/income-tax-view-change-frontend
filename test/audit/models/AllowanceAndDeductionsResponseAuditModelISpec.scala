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

import assets.BaseTestConstants.{testCredId, testMtditid, testNino, testSaUtr, testUserType}
import play.api.libs.json.Json
import testUtils.TestSupport

class AllowanceAndDeductionsResponseAuditModelISpec extends TestSupport {

  val transactionName = "allowances-deductions-details-response"
  val auditEvent = "AllowancesDeductionsDetailsResponse"

  "The AllowanceAndDeductionsResponseAuditModel" should {

    lazy val testAllowanceAndDeductionsResponseAuditModel = AllowanceAndDeductionsResponseAuditModel(testMtditid, testNino,
      Some(testSaUtr), Some(testCredId), Some(testUserType), Some(BigDecimal(123.12)), Some(BigDecimal(456.78)))

    s"Have the correct transaction name of '$transactionName'" in {
      testAllowanceAndDeductionsResponseAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testAllowanceAndDeductionsResponseAuditModel.auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testAllowanceAndDeductionsResponseAuditModel.detail shouldBe Json.obj(
        "mtditid" -> testMtditid,
        "nationalInsuranceNumber" -> testNino,
        "saUtr" -> testSaUtr,
        "credId" -> testCredId,
        "userType" -> testUserType,
        "personalAllowance" -> "123.12",
        "pensionContributions" -> "456.78"
      )
    }
  }
}
