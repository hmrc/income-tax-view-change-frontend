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


import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, RepaymentItem, RepaymentSupplementItem}
import testUtils.TestSupport
import play.api.libs.json.Json

import java.time.LocalDate

class RefundToTaxPayerResponseAuditModelSpec extends TestSupport {

  val testRepaymentHistory: RepaymentHistoryModel = RepaymentHistoryModel(
    List(RepaymentHistory(
      Some(705.2),
      705.2,
      Some("BACS"),
      Some(12345),
      Some(Vector(
        RepaymentItem(
          Vector(
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.78),
              Some(LocalDate.of(2021, 7, 31)),
              Some(LocalDate.of(2021, 9, 15)),
              Some(2.01)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(2.63),
              Some(LocalDate.of(2021, 9, 15)),
              Some(LocalDate.of(2021, 10, 24)),
              Some(1.76)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.26),
              Some(LocalDate.of(2021, 10, 24)),
              Some(LocalDate.of(2021, 11, 30)),
              Some(2.01))
          )
        )
      )),
      Some(LocalDate.of(2021, 7, 23)), Some(LocalDate.of(2021, 7, 21)), "000000003135")
    )
  )

  val refundToTaxPayerAuditModelIndividualJsonFull = Json.obj(
    "nationalInsuranceNumber" -> "AB123456C",
    "mtditid" -> "XAIT0000123456",
    "saUtr" -> "testSaUtr",
    "credId" -> "testCredId",
    "userType" -> "Individual",
    "estimatedDate" -> "2021-07-23",
    "method" -> "BACS",
    "totalRefund" -> "12345",
    "requestedOn" -> "2021-07-21",
    "refundReference" -> "000000003135",
    "requestedAmount" -> "705.2",
    "refundAmount" -> "705.2",
    "interestAmount" -> 9.67,
    "interestDescription" -> "31 July 2021 to 30 November 2021 at 1.76%")

  val refundToTaxPayerAuditModelAgentJsonFull = Json.obj("nationalInsuranceNumber" -> "AA111111A",
    "mtditid" -> "XAIT00000000015",
    "agentReferenceNumber" -> "XAIT0000123456",
    "saUtr" -> "1234567890",
    "credId" -> "testCredId",
    "userType" -> "Agent",
    "estimatedDate" -> "2021-07-23",
    "method" -> "BACS",
    "totalRefund" -> "12345",
    "requestedOn" -> "2021-07-21",
    "refundReference" -> "000000003135",
    "requestedAmount" -> "705.2",
    "refundAmount" -> "705.2",
    "interestAmount" -> 9.67,
    "interestDescription" -> "31 July 2021 to 30 November 2021 at 1.76%")

  val refundToTaxPayerAuditModelIndividualFull = RefundToTaxPayerResponseAuditModel(testRepaymentHistory)
  val refundToTaxPayerAuditModelAgentFull = {
    RefundToTaxPayerResponseAuditModel(testRepaymentHistory)(agentUserConfirmedClient())
  }
  val transactionName = enums.TransactionName.RefundToTaxPayer.name
  val auditType = enums.AuditType.RefundToTaxPayerResponse.name

  "RefundToTaxPayerAuditModel" should {
    s"have the correct transaction name of '$transactionName'" when {
      "user is an Individual" in {
        refundToTaxPayerAuditModelIndividualFull.transactionName shouldBe transactionName
      }

      "user is an Agent" in {
        refundToTaxPayerAuditModelAgentFull.transactionName shouldBe transactionName
      }
    }

    s"have the correct audit event type of '$auditType'" when {
      "user is an Individual" in {
        refundToTaxPayerAuditModelIndividualFull.auditType shouldBe auditType
      }

      "user is an Agent" in {
        refundToTaxPayerAuditModelAgentFull.auditType shouldBe auditType
      }

    }

    "have the correct detail for the audit event" when {
      "user is an Individual" in {
        refundToTaxPayerAuditModelIndividualFull.detail shouldBe refundToTaxPayerAuditModelIndividualJsonFull
      }

      "user is an Agent" in {
        refundToTaxPayerAuditModelAgentFull.detail shouldBe refundToTaxPayerAuditModelAgentJsonFull
      }

    }
  }
}
