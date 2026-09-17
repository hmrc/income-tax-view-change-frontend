/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.models.creditsandrefunds

import common.models.incomeSourceDetails.TaxYear
import common.testUtils.UnitSpec
import financials.controllers.routes as financialsRoutes
import financials.models.*
import financials.testConstants.{ANewCreditAndRefundModel, TestTransactions}

import java.time.LocalDate

class MoneyInYourAccountViewModelSpec extends UnitSpec {

  val testUrl = "testUrl"
  val refund10 = TestTransactions.refund(10.0, 1)
  val refund20 = TestTransactions.refund(20.0, 2)
  
  val payment10 = TestTransactions.payment(10.0, None)
  val payment20 = TestTransactions.payment(20.0, None)
  val payment30 = TestTransactions.payment(30.0, None)

  def dateInYear(year: Int): LocalDate = LocalDate.of(year, 1, 1)

  "sorted credit rows" should {

    "return credits in reverse date order" in {
      val model = ANewCreditAndRefundModel()
        .withCutoverCredit(dueDate = dateInYear(2023), outstandingAmount = 1.0)
        .withBalancingChargeCredit(dueDate = dateInYear(2024), outstandingAmount = 2.0)
        .withMfaCredit(dueDate = dateInYear(2021), outstandingAmount = 3.0)
        .withRepaymentInterest(dueDate = dateInYear(2022), outstandingAmount = 4.0)
        .get()

      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows

      rows shouldBe List(
        CreditViewRow(TestTransactions.balancingChargeCredit(dueDate = dateInYear(2024), outstandingAmount = 2.0), TaxYear.forYearEnd(2024), dateInYear(2024), false),
        CreditViewRow(TestTransactions.cutOverCredit(dueDate = dateInYear(2023), outstandingAmount = 1.0), TaxYear.forYearEnd(2023), dateInYear(2023), false),
        CreditViewRow(TestTransactions.repaymentInterestCredit(dueDate = dateInYear(2022), outstandingAmount = 4.0), TaxYear.forYearEnd(2022), dateInYear(2022), false),
        CreditViewRow(TestTransactions.mfaCredit(dueDate = dateInYear(2021), outstandingAmount = 3.0), TaxYear.forYearEnd(2021), dateInYear(2021), false)
      )
    }

    "return refunds in reverse order of amount" in {

      val model = ANewCreditAndRefundModel()
        .withFirstRefund(10.0)
        .withSecondRefund(20.0)
        .get()

      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows
      rows shouldBe List(
        RefundRow(refund20, LocalDate.now()),
        RefundRow(refund10, LocalDate.now())
      )
    }

    "return payments in reverse order of date" in {
      val model = ANewCreditAndRefundModel()
        .withPayment(dateInYear(2023), 10.0)
        .withPayment(dateInYear(2024), 20.0)
        .withPayment(dateInYear(2022), 30.0)
        .get()

      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows

      rows shouldBe List(
        PaymentCreditRow(TestTransactions.payment(dateInYear(2024), 20.0), dateInYear(2024), dateInYear(2024)),
        PaymentCreditRow(TestTransactions.payment(dateInYear(2023), 10.0), dateInYear(2023), dateInYear(2023)),
        PaymentCreditRow(TestTransactions.payment(dateInYear(2022), 30.0), dateInYear(2022), dateInYear(2022)),
      )
    }

    "return refunds after credits" in {
      val model = ANewCreditAndRefundModel()
        .withFirstRefund(10.0)
        .withSecondRefund(20.0)
        .withCutoverCredit(dueDate = dateInYear(2023), outstandingAmount = 1.0)
        .withBalancingChargeCredit(dueDate = dateInYear(2024), outstandingAmount = 2.0)
        .get()
      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows

      rows shouldBe List(
        CreditViewRow(TestTransactions.balancingChargeCredit(dateInYear(2024), 2.0), TaxYear.forYearEnd(2024), dateInYear(2024), false),
        CreditViewRow(TestTransactions.cutOverCredit(dateInYear(2023), 1.0), TaxYear.forYearEnd(2023), dateInYear(2023), false),
        RefundRow(refund20, LocalDate.now()),
        RefundRow(refund10, LocalDate.now())
      )
    }

    "filter out credits with no outstanding amount" in {
      val model = ANewCreditAndRefundModel()
        .withFirstRefund(10.0)
        .withSecondRefund(20.0)
        .withCutoverCredit(dueDate = dateInYear(2023), outstandingAmount = 0.0)
        .withBalancingChargeCredit(dueDate = dateInYear(2024), outstandingAmount = 2.0)
        .get()
      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows

      rows shouldBe List(
        CreditViewRow(TestTransactions.balancingChargeCredit(dueDate = dateInYear(2024), outstandingAmount = 2.0), TaxYear.forYearEnd(2024), dateInYear(2024), false),
        RefundRow(refund20, LocalDate.now()),
        RefundRow(refund10, LocalDate.now())
      )
    }
  }

