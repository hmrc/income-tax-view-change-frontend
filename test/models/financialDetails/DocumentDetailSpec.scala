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

package models.financialDetails

import testConstants.FinancialDetailsTestConstants.fullDocumentDetailModel
import testUtils.UnitSpec

class DocumentDetailSpec extends UnitSpec {

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
            documentText = Some("Class 2 National Insurance"), amountCodedOut = Some(0.0)).getChargeTypeKey(true) shouldBe "class2Nic.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Class 2 National Insurance"), amountCodedOut = Some(0.0)).getChargeTypeKey(true) shouldBe "class2Nic.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Class 2 National Insurance"), amountCodedOut = Some(0.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is class 2 nics" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Class 2 National Insurance"), amountCodedOut = Some(0.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
      "return coding out text or BCD charge" when {
        "when document description is TRM New Charge, coding out is enabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("PAYE Self Assessment"), amountCodedOut = Some(50.0)).getChargeTypeKey(true) shouldBe "codingOut.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("PAYE Self Assessment"), amountCodedOut = Some(50.0)).getChargeTypeKey(true) shouldBe "codingOut.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("PAYE Self Assessment"), amountCodedOut = Some(50.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("PAYE Self Assessment"), amountCodedOut = Some(50.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
      "return cancelled paye self assessment text or BCD charge" when {
        "when document description is TRM New Charge, coding out is enabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), amountCodedOut = Some(0.0)).getChargeTypeKey(true) shouldBe "cancelledPayeSelfAssessment.text"
        }
        "when document description is TRM Amend Charge, coding out is enabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), amountCodedOut = Some(0.0)).getChargeTypeKey(true) shouldBe "cancelledPayeSelfAssessment.text"
        }
        "when document description is TRM New Charge, coding out is disabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), amountCodedOut = Some(0.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
        "when document description is TRM Amend Charge, coding out is disabled and is cancelled paye self assessment" in {
          fullDocumentDetailModel.copy(documentDescription = Some("TRM Amend Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), amountCodedOut = Some(0.0)).getChargeTypeKey(false) shouldBe "balancingCharge.text"
        }
      }
    }
  }
}
