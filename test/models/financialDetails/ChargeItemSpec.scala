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
import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS}
import models.financialDetails.ChargeItem.filterAllowedCharges
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
  val latePaymentInterestAmount: Option[BigDecimal] = Some(30.0)
  val lpiWithDunningLock: Option[BigDecimal] = Some(20.0)


  val defaultDocDetails = documentDetailModel(documentDueDate = Some(dueDate),
    originalAmount = originalAmount,
    outstandingAmount = outstandingAmount,
    interestOutstandingAmount = interestOutstandingAmount,
    latePaymentInterestAmount = latePaymentInterestAmount,
    lpiWithDunningLock = lpiWithDunningLock)

  val poa1FinancialDetails = financialDetail()

  val poa2FinancialDetails = financialDetail(mainTransaction = "4930")

  val balancingNics2DocumentDetails = defaultDocDetails.copy(documentText = Some(CODING_OUT_CLASS2_NICS.name))
  val balancingNics2FinancialDetails = financialDetail(mainTransaction = "4910")

  val balancingAcceptedDocumentDetails = defaultDocDetails.copy(documentText = Some(CODING_OUT_ACCEPTED.name))
  val balancingAcceptedFinancialDetails = financialDetail(mainTransaction = "4910")

  val balancingCancelledDocumentDetails = defaultDocDetails.copy(documentText = Some(CODING_OUT_CANCELLED.name))
  val balancingCancelledFinancialDetails = financialDetail(mainTransaction = "4910")

  val mfaFinancialDetails = financialDetail(mainTransaction = "4003")


  implicit val dateService: DateServiceInterface = dateService(LocalDate.of(2000, 1, 1))
  def dateService(currentDate: LocalDate): DateService = new DateService()(app.injector.instanceOf[FrontendAppConfig]){
    override def getCurrentDate: LocalDate = currentDate
  }

  "fromDocumentPair" when {

    "coding out is enabled" when {

      val codingOutEnabled = true

      "from Payment on Account 1" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe PaymentOnAccountOne
        chargeItem.subTransactionType shouldBe None

      }

      "from Payment on Account 2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa2FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe PaymentOnAccountTwo
        chargeItem.subTransactionType shouldBe None
      }

      "from Balancing Payment Nics2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingNics2DocumentDetails,
          financialDetails = List(balancingNics2FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Nics2)
      }

      "from Balancing Payment Accepted" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingAcceptedDocumentDetails,
          financialDetails = List(balancingAcceptedFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Accepted)
      }

      "from Balancing Payment Cancelled" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingCancelledDocumentDetails,
          financialDetails = List(balancingCancelledFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Cancelled)
      }

      "from MFA" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(mfaFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe MfaDebitCharge
        chargeItem.subTransactionType shouldBe None
      }
    }

    "coding out is disabled" when {

      val codingOutEnabled = false

      "from Payment on Account 1" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe PaymentOnAccountOne
        chargeItem.subTransactionType shouldBe None
      }

      "from Payment on Account 2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa2FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe PaymentOnAccountTwo
        chargeItem.subTransactionType shouldBe None
      }

      "from Balancing Payment Nics2" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingNics2DocumentDetails,
          financialDetails = List(balancingNics2FinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Nics2)
      }

      "from Balancing Payment Accepted" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingAcceptedDocumentDetails,
          financialDetails = List(balancingAcceptedFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Accepted)
      }

      "from Balancing Payment Cancelled" in {

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = balancingCancelledDocumentDetails,
          financialDetails = List(balancingCancelledFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe BalancingCharge
        chargeItem.subTransactionType shouldBe Some(Cancelled)
      }

      "from MFA" in {
        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(mfaFinancialDetails),
          codingOut = codingOutEnabled)

        chargeItem.transactionType shouldBe MfaDebitCharge
        chargeItem.subTransactionType shouldBe None
      }
    }

    "isOverdue calculated correctly" when {

      "date is before due date" in {

        val dateServiceBeforeDueDate = dateService(dueDate.minusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails),
          codingOut = true)

        chargeItem.isOverdue()(dateServiceBeforeDueDate) shouldBe false
      }

      "date is due date" in {

        val dateServiceOnDueDate = dateService(dueDate)

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails),
          codingOut = true)

        chargeItem.isOverdue()(dateServiceOnDueDate) shouldBe false
      }

      "date is after due date" in {

        val dateServiceAfterDueDate = dateService(dueDate.plusDays(1))

        val chargeItem = ChargeItem.fromDocumentPair(
          documentDetail = defaultDocDetails,
          financialDetails = List(poa1FinancialDetails),
          codingOut = true)

        chargeItem.isOverdue()(dateServiceAfterDueDate) shouldBe true
      }
    }
  }

    "getChargeKey" when {

      "coding out is enabled" when {

        val codingOutEnabled = true

        "charge is a POA 1" in {
          val poa1 = chargeItemModel(transactionType = PaymentOnAccountOne, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "paymentOnAccount1.text"
        }

        "charge is a POA 2" in {
          val poa1 = chargeItemModel(transactionType = PaymentOnAccountTwo, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "paymentOnAccount2.text"
        }


        "charge is a HMRC adjustment" in {
          val poa1 = chargeItemModel(transactionType = MfaDebitCharge, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "hmrcAdjustment.text"
        }

        "charge is a Class 2 National Insurance Balancing Charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Nics2))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "class2Nic.text"
        }

        "charge is a PAYE payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Accepted))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "codingOut.text"
        }

        "charge is a cancelled PAYE SA payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Cancelled))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "cancelledPayeSelfAssessment.text"
        }

        "charge is a balancing charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "balancingCharge.text"
        }
      }


      "coding out is disabled" when {

        val codingOutEnabled = false

        "charge is a POA 1" in {
          val poa1 = chargeItemModel(transactionType = PaymentOnAccountOne, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "paymentOnAccount1.text"
        }

        "charge is a POA 2" in {
          val poa1 = chargeItemModel(transactionType = PaymentOnAccountTwo, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "paymentOnAccount2.text"
        }


        "charge is a HMRC adjustment" in {
          val poa1 = chargeItemModel(transactionType = MfaDebitCharge, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "hmrcAdjustment.text"
        }

        "charge is a Class 2 National Insurance Balancing Charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Nics2))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "balancingCharge.text"
        }

        "charge is a PAYE payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Accepted))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "balancingCharge.text"
        }

        "charge is a cancelled PAYE SA payment" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Cancelled))
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "balancingCharge.text"
        }

        "charge is a balancing charge" in {
          val poa1 = chargeItemModel(transactionType = BalancingCharge, subTransactionType = None)
          val key = poa1.getChargeTypeKey(codingOutEnabled)
          key shouldBe "balancingCharge.text"
        }
      }
  }

  "filterAllowedCharges" should {
    "filter out FS related charges" when {
      "relevant FS is disabled" in {
        val chargesListWithRandR: List[ChargeItem] = List(
          chargeItemModel(transactionType = BalancingCharge),
          chargeItemModel(transactionType = PaymentOnAccountOneReviewAndReconcileDebit),
          chargeItemModel(transactionType = PaymentOnAccountTwo),
          chargeItemModel(transactionType = PaymentOnAccountTwoReviewAndReconcileDebit)
        )
        val filtered = chargesListWithRandR.map(filterAllowedCharges(false, PaymentOnAccountOneReviewAndReconcileDebit, PaymentOnAccountTwoReviewAndReconcileDebit))
        filtered shouldBe List(true, false, true, false)
      }
    }
    "include FS related charges" when {
      "FS is enabled" in {
        val chargesListWithRandR: List[ChargeItem] = List(
          chargeItemModel(transactionType = BalancingCharge),
          chargeItemModel(transactionType = PaymentOnAccountOneReviewAndReconcileDebit),
          chargeItemModel(transactionType = PaymentOnAccountTwo),
          chargeItemModel(transactionType = PaymentOnAccountTwoReviewAndReconcileDebit)
        )
        val filtered = chargesListWithRandR.map(filterAllowedCharges(true, PaymentOnAccountOneReviewAndReconcileDebit, PaymentOnAccountTwoReviewAndReconcileDebit))
        filtered shouldBe List(true, true, true, true)
      }
    }
    "return empty list" when {
      "fed an empty list" in {
        val chargesList = List()
        val filtered = chargesList.map(filterAllowedCharges(true, PaymentOnAccountOneReviewAndReconcileDebit, PaymentOnAccountTwoReviewAndReconcileDebit))
        filtered shouldBe List()
      }
    }
  }
}
