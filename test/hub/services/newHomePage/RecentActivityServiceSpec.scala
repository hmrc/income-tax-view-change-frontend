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

package hub.services.newHomePage

import common.auth.MtdItUser
import common.mocks.services.MockDateService
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import common.testUtils.TestSupport
import financials.models.Payment
import financials.models.repaymentHistory.*
import hub.models.newHomePage.{RecentActivityPaymentModel, RecentActivitySubmissionsModel, RecentActivityViewModel, RecentRefundModel}
import hub.mocks.connectors.MockObligationsConnector
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import shared.models.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate

class RecentActivityServiceSpec
  extends TestSupport
    with MockObligationsConnector
    with MockDateService {

  private val service = new RecentActivityService(mockObligationsConnector, mockDateService)

  private val today = LocalDate.of(2026, 4, 14)
  private val within90Days = today.minusDays(10)
  private val outside90Days = today.minusDays(91)

  private def obligation(
                          obligationType: String,
                          start: LocalDate,
                          dateReceived: Option[LocalDate]
                        ): SingleObligationModel =
    SingleObligationModel(
      start = start,
      end = start.plusMonths(3),
      due = start.plusMonths(3),
      obligationType = obligationType,
      dateReceived = dateReceived,
      periodKey = "#001",
      status = StatusFulfilled
    )

  private def payment(amount: BigDecimal, paymentDate: Option[LocalDate]): Payment = {
    Payment(
      taxYear = 9999,
      reference = Some("reference"),
      amount = Some(amount),
      outstandingAmount = Some(0.00),
      method = Some("method"),
      documentDescription = Some("docDescription"),
      lot = Some("lot"), lotItem = Some("lotItem"),
      dueDate = paymentDate,
      documentDate = LocalDate.parse("2022-08-16"),
      transactionId = Some("transactionId")
    )
  }

  private def refund(refundDate: Option[LocalDate]) = {
    RepaymentHistory(
      amountApprovedforRepayment = Some(705.2),
      amountRequested = 800.0,
      repaymentMethod = Some("CARD"),
      totalRepaymentAmount = Some(705.2),
      repaymentItems = Some(Seq(RepaymentItem(
        repaymentSupplementItem = Seq(RepaymentSupplementItem(
          parentCreditReference = Some("ref"),
          amount = Some(500.23),
          fromDate = Some(LocalDate.of(2023, 4, 6)),
          toDate = Some(LocalDate.of(2024, 4, 5)),
          rate = Some(20.0)
        ))
      ))),
      estimatedRepaymentDate = refundDate,
      creationDate = Some(LocalDate.of(2023, 4, 6)),
      repaymentRequestNumber = "123",
      status = RepaymentHistoryStatus("A")
    )
  }

  private def obligationsModel(
                                obligations: List[SingleObligationModel]
                              ): ObligationsModel =
    ObligationsModel(
      Seq(
        GroupedObligationsModel(
          identification = "test-id",
          obligations = obligations
        )
      )
    )

  "getRecentSubmissionActivity" should {
    "return the most recent annual submission within 90 days" in {
      when(mockDateService.getCurrentDate).thenReturn(today)

      val annualOld = obligation("Crystallisation", LocalDate.of(2022, 4, 6), Some(within90Days.minusDays(5)))
      val annualRecent = obligation("Crystallisation", LocalDate.of(2023, 4, 6), Some(within90Days))
      val obligations = obligationsModel(List(annualOld, annualRecent))
      val result = service.getRecentSubmissionActivity(obligations, Mandated)

      result.mostRecentAnnualSubmission.value shouldBe annualRecent
    }

    "ignore obligations received more than 90 days ago" in {
      when(mockDateService.getCurrentDate).thenReturn(today)

      val oldAnnual = obligation("Crystallisation", LocalDate.of(2021, 4, 6), Some(outside90Days))
      val obligations = obligationsModel(List(oldAnnual))
      val result = service.getRecentSubmissionActivity(obligations, Mandated)

      result.mostRecentAnnualSubmission shouldBe None
    }

    "include quarterly submissions for Voluntary and Mandated ITSA status" in {
      when(mockDateService.getCurrentDate).thenReturn(today)

      val quarterly = obligation("Quarterly", LocalDate.of(2024, 4, 6), Some(within90Days))
      val obligations = obligationsModel(List(quarterly))
      val mandatedResult = service.getRecentSubmissionActivity(obligations, Mandated)
      val voluntaryResult = service.getRecentSubmissionActivity(obligations, Voluntary)

      mandatedResult.mostRecentQuarterlySubmission.value shouldBe quarterly
      voluntaryResult.mostRecentQuarterlySubmission.value shouldBe quarterly
    }

    "exclude quarterly submissions for unsupported ITSA statuses" in {
      when(mockDateService.getCurrentDate).thenReturn(today)

      val quarterly = obligation("Quarterly", LocalDate.of(2024, 4, 6), Some(within90Days))
      val obligations = obligationsModel(List(quarterly))
      val result = service.getRecentSubmissionActivity(obligations, null)

      result.mostRecentQuarterlySubmission shouldBe None
    }
  }

  "getRecentPaymentActivity" should {
    "return the most recent payment made within 90 days" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val recentPayment = payment(BigDecimal(50), Some(within90Days))
      val mostRecentPayment = payment(BigDecimal(100), Some(within90Days.plusDays(5)))

      val result = service.getRecentPaymentActivity(
        List(recentPayment, mostRecentPayment),
      )
      result shouldBe Some(RecentActivityPaymentModel(BigDecimal(100), within90Days.plusDays(5)))
    }
    "ignore payments made more than 90 days ago" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val oldPayment = payment(BigDecimal(50), Some(outside90Days))

      val result = service.getRecentPaymentActivity(
        List(oldPayment)
      )
      result shouldBe None
    }

    "ignore payments without 'effectiveDateOfPayment' field" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val noDatePayment = payment(BigDecimal(50), None)

      val result = service.getRecentPaymentActivity(
        List(noDatePayment)
      )

      result shouldBe None
    }

  }

  "getRecentRefundActivity" should {
    "return the most recent refund within 90 days" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val recentRefund = refund(Some(within90Days))
      val oldRefund = refund(Some(outside90Days))
      val repaymentHistoryModel = RepaymentHistoryModel(List(recentRefund, oldRefund))
      val result = service.getRecentRefundActivity(repaymentHistoryModel, mockDateService)

      result shouldBe Some(RecentRefundModel(recentRefund))
    }

    "return None if there are no refunds within 90 days" in {
      when(mockDateService.getCurrentDate).thenReturn(today)

      val oldRefund = refund(Some(outside90Days))
      val repaymentHistoryModel = RepaymentHistoryModel(List(oldRefund))
      val result = service.getRecentRefundActivity(repaymentHistoryModel, mockDateService)

      result shouldBe None
    }
  }


  "recentActivityCards" should {

    "return no cards for supporting agents" in {
      implicit val supportingAgentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(supportingAgentUser.isSupportingAgent).thenReturn(true)

      val submissions = RecentActivitySubmissionsModel(None, None)
      val result = service.recentActivityCards(submissions, None, None)

      result shouldBe RecentActivityViewModel(Seq.empty)
    }

    "return annual and quarterly cards for primary agents" in {
      implicit val agentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(agentUser.isSupportingAgent).thenReturn(false)
      when(agentUser.isAgent).thenReturn(true)

      val annual = obligation("Crystallisation", LocalDate.of(2023, 4, 6), Some(within90Days))
      val quarterly = obligation("Quarterly", LocalDate.of(2023, 4, 6), Some(within90Days))
      val submissions = RecentActivitySubmissionsModel(Some(annual), Some(quarterly))
      val result = service.recentActivityCards(submissions, None, None)

      result.recentActivityCards.size shouldBe 2
      result.recentActivityCards.head.cardTaxYear.value shouldBe TaxYear.getTaxYear(annual.start)
    }

    "return payment card for primary agents" in {
      implicit val agentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(agentUser.isSupportingAgent).thenReturn(false)
      when(agentUser.isAgent).thenReturn(true)
      val submissions = RecentActivitySubmissionsModel(None, None)
      val payment = RecentActivityPaymentModel(BigDecimal(123.45), within90Days)
      val result = service.recentActivityCards(submissions, Some(payment), None)

      result.recentActivityCards.size shouldBe 1
      result.recentActivityCards.head.cardDate shouldBe within90Days
    }

    "return a refund card when a recent refund is present" in {
      implicit val user: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(user.isSupportingAgent).thenReturn(false)
      when(user.isAgent).thenReturn(false)

      val recentRefund = refund(Some(within90Days))
      val submissions = RecentActivitySubmissionsModel(None, None)
      val refunds = RecentRefundModel(recentRefund)
      val result = service.recentActivityCards(submissions, None, Some(refunds))

      result.recentActivityCards.size shouldBe 1
      result.recentActivityCards.head.linkContentText shouldBe "new.home.recentActivity.recentRefund.link.text"
    }

    "return recent a refund, payment and submission cards when all are present" in {
      implicit val user: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(user.isSupportingAgent).thenReturn(false)
      when(user.isAgent).thenReturn(false)

      val annual = obligation("Crystallisation", LocalDate.of(2023, 4, 6), Some(within90Days))

      val submissions = RecentActivitySubmissionsModel(Some(annual), None)
      val payment = RecentActivityPaymentModel(BigDecimal(123.45), within90Days)
      val refunds = RecentRefundModel(refund(Some(within90Days)))
      val result = service.recentActivityCards(submissions, Some(payment), Some(refunds))

      result.recentActivityCards.size shouldBe 3
    }
  }
}
