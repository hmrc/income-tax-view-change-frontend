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

import assets.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr}
import auth.MtdItUser
import models.financialDetails.{DocumentDetail, FinancialDetail, SubItem}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.{AllocationDetail, PaymentAllocations}
import play.api.libs.json.Json
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class PaymentAllocationsResponseAuditModelSpec extends TestSupport {

  val transactionName = "payment-allocations-response"
  val auditEvent = "PaymentAllocations"
  private val fd1 = FinancialDetail(
    taxYear = "2017",
    chargeType = Some("ITSA- POA 1"),
    mainType = Some("SA Payment on Account 1"),
    transactionId = Some("transid2"),
    items = Some(Seq(SubItem(Some("2017-02-28")), SubItem(Some("2018-02-28"))))
  )
  private val dd1 = DocumentDetail(taxYear = "2017",
    transactionId = "transid2",
    documentDescription = Some("ITSA- POA 1"),
    outstandingAmount = Some(543.32),
    originalAmount = Some(23456.78),
    documentDate = LocalDate.parse("2018-03-21"))
  private val paymentAllocationChargeModel = FinancialDetailsWithDocumentDetailsModel(List(dd1), List(fd1))
  private val allocationDetail = AllocationDetail(transactionId = Some("transid2"), from = Some("2017-03-21"), to = Some("2017-03-20"),
    chargeType = Some("ITSA- POA 1"), mainType = Some("SA Payment on Account 1"), amount = Some(12345.67), clearedAmount = Some(12345.67), Some("chargeReference1"))

  private val originalPaymentAllocationWithClearingDate: Seq[AllocationDetailWithClearingDate] =
    Seq(AllocationDetailWithClearingDate(Some(allocationDetail), Some("2017-03-21")))

  def paymentAllocationsAuditFull(userType: Option[String] = Some("Agent")): PaymentAllocationsResponseAuditModel = {
    PaymentAllocationsResponseAuditModel(
      mtdItUser = MtdItUser(
        mtditid = testMtditid,
        nino = testNino,
        userName = Some(Name(Some("firstName"), Some("lastName"))),
        incomeSources = IncomeSourceDetailsModel(testMtditid, None, Nil, None),
        saUtr = Some(testSaUtr),
        credId = Some(testCredId),
        userType = userType,
        arn = if (userType.contains("Agent")) Some(testArn) else None
      ),
      paymentAllocations = PaymentAllocationViewModel(paymentAllocationChargeModel, originalPaymentAllocationWithClearingDate)
    )
  }

  "The PaymentAllocationsRequestAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentAllocationsAuditFull().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentAllocationsAuditFull().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" when {
      "the audit is full" when {
        "the user is an individual" in {
          paymentAllocationsAuditFull(userType = Some("Individual")).detail shouldBe Json.obj(
            "mtditid" -> testMtditid,
            "nationalInsuranceNumber" -> testNino,
            "saUtr" -> testSaUtr,
            "credId" -> testCredId,
            "userType" -> "Individual",
            "paymentMadeDate" -> "2017-02-28",
            "paymentMadeAmount" -> 23456.78,
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> "Income Tax for payment on account 1 of 2",
                "dateAllocated" -> "2017-03-21",
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
        }
        "the user is an agent" in {
          paymentAllocationsAuditFull(userType = Some("Agent")).detail shouldBe Json.obj(
            "mtditid" -> testMtditid,
            "nationalInsuranceNumber" -> testNino,
            "saUtr" -> testSaUtr,
            "credId" -> testCredId,
            "userType" -> "Agent",
            "agentReferenceNumber" -> testArn,
            "paymentMadeDate" -> "2017-02-28",
            "paymentMadeAmount" -> 23456.78,
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> "Income Tax for payment on account 1 of 2",
                "dateAllocated" -> "2017-03-21",
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
        }
      }
    }
  }
}

