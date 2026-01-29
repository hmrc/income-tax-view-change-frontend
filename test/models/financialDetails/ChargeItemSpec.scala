/*
 * Copyright 2024 HM Revenue & Customs
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

import config.FrontendAppConfig
import enums.CodingOutType.CODING_OUT_CLASS2_NICS
import exceptions.MissingFieldException
import models.financialDetails.ChargeItem.filterAllowedCharges
import play.api.libs.json.Json
import services.{DateService, DateServiceInterface}
import testConstants.BaseTestConstants.app
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{documentDetailModel, financialDetail}
import testUtils.UnitSpec

import java.time.LocalDate

class ChargeItemSpec extends UnitSpec with ChargeConstants  {

  val dueDate = LocalDate.of(2024, 1, 1)
  val originalAmount: BigDecimal = 100.0
  val outstandingAmount: BigDecimal = 50.0
  val interestOutstandingAmount: Option[BigDecimal] = Some(40.0)
  val accruingInterestAmount: Option[BigDecimal] = Some(30.0)
  val lpiWithDunningLock: Option[BigDecimal] = Some(20.0)
  val amountCodedOut: Option[BigDecimal] = Some(150.0)


  val defaultDocDetails = documentDetailModel(documentDueDate = Some(dueDate),
    originalAmount = originalAmount,
    outstandingAmount = outstandingAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    accruingInterestAmount = accruingInterestAmount,
    lpiWithDunningLock = lpiWithDunningLock)

  val docDetailsNoOutstandingAmout = defaultDocDetails.copy(outstandingAmount = 0)
  val docDetailsAmountCodedOut = defaultDocDetails.copy(amountCodedOut = amountCodedOut)

  val poa1FinancialDetails = financialDetail()
  val poa2FinancialDetails = financialDetail(mainTransaction = "4930")

  val poaOneReconciliationDebitDetails = financialDetail(mainTransaction = "4911")
  val PoaTwoReconciliationDebitDetails = financialDetail(mainTransaction = "4913")

  val balancingNics2DocumentDetails = defaultDocDetails.copy(documentText = Some(CODING_OUT_CLASS2_NICS.name))
  val balancingNics2FinancialDetails = financialDetail(mainTransaction = "4910")

  val balancingAcceptedFinancialDetails = financialDetail(mainTransaction = "4910", codedOutStatus = Some("I"))

  val balancingCancelledFinancialDetails = financialDetail(mainTransaction = "4910",codedOutStatus = Some("C"))

  val mfaFinancialDetails = financialDetail(mainTransaction = "4003")


  implicit val dateService: DateServiceInterface = dateService(LocalDate.of(2000, 1, 1))
  def dateService(currentDate: LocalDate): DateService = new DateService()(app.injector.instanceOf[FrontendAppConfig]){
    override def getCurrentDate: LocalDate = currentDate
  }

  "ChargeItem" when {

    "isNotPaidAndNotOverduePoaReconciliationDebit" when {

      "transaction type is PoaOneReconciliationDebit, is not overdue and is not paid returns true" in {
        val dateServiceBeforeDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))


        chargeItem.isNotPaidAndNotOverduePoaReconciliationDebit()(dateServiceBeforeDueDate) shouldBe true
      }

      "transaction type is PoaTwoReconciliationDebit, is not overdue and is not paid returns true" in {
        val dateServiceBeforeDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(PoaTwoReconciliationDebitDetails))

        chargeItem.isNotPaidAndNotOverduePoaReconciliationDebit()(dateServiceBeforeDueDate) shouldBe true
      }

      "returns false when charge item is not PoaOneReconciliationDebit or PoaTwoReconciliationDebit " in {
        val dateServiceAfterDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(balancingNics2FinancialDetails))

        chargeItem.isNotPaidAndNotOverduePoaReconciliationDebit()(dateServiceAfterDueDate) shouldBe false
      }

      "charge is overdue and is not paid returns false" in {
        val dateServiceAfterDueDate = dateService(dueDate.plusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.isNotPaidAndNotOverduePoaReconciliationDebit()(dateServiceAfterDueDate) shouldBe false
      }

      "charge is not overdue and is paid returns false" in {
        val dateServiceBeforeDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = docDetailsNoOutstandingAmout,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.isNotPaidAndNotOverduePoaReconciliationDebit()(dateServiceBeforeDueDate) shouldBe false
      }

    }

    "hasAccruingInterest" when {

      "Is true if we have and accruing interest amount and an interestOutstanding amount for a POA1" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails.copy(accruingInterestAmount = Some(-1.00), interestOutstandingAmount = Some(10.00)),
          financialDetails = List(poa1FinancialDetails))

        chargeItem.hasAccruingInterest shouldBe true

      }

      "Is false if we have and accruing interest amount and an interestOutstanding amount of zero for a POA1" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails.copy(accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(0.00)),
          financialDetails = List(poa1FinancialDetails))

        chargeItem.hasAccruingInterest shouldBe false

      }
    }

    "getDueDate" when {

      "successfully gets due date" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getDueDate shouldBe LocalDate.of(2024, 1, 1)

      }

      "throws MissingFieldException when due date is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(dueDate = None)

        val exception = intercept[MissingFieldException] {
          chargeItem.getDueDate
        }
        exception shouldBe MissingFieldException("documentDueDate")


      }

    }

    "getInterestFromDate" when {

      "successfully gets interestFromDate" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getInterestFromDate shouldBe LocalDate.of(2018, 3, 29)

      }

      "throws MissingFieldException when interestFromDate is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestFromDate = None)

        val exception = intercept[MissingFieldException] {
          chargeItem.getInterestFromDate
        }
        exception shouldBe MissingFieldException("documentInterestFromDate")


      }
    }

    "getInterestEndDate" when {

      "successfully gets InterestEndDate" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getInterestEndDate shouldBe LocalDate.of(2018, 6, 15)

      }

      "throws MissingFieldException when interestEndDate is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestEndDate = None)

        val exception = intercept[MissingFieldException] {
          chargeItem.getInterestEndDate
        }
        exception shouldBe MissingFieldException("documentInterestEndDate")


      }
    }

    "getInterestRate" when {

      "successfully gets InterestRate" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getInterestRate shouldBe 100

      }

      "throws MissingFieldException when interestRate is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestRate = None)

        val exception = intercept[MissingFieldException] {
          chargeItem.getInterestRate
        }
        exception shouldBe MissingFieldException("documentInterestRate")

      }
    }

    "getInterestOutstandingAmount" when {

      "successfully gets interestOutstandingAmount" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getInterestOutstandingAmount shouldBe 40

      }

      "throws MissingFieldException when interestRate is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestOutstandingAmount = None)
        
        println(Json.prettyPrint(Json.toJson(chargeItem)))

        val exception = intercept[MissingFieldException] {
          chargeItem.getInterestOutstandingAmount
        }
        exception shouldBe MissingFieldException("documentInterestOutstandingAmount")

      }
    }

    "getAmountCodedOut" when {

      "successfully gets amountCodedOut" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = docDetailsAmountCodedOut,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getAmountCodedOut shouldBe 150

      }

      "throws MissingFieldException when amountCodedOut is not found" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        val exception = intercept[MissingFieldException] {
          chargeItem.getAmountCodedOut
        }
        exception shouldBe MissingFieldException("documentAmountCodedOut")

      }
    }

    "interestIsPaid" when {

      "interest outstanding amount is 0 returns true" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestOutstandingAmount = Some(BigDecimal(0)))

        chargeItem.interestIsPaid shouldBe true
      }

      "interest outstanding amount is not 0 returns false" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.interestIsPaid shouldBe false
      }
    }

    "getInterestPaidStatus" when {

      "interest is 0, return paid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestOutstandingAmount = Some(BigDecimal(0)))

        chargeItem.getInterestPaidStatus shouldBe "paid"
      }

      "interest is part paid, return part-paid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getInterestPaidStatus shouldBe "part-paid"
      }

      "interest is not paid, return unpaid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(interestOutstandingAmount = Some(BigDecimal(30)))

        chargeItem.getInterestPaidStatus shouldBe "unpaid"
      }
    }

    "getChargePaidStatus" when {

      "outstanding amount is 0, return paid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(outstandingAmount = BigDecimal(0))

        chargeItem.getChargePaidStatus shouldBe "paid"
      }

      "interest is part paid, return part-paid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails))

        chargeItem.getChargePaidStatus shouldBe "part-paid"
      }

      "interest not paid, return unpaid" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poaOneReconciliationDebitDetails)).copy(outstandingAmount = BigDecimal(100))

        chargeItem.getChargePaidStatus shouldBe "unpaid"
      }
    }
  }

  "fromDocumentPair" when {

      "from Payment on Account 1" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails))

        chargeItem.transactionType shouldBe PoaOneDebit
        chargeItem.codedOutStatus shouldBe None
      }

      "from Payment on Account 2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa2FinancialDetails))

        chargeItem.transactionType shouldBe PoaTwoDebit
        chargeItem.codedOutStatus shouldBe None
      }

      "from Balancing Payment Nics2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingNics2DocumentDetails,
          financialDetails = List(balancingNics2FinancialDetails))

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.codedOutStatus shouldBe Some(Nics2)
      }

      "from Balancing Payment Accepted" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(balancingAcceptedFinancialDetails))

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.codedOutStatus shouldBe Some(Accepted)
      }

      "from Balancing Payment Cancelled" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(balancingCancelledFinancialDetails))

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.codedOutStatus shouldBe Some(Cancelled)
      }

      "from MFA" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(mfaFinancialDetails))

        chargeItem.transactionType shouldBe MfaDebitCharge
        chargeItem.codedOutStatus shouldBe None
      }

    "isOverdue calculated correctly" when {

      "date is before due date" in {

        val dateServiceBeforeDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails))

        chargeItem.isOverdue()(dateServiceBeforeDueDate) shouldBe false
      }

      "date is due date" in {

        val dateServiceOnDueDate = dateService(dueDate)

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails))

        chargeItem.isOverdue()(dateServiceOnDueDate) shouldBe false
      }

      "date is after due date" in {

        val dateServiceAfterDueDate = dateService(dueDate.plusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails))

        chargeItem.isOverdue()(dateServiceAfterDueDate) shouldBe true
      }
    }
  }

    "getChargeKey" when {

        "charge is a POA 1" in {
          val poa1 = chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = None)
          val key = poa1.getChargeTypeKey
          key shouldBe "paymentOnAccount1.text"
        }

        "charge is a POA 2" in {
          val poa1 = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = None)
          val key = poa1.getChargeTypeKey
          key shouldBe "paymentOnAccount2.text"
        }


        "charge is a HMRC adjustment" in {
          val poa1 = chargeItemModel(transactionType = MfaDebitCharge, codedOutStatus = None)
          val key = poa1.getChargeTypeKey
          key shouldBe "hmrcAdjustment.text"
        }

        "charge is a Class 2 National Insurance Balancing Charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2))
          val key = poa1.getChargeTypeKey
          key shouldBe "class2Nic.text"
        }

        "charge is a PAYE payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted))
          val key = poa1.getChargeTypeKey
          key shouldBe "codingOut.text"
        }

        "charge is a cancelled PAYE SA payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled))
          val key = poa1.getChargeTypeKey
          key shouldBe "cancelledPayeSelfAssessment.text"
        }

        "charge is a balancing charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = None)
          val key = poa1.getChargeTypeKey
          key shouldBe "balancingCharge.text"
        }
  }

  "filterAllowedCharges" should {
    "filter out FS related charges" when {
      "relevant FS is disabled" in {
        val chargesListWithRandR: List[ChargeItem] = List(
          chargeItemModel(transactionType = BalancingCharge),
          chargeItemModel(transactionType = PoaOneReconciliationDebit),
          chargeItemModel(transactionType = PoaTwoDebit),
          chargeItemModel(transactionType = PoaTwoReconciliationDebit)
        )
        val filtered = chargesListWithRandR.map(filterAllowedCharges(false, PoaOneReconciliationDebit, PoaTwoReconciliationDebit))
        filtered shouldBe List(true, false, true, false)
      }
    }
    "include FS related charges" when {
      "FS is enabled" in {
        val chargesListWithRandR: List[ChargeItem] = List(
          chargeItemModel(transactionType = BalancingCharge),
          chargeItemModel(transactionType = PoaOneReconciliationDebit),
          chargeItemModel(transactionType = PoaTwoDebit),
          chargeItemModel(transactionType = PoaTwoReconciliationDebit)
        )
        val filtered = chargesListWithRandR.map(filterAllowedCharges(true, PoaOneReconciliationDebit, PoaTwoReconciliationDebit))
        filtered shouldBe List(true, true, true, true)
      }
    }
    "return empty list" when {
      "fed an empty list" in {
        val chargesList = List()
        val filtered = chargesList.map(filterAllowedCharges(true, PoaOneReconciliationDebit, PoaTwoReconciliationDebit))
        filtered shouldBe List()
      }
    }
  }

  "originalAmountIsNotZeroOrNegative" should {
    def originalAmountIsNotZeroOrNegative(chargeItem: ChargeItem): Boolean = chargeItem.originalAmount match {
      case amount if amount <= 0 => false
      case _ => true
    }

    "return false" when {
      "original amount is zero" in {
        val chargeItemModelZeroAmount = chargeItemModel().copy(originalAmount = 0)
        originalAmountIsNotZeroOrNegative(chargeItemModelZeroAmount) shouldBe false
      }
      "original amount is negative" in {
        val chargeItemModelNegativeAmount = chargeItemModel().copy(originalAmount = -20.32)
        originalAmountIsNotZeroOrNegative(chargeItemModelNegativeAmount) shouldBe false
      }
    }
    "return true" when {
      "original amount is positive" in {
        val chargeItemModelPositiveAmount = chargeItemModel().copy(originalAmount = 20.89)
        originalAmountIsNotZeroOrNegative(chargeItemModelPositiveAmount) shouldBe true
      }
    }
  }
}
