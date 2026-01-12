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
import forms.IncomeSourcesFormsSpec.commonAuditDetails
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import testConstants.ANewCreditAndRefundModel
import testConstants.BaseTestConstants._
import testConstants.CreditAndRefundConstants.documentDetailWithDueDateFinancialDetailListModel
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class ClaimARefundAuditModelSpec extends AnyWordSpecLike {

  val transactionName: String = "claim-a-refund"
  val auditType: String = "ClaimARefundResponse"
  val balanceDetailsFull: BalanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, balanceNotDueIn30Days = 0, totalBalance = 0,
    totalCreditAvailableForRepayment = Some(-7600.00), allocatedCredit = Some(0), allocatedCreditForFutureCharges = Some(0), totalCredit = Some(2.00),
    firstPendingAmountRequested = Some(-100.00), secondPendingAmountRequested = Some(-150.00), None)
  val balanceDetailsMin: BalanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, balanceNotDueIn30Days = 0, totalBalance = 0,
    totalCreditAvailableForRepayment = None, allocatedCredit = None, allocatedCreditForFutureCharges = None, totalCredit = None, firstPendingAmountRequested = None, secondPendingAmountRequested = None, None)

  val creditDocuments: List[(DocumentDetailWithDueDate, FinancialDetail)] = List(
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-100), originalAmount = BigDecimal(-100), mainType = "SA Balancing Charge Credit", mainTransaction = "4905"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1000), originalAmount = BigDecimal(-1000), mainType = "ITSA Infml Dschrg Cntrct Sett", mainTransaction = "4018"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1100), originalAmount = BigDecimal(-1100), mainType = "ITSA NPS Overpayment", mainTransaction = "4012"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1200), originalAmount = BigDecimal(-1200), mainType = "ITSA Cutover Credits", mainTransaction = "6110"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1300), originalAmount = BigDecimal(-1300), mainType = "ITSA Cutover Credits", mainTransaction = "6110"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1350), originalAmount = BigDecimal(-1350), mainType = "SA Repayment Supplement Credit", mainTransaction = "6020"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1375), originalAmount = BigDecimal(-1375), mainType = "SA Repayment Supplement Credit", mainTransaction = "6020"),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1400), originalAmount = BigDecimal(-1400), paymentLot = Some("paymentLot")),
    documentDetailWithDueDateFinancialDetailListModel(outstandingAmount = BigDecimal(-1500), originalAmount = BigDecimal(-1500), paymentLot = Some("paymentLot"))
  )

  val model = ANewCreditAndRefundModel()
    .withAvailableCredit(7600.0)
    .withPoaOneReconciliationCredit(LocalDate.of(2019, 5, 15), 800.0)
    .withPoaTwoReconciliationCredit(LocalDate.of(2019, 5, 15), 500.0)
    .withITSAReturnAmendmentCredit(LocalDate.of(2019, 5, 15), 400.0)
    .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 100.0)
    .withMfaCredit(LocalDate.of(2019, 5, 15), 1000.0)
    .withMfaCredit(LocalDate.of(2019, 5, 15), 1100.0)
    .withCutoverCredit(LocalDate.of(2019, 5, 15), 1200.0)
    .withCutoverCredit(LocalDate.of(2019, 5, 15), 1300.0)
    .withRepaymentInterest(LocalDate.of(2019, 5, 15), 1350.0)
    .withRepaymentInterest(LocalDate.of(2019, 5, 15), 1375.0)
    .withPayment(LocalDate.of(2019, 5, 15), 1400.0)
    .withPayment(LocalDate.of(2019, 5, 15), 1500.0)
    .withFirstRefund(100.0)
    .withSecondRefund(150.0)
    .get()

  def claimARefundAuditFull(user: MtdItUser[_] = testMtdItUser): ClaimARefundAuditModel =
    ClaimARefundAuditModel(model)(user)

  def claimARefundAuditMin(user: MtdItUser[_] = testMtdItUser): ClaimARefundAuditModel =
    ClaimARefundAuditModel(ANewCreditAndRefundModel().get())(user)

  "ClaimARefundAuditModel" should {
    s"have the correct transaction name of '$transactionName'" in {
      claimARefundAuditFull().transactionName shouldBe transactionName
    }
    s"have the correct audit event type of '$auditType'" in {
      claimARefundAuditFull().auditType shouldBe auditType
    }
    s"return a full audit event correctly with POA 1/2 Reconciliation Credit, ITSA Return Amendment Credit, MFA Credits, Cutover Credits, Payments and Refunds, Balancing Charge Credit & Repayment Supplement Credit" in {
      claimARefundAuditFull().detail shouldBe commonAuditDetails(Individual) ++ Json.obj(
        "creditOnAccount" -> 7600,
        "creditDocuments" ->
          Json.arr(
            Json.obj("description" -> "First payment on account: credit from your tax return", "amount" -> 800),
            Json.obj("description" -> "Second payment on account: credit from your tax return", "amount" -> 500),
            Json.obj("description" -> "Credit from your amended tax return", "amount" -> 400),
            Json.obj("description" -> "Balancing charge credit", "amount" -> 100),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1000),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1100),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1200),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1300),
            Json.obj("description" -> "Credit from repayment interest - 2019 to 2020 tax year", "amount" -> 1350),
            Json.obj("description" -> "Credit from repayment interest - 2019 to 2020 tax year", "amount" -> 1375),
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
      claimARefundAuditMin().detail shouldBe commonAuditDetails(Individual) ++ Json.obj(
        "creditOnAccount" -> 0,
        "creditDocuments" ->
          Json.arr(),
        "refundDocuments" ->
          Json.arr())
    }
    s"return a full audit event for an agent user correctly with POA 1/2 Reconciliation Credit, ITSA Return Amendment Credit, MFA Credits, Cutover Credits, Payments and Refunds & Repayment Supplement Credit" in {
      claimARefundAuditFull(testMtdItAgentUser).detail shouldBe commonAuditDetails(Agent) ++ Json.obj(
        "creditOnAccount" -> 7600,
        "creditDocuments" ->
          Json.arr(
            Json.obj("description" -> "First payment on account: credit from your tax return", "amount" -> 800),
            Json.obj("description" -> "Second payment on account: credit from your tax return", "amount" -> 500),
            Json.obj("description" -> "Credit from your amended tax return", "amount" -> 400),
            Json.obj("description" -> "Balancing charge credit", "amount" -> 100),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1000),
            Json.obj("description" -> "Credit from HMRC adjustment", "amount" -> 1100),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1200),
            Json.obj("description" -> "Credit from an earlier tax year", "amount" -> 1300),
            Json.obj("description" -> "Credit from repayment interest - 2019 to 2020 tax year", "amount" -> 1350),
            Json.obj("description" -> "Credit from repayment interest - 2019 to 2020 tax year", "amount" -> 1375),
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
      claimARefundAuditMin(testMtdItAgentUser).detail shouldBe commonAuditDetails(Agent) ++ Json.obj(
        "creditOnAccount" -> 0,
        "creditDocuments" ->
          Json.arr(),
        "refundDocuments" ->
          Json.arr())
    }
  }
}
