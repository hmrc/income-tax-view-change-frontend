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

package financials.services

import common.auth.MtdItUser
import common.auth.actions.AuthActionsTestData.defaultMTDITUser
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import models.financialDetails.{BalanceDetails, FinancialDetailsErrorModel, FinancialDetailsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class MakingPaymentServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncomeWithCurrentYear)
  implicit val hc: HeaderCarrier = headerCarrier

  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])

  object TestMakingPaymentService extends MakingPaymentService(mockFinancialDetailsService, dateService)

  private def financialDetailsModel(overDueAmount: BigDecimal = 0,
                                    unallocatedCredit: Option[BigDecimal] = None,
                                    totalCreditAvailableForRepayment: Option[BigDecimal] = None): FinancialDetailsModel =
    FinancialDetailsModel(
      balanceDetails = BalanceDetails(
        balanceDueWithin30Days = 0,
        overDueAmount = overDueAmount,
        balanceNotDuein30Days = 0,
        totalBalance = overDueAmount,
        totalCreditAvailableForRepayment = totalCreditAvailableForRepayment,
        allocatedCredit = None,
        allocatedCreditForFutureCharges = None,
        totalCredit = None,
        firstPendingAmountRequested = None,
        secondPendingAmountRequested = None,
        unallocatedCredit = unallocatedCredit
      ),
      documentDetails = List.empty,
      financialDetails = List.empty
    )

  "MakingPaymentService.createViewModel" should {

    "set hasInterest when any financial details response has an overdue amount" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(2025 -> financialDetailsModel(overDueAmount = 1.67))))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasInterest) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set unallocatedCredit to the highest positive credit found across financial details responses" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2024 -> financialDetailsModel(unallocatedCredit = Some(200)),
          2025 -> financialDetailsModel(unallocatedCredit = Some(400))
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.flatMap(_.unallocatedCredit) shouldBe Some(BigDecimal(400))
      result.map(_.hasMoneyInAccount) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "fall back to totalCreditAvailableForRepayment when unallocatedCredit is not present" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(totalCreditAvailableForRepayment = Some(2300))
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.flatMap(_.unallocatedCredit) shouldBe Some(BigDecimal(2300))
      result.map(_.hasMoneyInAccount) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "return None when any financial details response is an error" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(2025 -> FinancialDetailsErrorModel(500, "error"))))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result shouldBe None
    }
  }
}