  "CreditRow" should {
    "create the correct PaymentCreditRow" in {

      val dueDate = dateInYear(2021)
      val effectiveDateOfPayment = dateInYear(2022)

      val paymentTransaction: Transaction =
        Transaction(
          transactionType = PaymentType,
          amount = 5,
          taxYear = Some(TaxYear.forYearEnd(2021)),
          dueDate = Some(dueDate),
          documentDate = None,
          effectiveDateOfPayment = Some(effectiveDateOfPayment),
          transactionId = "PAYMENT01"
        )
      val paymentRow = CreditRow.fromTransaction(paymentTransaction)
      paymentRow shouldBe Some(PaymentCreditRow(paymentTransaction, dueDate, effectiveDateOfPayment))
    }
    "create the correct RefundRow" in {
      val refundTransaction: Transaction =
        Transaction(
          transactionType = Repayment,
          amount = 5,
          taxYear = None,
          dueDate = None,
          documentDate = None,
          effectiveDateOfPayment = None,
          transactionId = "REFUND01"
        )
      val refundRow = CreditRow.fromTransaction(refundTransaction)
      refundRow shouldBe Some(RefundRow(refundTransaction, LocalDate.now()))
    }
    "create the correct CreditViewRow" in {
      val documentDate = dateInYear(2022)
      val taxYear = TaxYear.forYearEnd(2021)

      val creditTransaction: Transaction =
        Transaction(
          transactionType = MfaCreditType,
          amount = 3,
          taxYear = Some(taxYear),
          dueDate = None,
          documentDate = Some(documentDate),
          effectiveDateOfPayment = None,
          transactionId = "MFA01"
        )
      val creditRow = CreditRow.fromTransaction(creditTransaction)
      creditRow shouldBe Some(CreditViewRow(creditTransaction, taxYear, documentDate, false))
    }
  }

  "CreditViewRow" should {

    
    val creditAmount = 10.0
    val creditTaxYear = 2024
    val creditId = "credit"

    val chargeSummaryCredits: Seq[Transaction] = Seq(
      TestTransactions.poaOneReconciliationCredit(dateInYear(creditTaxYear), creditAmount, creditId),
      TestTransactions.poaTwoReconciliationCredit(dateInYear(creditTaxYear), creditAmount, creditId),
      TestTransactions.itsaReturnAmendmentCredit(dateInYear(creditTaxYear), creditAmount, creditId)
    )
  
    val creditSummaryCredits: Seq[Transaction] = Seq(
      TestTransactions.cutOverCredit(dateInYear(creditTaxYear), creditAmount),
      TestTransactions.balancingChargeCredit(dateInYear(creditTaxYear), creditAmount),
      TestTransactions.mfaCredit(dateInYear(creditTaxYear), creditAmount),
      TestTransactions.repaymentInterestCredit(dateInYear(creditTaxYear), creditAmount)
    )

    s"have the correct description link url" in {
      val convertToNonRevenueAmendmentRow: Transaction => CreditViewRow = 
        transaction => 
          CreditViewRow(
            transaction, 
            TaxYear.forYearEnd(creditTaxYear), 
            dateInYear(creditTaxYear), 
            false
          )
      val chargeSummaryCreditRows = chargeSummaryCredits.map(convertToNonRevenueAmendmentRow)

      val creditSummaryCreditRows = creditSummaryCredits.map(convertToNonRevenueAmendmentRow)
        
      chargeSummaryCreditRows.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe financialsRoutes.ChargeSummaryController.show(creditTaxYear, creditId).url
        creditRow.descriptionLink(true) shouldBe financialsRoutes.ChargeSummaryController.showAgent(creditTaxYear, creditId).url
      }

      creditSummaryCreditRows.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe financialsRoutes.CreditsSummaryController.showCreditsSummary(creditTaxYear).url
        creditRow.descriptionLink(true) shouldBe financialsRoutes.CreditsSummaryController.showAgentCreditsSummary(creditTaxYear).url
      }
    }
    s"have the correct description link url for revenue amendment credits" in {
      val convertToRevenueAmendmentRow: Transaction => CreditViewRow = 
        transaction => 
          CreditViewRow(
            transaction, 
            TaxYear.forYearEnd(creditTaxYear), 
            dateInYear(creditTaxYear), 
            isRevenueAmendment = true 
          )

      val chargeSummaryCreditsRows = chargeSummaryCredits.map(convertToRevenueAmendmentRow)

      val creditSummaryCreditsRows = creditSummaryCredits.map(convertToRevenueAmendmentRow)

      chargeSummaryCreditsRows.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe financialsRoutes.ChargeSummaryController.show(creditTaxYear, creditId).url
        creditRow.descriptionLink(true) shouldBe financialsRoutes.ChargeSummaryController.showAgent(creditTaxYear, creditId).url
      }

      creditSummaryCreditsRows.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe financialsRoutes.CreditsSummaryController.showCreditsSummary(creditTaxYear).url
        creditRow.descriptionLink(true) shouldBe financialsRoutes.CreditsSummaryController.showAgentCreditsSummary(creditTaxYear).url
      }
    }
  }

  "PaymentCreditRow" should {
    "have the correct tax year" in {
      val paymentRow = PaymentCreditRow(payment20, dateInYear(2024), dateInYear(2024))
      paymentRow.taxYear shouldBe TaxYear.forYearEnd(2024)
    }
    "have the correct description link url" in {
      val payment = TestTransactions.payment(20.0, None)
      val paymentRow = PaymentCreditRow(payment, dateInYear(2024), dateInYear(2024))
      paymentRow.descriptionLink(false) shouldBe financialsRoutes.PaymentAllocationsController.viewPaymentAllocation("payment").url
      paymentRow.descriptionLink(true) shouldBe financialsRoutes.PaymentAllocationsController.viewPaymentAllocationAgent("payment").url
    }
  }

  "RefundRow" should {
    "have the correct description link url" in {
      val refundRow = RefundRow(refund20, dateInYear(2024))
      refundRow.descriptionLink shouldBe financialsRoutes.PaymentHistoryController.refundStatus().url
    }
  }

}