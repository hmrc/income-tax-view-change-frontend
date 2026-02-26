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

package models.financialDetails

import enums.CodingOutType._
import enums.{BalancingCharge, Poa1ReconciliationDebit, Poa2ReconciliationDebit}
import testConstants.FinancialDetailsTestConstants.{dateService, documentDetailBalancingCharge, documentDetailClass2Nic, documentDetailModel, documentDetailPOA2, documentDetailPaye, fullDocumentDetailModel, fullDocumentDetailWithDueDateModel}
import testUtils.UnitSpec

import java.time.LocalDate

class DocumentDetailSpec extends UnitSpec {

  "DocumentDetail" when {

    "calling predicate hasAccruingInterest" should {

      "return true" when {
        "accruingInterestAmount exists and is greater than 0" in {
          fullDocumentDetailModel.copy(accruingInterestAmount = Some(200))
            .hasAccruingInterest shouldBe true
        }
      }

      "return false" when {
        "accruingInterestAmount exists and is a negative amount" in {
          fullDocumentDetailModel.copy(accruingInterestAmount = Some(-200))
            .hasAccruingInterest shouldBe false
        }

        "accruingInterestAmount does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = None, accruingInterestAmount = Some(0))
            .hasAccruingInterest shouldBe false
        }

        "interestOutstandingAmount is zero and accruingInterestAmount is a non-negative amount or does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(0), accruingInterestAmount = Some(0))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(0), accruingInterestAmount = Some(-1))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(0), accruingInterestAmount = None)
            .hasAccruingInterest shouldBe false
        }
      }

    }

    "getDueDate" should {
      val dd1 = DocumentDetail(taxYear = 2017,
        transactionId = "transid2",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        outstandingAmount = 0,
        originalAmount = 0,
        documentDate = LocalDate.parse("2018-03-21"),
        effectiveDateOfPayment = Some(LocalDate.parse("2021-12-01")),
        documentDueDate = Some(LocalDate.parse("2021-12-01")))

      val dd2 = DocumentDetail(taxYear = 2017,
        transactionId = "transid2",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        outstandingAmount = 0,
        originalAmount = 0,
        documentDate = LocalDate.parse("2018-03-21"),
        effectiveDateOfPayment = Some(LocalDate.parse("2021-12-01")),
        documentDueDate = Some(LocalDate.parse("2021-12-01")),
        accruingInterestAmount = Some(100),
        interestEndDate = Some(LocalDate.parse("2022-01-02")))

      "return the right due date" in {
        dd1.getDueDate().get shouldBe LocalDate.parse("2021-12-01")
      }
      "return the right due date if its has positive accruingInterestAmount" in {
        dd2.getDueDate().get shouldBe LocalDate.parse("2022-01-02")
      }
    }

    "getChargeTypeKey" should {

      "return POA1" when {
        "when document description is ITSA- POA 1" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA- POA 1")).getChargeTypeKey shouldBe "paymentOnAccount1.text"

        }
      }
      "return POA2" when {
        "when document description is ITSA - POA 2" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA- POA 2")).getChargeTypeKey shouldBe "paymentOnAccount2.text"

        }
      }
      "return unknown charge" when {
        "when document description is ITSA- XYZ" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA- XYZ")).getChargeTypeKey shouldBe "unknownCharge"
        }
      }
      "return BCD" when {
        "when document description is TRM New Charge and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge")).getChargeTypeKey shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge")).getChargeTypeKey shouldBe "balancingCharge.text"
        }
      }
      "return class 2 nics or BCD charge" when {
        "when document description is TRM New Charge and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CLASS2_NICS)).getChargeTypeKey shouldBe "class2Nic.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some(CODING_OUT_CLASS2_NICS)).getChargeTypeKey shouldBe "class2Nic.text"
        }
      }
      "return coding out text or BCD charge" when {
        "when document description is TRM New Charge and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_ACCEPTED)).getChargeTypeKey shouldBe "codingOut.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some(CODING_OUT_ACCEPTED)).getChargeTypeKey shouldBe "codingOut.text"
        }
      }
      "return cancelled paye self assessment text or BCD charge" when {
        "when document description is TRM New Charge and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CANCELLED)).getChargeTypeKey shouldBe "cancelledPayeSelfAssessment.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some(CODING_OUT_CANCELLED)).getChargeTypeKey shouldBe "cancelledPayeSelfAssessment.text"
        }
      }
      "return POA 1 Reconciliation Debit" when {
        "document description is POA 1 Reconciliation Debit" in {
          fullDocumentDetailModel.copy(documentDescription = Some("POA 1 Reconciliation Debit")).getChargeTypeKey shouldBe "poa1ExtraCharge.text"
        }
      }
      "return POA 2 Reconciliation Debit" when {
        "document description is POA 2 Reconciliation Debit" in {
          fullDocumentDetailModel.copy(documentDescription = Some("POA 2 Reconciliation Debit")).getChargeTypeKey shouldBe "poa2ExtraCharge.text"
        }
      }
      "return Late submission penalty" when {
        "document description is LSP" in {
          fullDocumentDetailModel.copy(documentDescription = Some("LSP")).getChargeTypeKey shouldBe "lateSubmissionPenalty.text"
        }
      }
      "return Balancing charge" when {
        "document description is ITSA BCD" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA BCD")).getChargeTypeKey shouldBe "balancingCharge.text"
        }
      }
      "return First late payment penalty" when {
        "document description is LPP1" in {
          fullDocumentDetailModel.copy(documentDescription = Some("LPP1")).getChargeTypeKey shouldBe "firstLatePaymentPenalty.text"
        }
      }
      "return Second late payment penalty" when {
        "document description is LPP2" in {
          fullDocumentDetailModel.copy(documentDescription = Some("LPP2")).getChargeTypeKey shouldBe "secondLatePaymentPenalty.text"
        }
      }
    }

    "getDocType" should {
      "return Poa1ReconciliationDebit when correct key is passed" in {
        fullDocumentDetailModel.copy(documentDescription = Some("POA 1 Reconciliation Debit")).getDocType shouldBe Poa1ReconciliationDebit
      }
      "return Poa2ReconciliationDebit when correct key is passed" in {
        fullDocumentDetailModel.copy(documentDescription = Some("POA 2 Reconciliation Debit")).getDocType shouldBe Poa2ReconciliationDebit
      }
      "return BalancingCharge when correct key is passed" in {
        fullDocumentDetailModel.copy(documentDescription = Some("ITSA BCD")).getDocType shouldBe BalancingCharge
      }
    }

    "isReviewAndReconcileDebit" should {
      "return false if both values passed are false" in {
        fullDocumentDetailWithDueDateModel.isReviewAndReconcileDebit shouldBe false
      }
    }

    "deriving the credit value" should {
      "produce a credit value" when {
        "there is an original amount, it is negative and there is no payment Id or lot item" in {
          fullDocumentDetailModel.copy(originalAmount = BigDecimal(-10.00), paymentLot = None,
            paymentLotItem = None).credit shouldBe Some(BigDecimal(10.00))
        }
      }

      "produce no value" when {

        "there is a payment Id and lot Item" in {
          fullDocumentDetailModel.copy(originalAmount = BigDecimal(-10.00), paymentLot = Some("1"),
            paymentLotItem = Some("1")).credit shouldBe None
        }

        "the outstanding amount is not negative" in {
          fullDocumentDetailModel.copy(originalAmount = BigDecimal(10.00), paymentLot = None,
            paymentLotItem = None).credit shouldBe None
        }
      }
    }

    "deriving the paymentOrChargeCredit value" should {
      def paymentOrChargeCredit(documentDetail: DocumentDetail):Option[BigDecimal] = {
        documentDetail.outstandingAmount match {
          case _ if documentDetail.outstandingAmount >= 0 => None
          case credit => Some(credit * -1)
        }
      }

      "produce a credit value" when {
        "there is an outstanding amount and it is negative" in {
          val updateDocumentDetails = fullDocumentDetailModel.copy(outstandingAmount = BigDecimal(-10.00))
          val result = paymentOrChargeCredit(updateDocumentDetails)
          result shouldBe Some(BigDecimal(10.00))

        }
      }

      "produce no value" when {
        "the outstanding amount is not negative" in {
          val updatedModel = fullDocumentDetailModel.copy(outstandingAmount = BigDecimal(10.00), paymentLot = None, paymentLotItem = None)
          val result = paymentOrChargeCredit(updatedModel)
          result shouldBe None
        }
      }
    }

    "getChargePaidStatus" should {
      "return correct charge paid status paid for various outstandingAmount" in {
        fullDocumentDetailModel.copy(outstandingAmount = 0).getChargePaidStatus shouldBe "paid"
        fullDocumentDetailModel.copy(outstandingAmount = 50, originalAmount = 100).getChargePaidStatus shouldBe "part-paid"
        fullDocumentDetailModel.copy(outstandingAmount = -50, originalAmount = 100).getChargePaidStatus shouldBe "part-paid"
        fullDocumentDetailModel.copy(outstandingAmount = -50, originalAmount = -100).getChargePaidStatus shouldBe "part-paid"
        fullDocumentDetailModel.copy(outstandingAmount = -50, originalAmount = 50).getChargePaidStatus shouldBe "part-paid"
        fullDocumentDetailModel.copy(outstandingAmount = 300, originalAmount = 300).getChargePaidStatus shouldBe "unpaid"
        fullDocumentDetailModel.copy(outstandingAmount = -300, originalAmount = -300).getChargePaidStatus shouldBe "unpaid"
      }
    }

    "isBalancingCharge" should {
      "return true" when {
        "the charge is Nic2 balancing charge" in {
          documentDetailClass2Nic.documentDetail.isBalancingCharge shouldBe false
        }
        "the charge is balancing charge" in {
          documentDetailBalancingCharge.documentDetail.isBalancingCharge shouldBe true
        }
      }
      "return false" when {
        "the charge is other than balancing charge" in {
          documentDetailPOA2.documentDetail.isBalancingCharge shouldBe false
        }
        "coding out charge" in {
          documentDetailPaye.documentDetail.isBalancingCharge shouldBe false
        }
      }
    }

    "isBalancingChargeZero" should {
      "return true" when {
        "the charge is balancing charge and the original amount is zero" in {
          documentDetailBalancingCharge.documentDetail.copy(originalAmount = BigDecimal(0)).isBalancingChargeZero shouldBe true
        }
      }

      "return false" when {
        "the charge is not balancing charge" in {
          documentDetailPOA2.documentDetail.isBalancingChargeZero shouldBe false
        }
        "coding out is enabled with coding out type charge" in {
          documentDetailClass2Nic.documentDetail.isBalancingChargeZero shouldBe false
        }
      }
    }

    "getBalancingChargeDueDate" should {
      "return LocalDate" when {
        "the originalAmount is not zero" in {
          documentDetailBalancingCharge.documentDetail.getBalancingChargeDueDate() shouldBe Some(LocalDate.of(2019, 5, 15))
        }
      }
      "return None" when {
        "the originalAmount is zero" in {
          documentDetailBalancingCharge.documentDetail
            .copy(originalAmount = BigDecimal(0)).getBalancingChargeDueDate() shouldBe None
        }
      }
    }
  }

  "remainingToPay" should {
    "return outstandingAmount when amount is not paid" in {
      fullDocumentDetailModel.remainingToPay shouldBe BigDecimal(1400)
    }
  }

  "interestRemainingToPay" should {
    "return zero when there is no interest remaining to pay" in {
      fullDocumentDetailModel.copy(interestOutstandingAmount = Some(BigDecimal(0)))
        .interestRemainingToPay shouldBe BigDecimal(0)
    }

    "return an amount when there is interest remaining to pay" in {
      fullDocumentDetailModel.interestRemainingToPay shouldBe BigDecimal(80)
    }

    "return zero when interestOutstandingAmount is not present" in {
      fullDocumentDetailModel.copy(interestOutstandingAmount = None, accruingInterestAmount = None)
        .interestRemainingToPay shouldBe BigDecimal(0)
    }
  }

  "isAccruingInterest" should {
    "false when isReviewAndReconcileDebit is false with outstanding amount unpaid and obligations is still due" in {
      fullDocumentDetailWithDueDateModel.isAccruingInterest shouldBe false
    }
  }

}
