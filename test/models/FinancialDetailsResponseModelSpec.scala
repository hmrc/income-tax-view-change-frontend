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

package models

import models.financialDetails._
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testConstants.FinancialDetailsTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate

class FinancialDetailsResponseModelSpec extends UnitSpec with Matchers {

  "The ChargesModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsModel](testValidFinancialDetailsModel) shouldBe testValidFinancialDetailsModelJsonWrites
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsModel](testValidFinancialDetailsModelJsonReads).fold(
        invalid => invalid,
        valid => valid) shouldBe testValidFinancialDetailsModel
    }
  }

  "The ChargesErrorModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModel) shouldBe testFinancialDetailsErrorModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModelJson) shouldBe JsSuccess(testFinancialDetailsErrorModel)
    }
  }

  "dunningLockExists" should {
    val dunningLock = Some("Stand over order")
    val unsupportedLock = Some("Disputed debt")

    def financialDetailsModelWithDunningLock: FinancialDetailsModel = financialDetailsModel(dunningLock = dunningLock)

    def financialDetailsModelWithUnsupportedLock: FinancialDetailsModel = financialDetailsModel(dunningLock = unsupportedLock)

    def financialDetailsModelWithoutDunningLock: FinancialDetailsModel = financialDetailsModel()

    "return true when there is a dunningLock against a charge" in {
      financialDetailsModelWithDunningLock.dunningLockExists(id1040000123) shouldBe true
    }

    "return false when there is an unsupported dunningLock against a charge" in {
      financialDetailsModelWithUnsupportedLock.dunningLockExists(id1040000123) shouldBe false
    }

    "return true when there is not a dunningLock against a charge" in {
      financialDetailsModelWithoutDunningLock.dunningLockExists(id1040000123) shouldBe false
    }
  }

"getAllocationsToCharge" should {
  val chargesWithPayments = chargesWithAllocatedPaymentModel()
  val chargeFinancialDetail = chargesWithPayments.financialDetails.find(_.transactionId.contains(id1040000123)).head
  val allocationsToCharge = chargesWithPayments.getAllocationsToCharge(chargeFinancialDetail).head

  "only return allocations from Payments" in {
    allocationsToCharge.allocations.size shouldBe 1
    val allocation = allocationsToCharge.allocations.head
    allocation.dueDate shouldBe Some(LocalDate.parse("2018-09-07"))
  }
}

  "WhatYouOweService.validChargeType val" should {
    val fdm: FinancialDetailsModel = financialDetailsModel()

    def testValidChargeType(documentDescriptions: List[DocumentDetail], expectedResult: Boolean): Unit = {
      assertResult(expected = expectedResult)(actual = documentDescriptions.forall(dd => fdm.validChargeTypeCondition(dd)))
    }

    "validate a list of documents with valid descriptions" in {
      val documentDescriptions: List[DocumentDetail] = List(documentDetailModel(documentDescription = Some("ITSA- POA 1"))
        , documentDetailModel(documentDescription = Some("ITSA - POA 2"))
        , documentDetailModel(documentDescription = Some("TRM New Charge"))
        , documentDetailModel(documentDescription = Some("TRM Amend Charge"))
      )
      testValidChargeType(documentDescriptions, expectedResult = true)
    }
    "not validate a list of documents with other descriptions" in {
      val otherStrings: List[DocumentDetail] = List(documentDetailModel(documentDescription = Some("lorem"))
        , documentDetailModel(documentDescription = Some("ipsum"))
        , documentDetailModel(documentDescription = Some("dolor"))
        , documentDetailModel(documentDescription = Some("sit"))
      )
      testValidChargeType(otherStrings, expectedResult = false)
    }
    "valid a document containing valid text" in {
      testValidChargeType(List(documentDetailModel(documentText = Some("Lorem ips Class 2 National Insurance um dolor"))), expectedResult = true)
    }
  }

  "isMFADebit" should {
    def testIsMFADebit(documentId: String, financialDetailsModel: FinancialDetailsModel): Boolean = {
      val fdm: FinancialDetailsModel = financialDetailsModel
      fdm.isMFADebit(documentId)
    }

    "return true for MFA debits" in {
      testIsMFADebit(id1040000123, financialDetailsMFADebits) shouldBe true
    }
    "return false for non-MFA debits" in {
      testIsMFADebit(id1040000123, financialDetailsWithMixedData1) shouldBe false
    }
  }
}
