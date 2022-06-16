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

package models.financialDetails

import org.scalacheck.Gen
import testConstants.FinancialDetailsTestConstants.fullDocumentDetailModel
import testUtils.UnitSpec

import scala.util.Random

class DocumentDetailSpec extends UnitSpec {

  val validMfaCreditDescription = Gen.oneOf(
    "ITSA Overpayment Relief",
    "ITSA Standalone Claim",
    "ITSA Averaging Adjustment",
    "ITSA Literary Artistic Spread",
    "ITSA Loss Relief Claim",
    "ITSA Post Cessation Claim",
    "ITSA Pension Relief Claim",
    "ITSA PAYE in year Repayment",
    "ITSA NPS Overpayment",
    "ITSA In year Rept pension schm",
    "ITSA Increase in PAYE Credit",
    "ITSA CIS Non Resident Subbie",
    "ITSA CIS Incorrect Deductions",
    "ITSA Stand Alone Assessment",
    "ITSA Infml Dschrg Cntrct Sett",
    "ITSA Third Party Rept - FIS",
    "ITSA CGT Adjustments",
    "ITSA EIS Carry Back Claims",
    "ITSA Calc Error Correction",
    "ITSA Misc Credit"
  )

  "DocumentDetail" when {

    "calling predicate hasAccruingInterest" should {

      "return true" when {
        "interestOutstandingAmount exists and latePaymentInterestAmount is a non-negative amount or does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(0))
            .hasAccruingInterest shouldBe true

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(-1))
            .hasAccruingInterest shouldBe true

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = None)
            .hasAccruingInterest shouldBe true
        }
      }

      "return false" when {
        "interestOutstandingAmount exists and latePaymentInterestAmount is a positive amount" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(2))
            .hasAccruingInterest shouldBe false
        }

        "interestOutstandingAmount does not exist and latePaymentInterestAmount is a non-negative amount or does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = Some(0))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = Some(-1))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = None)
            .hasAccruingInterest shouldBe false
        }
      }

    }

    "originalAmountIsNotZeroOrNegative" should {
      "return false" when {
        "original amount is zero" in {
          fullDocumentDetailModel.copy(originalAmount = Some(0)).originalAmountIsNotZeroOrNegative shouldBe false
        }
        "original amount is negative" in {
          fullDocumentDetailModel.copy(originalAmount = Some(-20)).originalAmountIsNotZeroOrNegative shouldBe false
        }
      }
      "return true" when {
        "original amount is not present" in {
          fullDocumentDetailModel.copy(originalAmount = None).originalAmountIsNotZeroOrNegative shouldBe true
        }
        "original amount is positive" in {
          fullDocumentDetailModel.copy(originalAmount = Some(20)).originalAmountIsNotZeroOrNegative shouldBe true
        }
      }
    }

    "getChargeTypeKey" should {
      "return POA1" when {
        "when document description is ITSA- POA 1" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA- POA 1")).getChargeTypeKey(false) shouldBe "paymentOnAccount1.text"

        }
      }
      "return POA2" when {
        "when document description is ITSA - POA 2" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA - POA 2")).getChargeTypeKey(false) shouldBe "paymentOnAccount2.text"

        }
      }
      "return unknown charge" when {
        "when document description is ITSA- XYZ" in {
          fullDocumentDetailModel.copy(documentDescription = Some("ITSA- XYZ")).getChargeTypeKey(false) shouldBe "unknownCharge"
        }
      }
      "return BCD" when {
        "when document description is TRM New Charge, coding out is disabled and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM New Charge, coding out is enabled and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge")).getChargeTypeKey(true) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is not coding out" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge")).getChargeTypeKey(true) shouldBe "balancingCharge.text"
        }
      }
      "return class 2 nics or BCD charge" when {
        "when document description is TRM New Charge, coding out is enabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Class 2 National Insurance")).getChargeTypeKey(true) shouldBe "class2Nic.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Class 2 National Insurance")).getChargeTypeKey(true) shouldBe "class2Nic.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Class 2 National Insurance")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Class 2 National Insurance")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
      "return coding out text or BCD charge" when {
        "when document description is TRM New Charge, coding out is enabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("PAYE Self Assessment")).getChargeTypeKey(true) shouldBe "codingOut.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("PAYE Self Assessment")).getChargeTypeKey(true) shouldBe "codingOut.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("PAYE Self Assessment")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("PAYE Self Assessment")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
      "return cancelled paye self assessment text or BCD charge" when {
        "when document description is TRM New Charge, coding out is enabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment")).getChargeTypeKey(true) shouldBe "cancelledPayeSelfAssessment.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Cancelled PAYE Self Assessment")).getChargeTypeKey(true) shouldBe "cancelledPayeSelfAssessment.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Cancelled PAYE Self Assessment")).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
    }

    "deriving the credit value" should {
      "produce a credit value" when {
        "there is an original amount, it is negative and there is no payment Id or lot item" in {
          fullDocumentDetailModel.copy(originalAmount = Some(BigDecimal(-10.00)), paymentLot = None,
            paymentLotItem = None).credit shouldBe Some(BigDecimal(10.00))
        }
      }

      "produce no value" when {
        "there is no original amount" in {
          fullDocumentDetailModel.copy(originalAmount = None, paymentLot = None,
            paymentLotItem = None).credit shouldBe None
        }

        "there is a payment Id and lot Item" in {
          fullDocumentDetailModel.copy(originalAmount = Some(BigDecimal(-10.00)), paymentLot = Some("1"),
            paymentLotItem = Some("1")).credit shouldBe None
        }

        "the outstanding amount is not negative" in {
          fullDocumentDetailModel.copy(originalAmount = Some(BigDecimal(10.00)), paymentLot = None,
            paymentLotItem = None).credit shouldBe None
        }
      }
    }

    "deriving the paymentOrChargeCredit value" should {
      "produce a credit value" when {
        "there is an outstanding amount and it is negative" in {
          fullDocumentDetailModel.copy(outstandingAmount = Some(BigDecimal(-10.00))).paymentOrChargeCredit shouldBe Some(BigDecimal(10.00))
        }
      }

      "produce no value" when {
        "there is no outstanding amount" in {
          fullDocumentDetailModel.copy(outstandingAmount = None, paymentLot = None,
            paymentLotItem = None).paymentOrChargeCredit shouldBe None
        }

        "the outstanding amount is not negative" in {
          fullDocumentDetailModel.copy(outstandingAmount = Some(BigDecimal(10.00)), paymentLot = None,
            paymentLotItem = None).paymentOrChargeCredit shouldBe None
        }
      }
    }

    "validMFACredit" should {

      "return false" when {
        "documentDescription is 'ITSA- POA 1'" in {
          fullDocumentDetailModel.validMFACreditDescription() shouldBe false
        }
        "documentDescription is '~random alpha-numeric string'" in {
          fullDocumentDetailModel
            .copy(documentDescription = Some(Random.alphanumeric.take(20).mkString("")))
            .validMFACreditDescription() shouldBe false
        }
      }
      "return true" when {
        val documentDetail: String = validMfaCreditDescription.sample.getOrElse("ITSA Misc Credit")
        s"documentDescription is '$documentDetail'" in {
          fullDocumentDetailModel
            .copy(documentDescription = Some(documentDetail))
            .validMFACreditDescription() shouldBe true
        }


      }

    }
  }
}
