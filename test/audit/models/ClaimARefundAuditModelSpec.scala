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

import auth.MtdItUser
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import testConstants.BaseTestConstants._
import testConstants.CreditAndRefundConstants.documentDetailWithDueDateFinancialDetailListModel

class ClaimARefundAuditModelSpec extends AnyWordSpecLike {

  val transactionName: String = "claim-a-refund"
  val auditType: String = "ClaimARefundResponse"
  val balanceDetailsFull: BalanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 0,
    availableCredit = Some(-7600.00), firstPendingAmountRequested = Some(-100.00), secondPendingAmountRequested = Some(-150.00), None)
  val balanceDetailsMin: BalanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 0,
    availableCredit = None, firstPendingAmountRequested = None, secondPendingAmountRequested = None, None)

  val creditDocuments: List[(DocumentDetailWithDueDate, FinancialDetail)] = List(
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-100)), originalAmount = Some(BigDecimal(-100)), mainType = "SA Balancing Charge Credit"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1000)), originalAmount = Some(BigDecimal(-1000)), mainType = "ITSA Infml Dschrg Cntrct Sett"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1100)), originalAmount = Some(BigDecimal(-1100)), mainType = "ITSA NPS Overpayment"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1200)), originalAmount = Some(BigDecimal(-1200)), mainType = "ITSA Cutover Credits"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1300)), originalAmount = Some(BigDecimal(-1300)), mainType = "ITSA Cutover Credits"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1400)), originalAmount = Some(BigDecimal(-1400)), paymentLot = Some("paymentLot")),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = Some(BigDecimal(-1500)), originalAmount = Some(BigDecimal(-1500)), paymentLot = Some("paymentLot"))
  )

  def claimARefundAuditFull(user: MtdItUser[_] = testMtdItUser): ClaimARefundAuditModel = ClaimARefundAuditModel(
    balanceDetails = Some(balanceDetailsFull),
    creditDocuments = creditDocuments)(user)

  def claimARefundAuditMin(user: MtdItUser[_] = testMtdItUser): ClaimARefundAuditModel = ClaimARefundAuditModel(
    balanceDetails = Some(balanceDetailsMin),
    creditDocuments = List.empty)(user)

  "ClaimARefundAuditModel" should {
    s"have the correct transaction name of '$transactionName'" in {
      claimARefundAuditFull().transactionName shouldBe transactionName
    }
    s"have the correct audit event type of '$auditType'" in {
      claimARefundAuditFull().auditType shouldBe auditType
    }
    s"return a full audit event correctly with MFA Credits, Cutover Credits, Payments and Refunds, Balancing Charge Credit" in {
      claimARefundAuditFull().detail shouldBe Json.obj(
        "nino" -> testMtdItUser.nino,
        "mtditid" -> testMtdItUser.mtditid,
        "saUtr" -> testMtdItUser.saUtr,
        "credId" -> testMtdItUser.credId,
        "userType" -> testMtdItUser.userType,
        "creditOnAccount" -> 7600,
        "creditDocuments" ->
          Json.arr(
            Json.obj("description" -> "Balancing charge credit", "amount" -> 100),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1000),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1100),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1200),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1300),
            Json.obj("description" -> "Payment made on 15 May 2019", "amount" -> 1400),
            Json.obj("description" -> "Payment made on 15 May 2019", "amount" -> 1500),
          ),
        "refundDocuments" ->
          Json.arr(
            Json.obj("description" -> "Refund in progress", "amount" -> 100),
            Json.obj("description" -> "Refund in progress", "amount" -> 150),
          ))
    }
    "return a minimal audit event correctly" in {
      claimARefundAuditMin().detail shouldBe Json.obj(
        "nino" -> testMtdItUser.nino,
        "mtditid" -> testMtdItUser.mtditid,
        "saUtr" -> testMtdItUser.saUtr,
        "credId" -> testMtdItUser.credId,
        "userType" -> testMtdItUser.userType,
        "creditOnAccount" -> 0,
        "creditDocuments" ->
          Json.arr(),
        "refundDocuments" ->
          Json.arr())
    }
    s"return a full audit event for an agent user correctly with MFA Credits, Cutover Credits, Payments and Refunds" in {
      claimARefundAuditFull(testMtdItAgentUser).detail shouldBe Json.obj(
        "nino" -> testMtdItAgentUser.nino,
        "mtditid" -> testMtdItAgentUser.mtditid,
        "userType" -> testMtdItAgentUser.userType,
        "agentReferenceNumber" -> testMtdItAgentUser.arn,
        "saUtr" -> testMtdItAgentUser.saUtr,
        "credId" -> testMtdItAgentUser.credId,
        "creditOnAccount" -> 7600,
        "creditDocuments" ->
          Json.arr(
            Json.obj("description" -> "Balancing charge credit", "amount" -> 100),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1000),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1100),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1200),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1300),
            Json.obj("description" -> "Payment made on 15 May 2019", "amount" -> 1400),
            Json.obj("description" -> "Payment made on 15 May 2019", "amount" -> 1500),
          ),
        "refundDocuments" ->
          Json.arr(
            Json.obj("description" -> "Refund in progress", "amount" -> 100),
            Json.obj("description" -> "Refund in progress", "amount" -> 150),
          ))
    }
    "return a minimal audit event for an agent user correctly" in {
      claimARefundAuditMin(testMtdItAgentUser).detail shouldBe Json.obj(
        "nino" -> testMtdItAgentUser.nino,
        "mtditid" -> testMtdItAgentUser.mtditid,
        "userType" -> testMtdItAgentUser.userType,
        "agentReferenceNumber" -> testMtdItAgentUser.arn,
        "saUtr" -> testMtdItAgentUser.saUtr,
        "credId" -> testMtdItAgentUser.credId,
        "creditOnAccount" -> 0,
        "creditDocuments" ->
          Json.arr(),
        "refundDocuments" ->
          Json.arr())
    }
  }
}
