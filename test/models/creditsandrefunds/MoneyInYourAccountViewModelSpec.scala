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

package models.creditsandrefunds

import models.financialDetails.{BalancingChargeCreditType, CutOverCreditType, MfaCreditType, RepaymentInterest}
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
       CreditViewRow(2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024)),
       CreditViewRow(1.0, CutOverCreditType, TaxYear.forYearEnd(2023)),
       CreditViewRow(4.0, RepaymentInterest, TaxYear.forYearEnd(2022)),
       CreditViewRow(3.0, MfaCreditType, TaxYear.forYearEnd(2021))
     )
   }

    "return refunds in reverse order of amount" in {

      val model = ANewCreditAndRefundModel()
               .withFirstRefund(10.0)
               .withSecondRefund(20.0)
               .get()

      val rows = MoneyInYourAccountViewModel.fromCreditsModel(model, testUrl).creditRows

      rows shouldBe List(
        RefundRow(20.0),
        RefundRow(10.0)
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
        CreditViewRow(2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024)),
        CreditViewRow(1.0, CutOverCreditType, TaxYear.forYearEnd(2023)),
        RefundRow(20.0),
        RefundRow(10.0)
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
        CreditViewRow(2.0, BalancingChargeCreditType, TaxYear.forYearEnd(2024)),
        RefundRow(20.0),
        RefundRow(10.0)
      )
    }
  }

  def dateInYear(year: Int) = LocalDate.of(year, 1, 1)

}
