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

package models.creditsandrefunds

import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import testConstants.ANewCreditAndRefundModel
import testUtils.UnitSpec
import java.time.LocalDate

class MoneyInYourAccountViewModelSpec extends UnitSpec {

  val testUrl = "testUrl"

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
        CreditViewRow("balancing", 2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024), dateInYear(2024)),
        CreditViewRow("cutover", 1.0, CutOverCreditType, TaxYear.forYearEnd(2023), dateInYear(2023)),
        CreditViewRow("repayment", 4.0, RepaymentInterest, TaxYear.forYearEnd(2022), dateInYear(2022)),
        CreditViewRow("mfa", 3.0, MfaCreditType, TaxYear.forYearEnd(2021), dateInYear(2021))
      )
    }

    "return refunds in reverse order of amount" in {

      val model = ANewCreditAndRefundModel()
        .withFirstRefund(10.0)
        .withSecondRefund(20.0)
        .get()

      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows
      rows shouldBe List(
        RefundRow(20.0, LocalDate.now()),
        RefundRow(10.0, LocalDate.now())
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
        PaymentCreditRow("payment", 20.0, dateInYear(2024), dateInYear(2024)),
        PaymentCreditRow("payment", 10.0, dateInYear(2023), dateInYear(2023)),
        PaymentCreditRow("payment", 30.0, dateInYear(2022), dateInYear(2022)),
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
        CreditViewRow("balancing", 2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024), dateInYear(2024)),
        CreditViewRow("cutover", 1.0, CutOverCreditType, TaxYear.forYearEnd(2023), dateInYear(2023)),
        RefundRow(20.0, LocalDate.now()),
        RefundRow(10.0, LocalDate.now())
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
        CreditViewRow("balancing", 2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024), dateInYear(2024)),
        RefundRow(20.0, LocalDate.now()),
        RefundRow(10.0, LocalDate.now())
      )
    }
  }

  "CreditViewRow" should {
    s"have the correct description link url" in {
      val creditId = "credit"
      val creditAmount = 10.0
      val creditTaxYear = 2024

      val chargeSummaryCredits = Seq(
        PoaOneReconciliationCredit,
        PoaTwoReconciliationCredit,
        ITSAReturnAmendmentCredit
      ).map { creditType => CreditViewRow(creditId, creditAmount, creditType, TaxYear.forYearEnd(creditTaxYear), dateInYear(creditTaxYear)) }

      val creditSummaryCredits = Seq(
        CutOverCreditType,
        BalancingChargeCreditType,
        MfaCreditType,
        RepaymentInterest
      ).map { creditType => CreditViewRow(creditId, creditAmount, creditType, TaxYear.forYearEnd(creditTaxYear), dateInYear(creditTaxYear)) }

      chargeSummaryCredits.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe controllers.routes.ChargeSummaryController.show(creditTaxYear, creditId).url
        creditRow.descriptionLink(true) shouldBe controllers.routes.ChargeSummaryController.showAgent(creditTaxYear, creditId).url
      }

      creditSummaryCredits.foreach { creditRow =>
        creditRow.descriptionLink(false) shouldBe controllers.routes.CreditsSummaryController.showCreditsSummary(creditTaxYear).url
        creditRow.descriptionLink(true) shouldBe controllers.routes.CreditsSummaryController.showAgentCreditsSummary(creditTaxYear).url
      }
    }
  }

  "PaymentCreditRow" should {
    "have the correct tax year" in {
      val paymentRow = PaymentCreditRow("payment", 20.0, dateInYear(2024), dateInYear(2024))
      paymentRow.taxYear shouldBe TaxYear.forYearEnd(2024)
    }
    "have the correct description link url" in {
      val paymentRow = PaymentCreditRow("payment", 20.0, dateInYear(2024), dateInYear(2024))
      paymentRow.descriptionLink(false) shouldBe controllers.routes.PaymentAllocationsController.viewPaymentAllocation("payment").url
      paymentRow.descriptionLink(true) shouldBe controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent("payment").url
    }
  }
  "RefundRow" should {
    "have the correct description link url" in {
      val refundRow = RefundRow(30.0, dateInYear(2024))
      refundRow.descriptionLink shouldBe controllers.routes.PaymentHistoryController.refundStatus().url
    }
  }

  def dateInYear(year: Int): LocalDate = LocalDate.of(year, 1, 1)

}