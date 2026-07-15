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
import financials.models.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import common.testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class MakingPaymentServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncomeWithCurrentYear)
  implicit val hc: HeaderCarrier = headerCarrier

  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])

  object TestMakingPaymentService extends MakingPaymentService(mockFinancialDetailsService, dateService)

  private def financialDetailsModel(overDueAmount: BigDecimal = 0,
                                    unallocatedCredit: Option[BigDecimal] = None,
                                    totalCreditAvailableForRepayment: Option[BigDecimal] = None,
                                    documentDetails: List[DocumentDetail] = List.empty,
                                    financialDetails: List[FinancialDetail] = List.empty): FinancialDetailsModel =
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
      documentDetails = documentDetails,
      financialDetails = financialDetails
    )

  val penaltyTransactionIdFirst = "penalty-transaction-1"
  val penaltyTransactionIdSecond = "penalty-transaction-2"
  val penaltyTransactionIdThird = "penalty-transaction-3"
  val nonPenaltyTransactionId = "non-penalty-transaction"
  val balancingChargeMainTransaction = "4910"

  private def getDocumentDetail(transactionId: String, documentDueDate: Option[LocalDate] = Some(dateService.getCurrentDate.minusDays(1))): DocumentDetail = DocumentDetail(
    taxYear = 2025,
    transactionId = transactionId,
    documentDescription = None,
    documentText = None,
    outstandingAmount = 100,
    originalAmount = 100,
    documentDueDate = documentDueDate,
    documentDate = LocalDate.of(2025, 4, 6)
  )

  private def getFinancialDetail(mainTransaction: Option[String], transactionId: Option[String]): FinancialDetail = FinancialDetail(
    taxYear = "2025",
    mainTransaction = mainTransaction,
    transactionId = transactionId,
    outstandingAmount = Some(100),
    items = None
  )

  "MakingPaymentService.createViewModel" should {

    "set hasInterest when any financial details response has accruing interest" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(DocumentDetail(
              taxYear = 2025,
              transactionId = "charge-with-accruing-interest",
              documentDescription = None,
              documentText = None,
              outstandingAmount = 100,
              originalAmount = 100,
              documentDate = LocalDate.of(2025, 4, 6),
              accruingInterestAmount = Some(1.67)
            ))
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasInterest) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "not set hasInterest when the interest section is suppressed" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(DocumentDetail(
              taxYear = 2025,
              transactionId = "charge-with-accruing-interest",
              documentDescription = None,
              documentText = None,
              outstandingAmount = 100,
              originalAmount = 100,
              documentDate = LocalDate.of(2025, 4, 6),
              accruingInterestAmount = Some(1.67)
            ))
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties", showInterestSection = false)
        .futureValue

      result.map(_.hasInterest) shouldBe Some(false)
      result.map(_.hasAdditionalSections) shouldBe Some(false)
    }

    "not set hasInterest for overdue amounts without accruing interest" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(2025 -> financialDetailsModel(overDueAmount = 1.67))))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasInterest) shouldBe Some(false)
      result.map(_.hasAdditionalSections) shouldBe Some(false)
    }

    "not set hasInterest for crystallised interest without accruing interest" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(DocumentDetail(
              taxYear = 2025,
              transactionId = "crystallised-interest",
              documentDescription = None,
              documentText = None,
              outstandingAmount = 0,
              originalAmount = 100,
              documentDate = LocalDate.of(2025, 4, 6),
              interestOutstandingAmount = Some(1.67),
              latePaymentInterestAmount = Some(1.67)
            ))
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasInterest) shouldBe Some(false)
      result.map(_.hasAdditionalSections) shouldBe Some(false)
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

    "set hasPenalty when there is an outstanding penalty charge" in {
      val penaltyTransactionId = "penalty-transaction"
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              DocumentDetail(
                taxYear = 2025,
                transactionId = penaltyTransactionId,
                documentDescription = None,
                documentText = None,
                outstandingAmount = 200,
                originalAmount = 200,
                documentDate = LocalDate.of(2025, 4, 6)
              )
            ),
            financialDetails = List(
              FinancialDetail(
                taxYear = "2025",
                mainTransaction = Some("4027"),
                transactionId = Some(penaltyTransactionId),
                outstandingAmount = Some(200),
                items = None
              )
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasPenalty) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasAllPenaltiesOverdue to true when all LSP/LPP outstanding penalties charges overdue" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst),
              getDocumentDetail(penaltyTransactionIdSecond),
              getDocumentDetail(penaltyTransactionIdThird)
            ),
            financialDetails = List(
              getFinancialDetail(Some("4027"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdSecond)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdThird)),
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasAllPenaltiesOverdue) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasAllPenaltiesOverdue to false when at least one of the LSP/LPP outstanding penalties charge not overdue" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst, Some(dateService.getCurrentDate)),
              getDocumentDetail(penaltyTransactionIdSecond),
              getDocumentDetail(penaltyTransactionIdThird)
            ),
            financialDetails = List(
              getFinancialDetail(Some("4027"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdSecond)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdThird)),
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasAllPenaltiesOverdue) shouldBe Some(false)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasOverdueNonPenaltyCharges to true when there is an overdue non-LSP/LPP outstanding penalty charge" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst),
              getDocumentDetail(penaltyTransactionIdSecond),
              getDocumentDetail(penaltyTransactionIdThird),
              getDocumentDetail(nonPenaltyTransactionId)
            ),
            financialDetails = List(
              getFinancialDetail(Some("4027"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdSecond)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdThird)),
              getFinancialDetail(Some(balancingChargeMainTransaction), Some(nonPenaltyTransactionId))
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasOverdueNonPenaltyCharges) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasOverdueNonPenaltyCharges to false when there is no overdue non-LSP/LPP outstanding penalties charge" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst),
              getDocumentDetail(penaltyTransactionIdSecond),
              getDocumentDetail(penaltyTransactionIdThird),
              getDocumentDetail(nonPenaltyTransactionId, Some(dateService.getCurrentDate))
            ),
            financialDetails = List(
              getFinancialDetail(Some("4027"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdSecond)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdThird)),
              getFinancialDetail(Some(balancingChargeMainTransaction), Some(nonPenaltyTransactionId))
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasOverdueNonPenaltyCharges) shouldBe Some(false)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasNotOverdueLPP to true when there is no overdue LPP outstanding penalty charge" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst, Some(dateService.getCurrentDate)),
              getDocumentDetail(penaltyTransactionIdSecond)
            ),
            financialDetails = List(
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdSecond)),
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasNotOverdueLPP) shouldBe Some(true)
      result.map(_.hasAdditionalSections) shouldBe Some(true)
    }

    "set hasNotOverdueLPP to false when there is no overdue LPP outstanding penalty charge" in {
      when(mockFinancialDetailsService.getAllFinancialDetails(any(), any(), any()))
        .thenReturn(Future.successful(List(
          2025 -> financialDetailsModel(
            documentDetails = List(
              getDocumentDetail(penaltyTransactionIdFirst),
              getDocumentDetail(penaltyTransactionIdSecond)
            ),
            financialDetails = List(
              getFinancialDetail(Some("4028"), Some(penaltyTransactionIdFirst)),
              getFinancialDetail(Some("4029"), Some(penaltyTransactionIdSecond)),
            )
          )
        )))

      val result = TestMakingPaymentService
        .createViewModel("/back", "/payment", "/what-you-owe", "/money-in-account", "/penalties")
        .futureValue

      result.map(_.hasNotOverdueLPP) shouldBe Some(false)
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
