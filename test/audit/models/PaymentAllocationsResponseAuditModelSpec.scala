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

import authV2.AuthActionsTestData._
import models.financialDetails.{DocumentDetail, FinancialDetail, SubItem}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class PaymentAllocationsResponseAuditModelSpec extends TestSupport {

  val transactionName = "payment-allocations-response"
  val auditEvent = "PaymentAllocations"
  val poa1IncomeTax: String = messages("paymentAllocation.paymentAllocations.poa1.incomeTax")

  private val fd1 = FinancialDetail(
    taxYear = "2017",
    chargeType = Some("ITSA- POA 1"),
    mainType = Some("SA Payment on Account 1"),
    transactionId = Some("transid2"),
    items = Some(Seq(SubItem(Some(LocalDate.parse("2017-02-28"))), SubItem(Some(LocalDate.parse("2018-02-28")))))
  )
  private val fd2 = FinancialDetail(
    taxYear = "2017",
    chargeType = Some("Cutover Credits"),
    mainType = Some("ITSA Cutover Credits"),
    transactionId = Some("transid2"),
    items = Some(Seq(SubItem(Some(LocalDate.parse("2017-02-28"))), SubItem(Some(LocalDate.parse("2018-02-28")))))
  )

  private val dd1 = DocumentDetail(taxYear = 2017,
    transactionId = "transid2",
    documentDescription = Some("ITSA- POA 1"),
    documentText = Some("documentText"),
    outstandingAmount = 543.32,
    originalAmount = 23456.78,
    documentDate = LocalDate.parse("2018-03-21"))

  private val dd2 = DocumentDetail(taxYear = 2017,
    transactionId = "transid2",
    documentDescription = Some("New Charge"),
    documentText = Some("documentText"),
    outstandingAmount = -543.32,
    originalAmount = -23456.78,
    documentDate = LocalDate.parse("2018-03-21"))
  private val paymentAllocationChargeModel = FinancialDetailsWithDocumentDetailsModel(List(dd1), List(fd1))
  private val paymentAllocationChargeModelCredit = FinancialDetailsWithDocumentDetailsModel(List(dd2), List(fd2))

  private val allocationDetail = AllocationDetail(transactionId = Some("transid2"), from = Some(LocalDate.parse("2017-03-21")), to = Some(LocalDate.parse("2017-03-20")),
    chargeType = Some("ITSA- POA 1"), mainType = Some("SA Payment on Account 1"), amount = Some(12345.67), clearedAmount = Some(12345.67), Some("chargeReference1"))

  private val allocationDetailCredit = AllocationDetail(transactionId = Some("transid2"), from = Some(LocalDate.parse("2017-03-21")), to = Some(LocalDate.parse("2017-03-20")),
    chargeType = Some("ITSA- POA 1"), mainType = Some("SA Payment on Account 1"), amount = Some(12345.67), clearedAmount = Some(12345.67), Some("chargeReference1"))

  private val originalPaymentAllocationWithClearingDate: Seq[AllocationDetailWithClearingDate] =
    Seq(AllocationDetailWithClearingDate(Some(allocationDetail), Some(LocalDate.parse("2017-03-21"))))

  private val originalPaymentAllocationWithClearingDateCredit: Seq[AllocationDetailWithClearingDate] =
    Seq(AllocationDetailWithClearingDate(Some(allocationDetailCredit), Some(LocalDate.parse("2017-03-21"))))


  def paymentAllocationsAuditFull(userType: Option[AffinityGroup] = Some(Agent)): PaymentAllocationsResponseAuditModel = {
    PaymentAllocationsResponseAuditModel(
      mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel(testNino, testMtditid, None, List.empty, List.empty)),
      paymentAllocations = PaymentAllocationViewModel(paymentAllocationChargeModel, originalPaymentAllocationWithClearingDate)
    )
  }

  def paymentAllocationsAuditFullCredit(userType: Option[AffinityGroup] = Some(Agent)): PaymentAllocationsResponseAuditModel = {
    PaymentAllocationsResponseAuditModel(
      mtdItUser = defaultMTDITUser(userType, IncomeSourceDetailsModel(testNino, testMtditid, None, List.empty, List.empty)),
      paymentAllocations = PaymentAllocationViewModel(paymentAllocationChargeModelCredit, originalPaymentAllocationWithClearingDateCredit)
    )
  }

  "The PaymentAllocationsRequestAuditModel with Credit disabled" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentAllocationsAuditFullCredit().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentAllocationsAuditFullCredit().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event with Credit" when {
      "the audit is full" when {
        "the user is an individual" in {
          val expectedAudit = commonAuditDetails(Individual) ++ Json.obj(
            "paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made from earlier tax year",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFullCredit(userType = Some(Individual)).detail shouldBe expectedAudit
        }
        "the user is an agent" in {
          val expectedAudit = commonAuditDetails(Agent) ++ Json.obj(
            "paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made from earlier tax year",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFullCredit(userType = Some(Agent)).detail shouldBe expectedAudit
        }
      }
    }
  }

  "The PaymentAllocationsRequestAuditModel with expected behaviour" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentAllocationsAuditFull().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentAllocationsAuditFull().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" when {
      "the audit is full" when {
        "the user is an individual" in {
          val expectedAudit = commonAuditDetails(Individual) ++ Json.obj("paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made to HMRC",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFull(userType = Some(Individual)).detail shouldBe expectedAudit
        }
        "the user is an agent" in {
          val expectedAudit = commonAuditDetails(Agent) ++ Json.obj(
            "paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made to HMRC",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFull(userType = Some(Agent)).detail shouldBe expectedAudit
        }
      }
    }
  }

  "The PaymentAllocationsRequestAuditModel with Credit FS enabled" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentAllocationsAuditFullCredit().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentAllocationsAuditFullCredit().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event with Credit" when {
      "the audit is full" when {
        "the user is an individual" in {
          val expectedAudit = commonAuditDetails(Individual) ++ Json.obj(
            "paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made from earlier tax year",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFullCredit(userType = Some(Individual)).detail shouldBe expectedAudit
        }
        "the user is an agent" in {
          val expectedAudit = commonAuditDetails(Agent) ++ Json.obj(
            "paymentMadeDate" -> LocalDate.parse("2017-02-28"),
            "paymentMadeAmount" -> 23456.78,
            "paymentType" -> "Payment made from earlier tax year",
            "paymentAllocations" -> Json.arr(
              Json.obj(
                "paymentAllocationDescription" -> poa1IncomeTax,
                "dateAllocated" -> LocalDate.parse("2017-03-21"),
                "amount" -> 12345.67,
                "taxYear" -> "2016 to 2017"
              )
            ),
            "creditOnAccount" -> 543.32
          )
          paymentAllocationsAuditFullCredit(userType = Some(Agent)).detail shouldBe expectedAudit
        }
      }
    }
  }
}

