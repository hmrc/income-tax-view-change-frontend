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

package models

import models.financialDetails._
import models.homePage.NextPaymentsTileViewModel
import org.scalatest.matchers.should.Matchers
import testUtils.UnitSpec

import java.time.LocalDate

class NextPaymentsTileViewModelSpec extends UnitSpec with Matchers{

  val mockDate = Some(LocalDate.parse("2022-08-16"))
  val nextPaymentsTileViewModel: NextPaymentsTileViewModel = NextPaymentsTileViewModel(mockDate, 1, 0, false)

  val futureDate: LocalDate = LocalDate.of(2100, 1, 1)
  val pastDate: LocalDate = LocalDate.of(2000,1,1)
  val currentDate: LocalDate = LocalDate.of(2024, 9, 20)

  val chargesListValid: List[FinancialDetailsModel] = List(
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(taxYear = pastDate.getYear.toString, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate))))))),
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(pastDate.getYear.toString, mainType = Some("SA POA 2 Reconciliation Debit"),
        transactionId = Some("testId3"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate)))))))
  )

  val chargesListOverdue: List[FinancialDetailsModel] = List(
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(pastDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(taxYear = pastDate.getYear.toString, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
        items = Some(Seq(SubItem(dueDate = Some(pastDate))))))),
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(pastDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(pastDate.getYear.toString, mainType = Some("SA POA 2 Reconciliation Debit"),
        transactionId = Some("testId3"),
        items = Some(Seq(SubItem(dueDate = Some(pastDate)))))))
  )

  val chargesListPaid: List[FinancialDetailsModel] = List(
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 0, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(taxYear = pastDate.getYear.toString, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate))))))),
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 0, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = Some(400))),
      financialDetails = List(FinancialDetail(pastDate.getYear.toString, mainType = Some("SA POA 2 Reconciliation Debit"),
        transactionId = Some("testId3"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate)))))))
  )

  val chargesListInterest: List[FinancialDetailsModel] = List(
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = None)),
      financialDetails = List(FinancialDetail(taxYear = pastDate.getYear.toString, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate))))))),
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = List(DocumentDetail(pastDate.getYear, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
        documentDueDate = Some(futureDate), interestOutstandingAmount = None)),
      financialDetails = List(FinancialDetail(pastDate.getYear.toString, mainType = Some("SA POA 2 Reconciliation Debit"),
        transactionId = Some("testId3"),
        items = Some(Seq(SubItem(dueDate = Some(futureDate)))))))
  )

  "The verify method" when {
    "overdue payment exists" should {
      "create the NextPaymentsTileViewModel" in {
        NextPaymentsTileViewModel(mockDate, 1, 0, false).verify shouldBe Right(nextPaymentsTileViewModel)
      }
    }

    "overdue payment doesn't exist" should {
      "return an error" in {
        NextPaymentsTileViewModel(None, 1, 0, false).verify.isLeft shouldBe true
      }
    }
  }

  "The paymentsAccruingInterestCount method" when {
    "given payments that are not yet due, have interest, and aren't paid" should {
      "return the number of such payments" in {
        val res = NextPaymentsTileViewModel.paymentsAccruingInterestCount(chargesListValid, currentDate)
        res shouldBe 2
      }
    }
    "given payments that don't meet the above criteria" should {
      "return zero when payments overdue" in {
        val res = NextPaymentsTileViewModel.paymentsAccruingInterestCount(chargesListOverdue, currentDate)
        res shouldBe 0
      }
      "return zero when payments have no interest" in {
        val res = NextPaymentsTileViewModel.paymentsAccruingInterestCount(chargesListInterest, currentDate)
        res shouldBe 0
      }
      "return zero when payments are paid" in {
        val res = NextPaymentsTileViewModel.paymentsAccruingInterestCount(chargesListPaid, currentDate)
        res shouldBe 0
      }
    }
  }
}
