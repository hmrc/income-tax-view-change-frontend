/*
 * Copyright 2022 HM Revenue & Customs
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

import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.libs.json.Json
import testConstants.BaseTestConstants._
import testUtils.TestSupport

class AllowanceAndDeductionsResponseAuditModelISpec extends TestSupport {

  val transactionName = "allowances-deductions-details-response"
  val auditEvent = "AllowancesDeductionsDetailsResponse"

  "The AllowanceAndDeductionsResponseAuditModel with TxmApproved FS enabled" should {

    val testAllowanceAndDeductionsResponseAuditModel = AllowanceAndDeductionsResponseAuditModel(testMtdItAgentUser,
      AllowancesAndDeductionsViewModel(
        personalAllowance = Some(123.12),
        pensionContributions = Some(456.78),
        lossesAppliedToGeneralIncome = Some(1234.12),
        giftOfInvestmentsAndPropertyToCharity = Some(4561.78),
        totalAllowancesAndDeductions = Some(1),
        totalReliefs = Some(3),
        grossAnnuityPayments = Some(1235.12),
        qualifyingLoanInterestFromInvestments = Some(4562.78),
        postCessationTradeReceipts = Some(1236.12),
        paymentsToTradeUnionsForDeathBenefits = Some(4563.78),
        transferredOutAmount = Some(256.78)
      )
    )

    s"Have the correct transaction name of '$transactionName'" in {
      testAllowanceAndDeductionsResponseAuditModel.transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testAllowanceAndDeductionsResponseAuditModel.auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" when {
      "information for the audit is complete" in {
        testAllowanceAndDeductionsResponseAuditModel.detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nationalInsuranceNumber" -> testNino,
          "saUtr" -> testSaUtr,
          "credId" -> testCredId,
          "userType" -> testUserTypeAgent,
          "agentReferenceNumber" -> testArn,
          "personalAllowance" -> 123.12,
          "pensionContributions" -> 456.78,
          "lossRelief" -> 1234.12,
          "giftsToCharity" -> 4561.78,
          "annualPayments" -> 1235.12,
          "qualifyingLoanInterest" -> 4562.78,
          "postCessationTradeReceipts" -> 1236.12,
          "tradeUnionPayments" -> 4563.78,
          "marriageAllowanceTransfer" -> 256.78
        )
      }

      "information for the audit has minimal details" in {
        AllowanceAndDeductionsResponseAuditModel(testMtdItUserMinimal, AllowancesAndDeductionsViewModel()).detail shouldBe Json.obj(
          "mtditid" -> testMtditid,
          "nationalInsuranceNumber" -> testNino
        )
      }
    }
  }
}
