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

package services.newHomePage

import auth.MtdItUser
import mocks.connectors.MockObligationsConnector
import mocks.services.MockDateService
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import models.newHomePage.{RecentActivityPaymentModel, RecentActivitySubmissionsModel, RecentActivityViewModel}
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetailsModel, Payment}
import models.obligations.*
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.DateServiceInterface
import testConstants.FinancialDetailsTestConstants
import testUtils.TestSupport

import java.time.LocalDate
import scala.math.BigDecimal

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


  private def paymentAndDocDetail(transactionId: String, amount: BigDecimal, paymentDate: Option[LocalDate]): (Payment, DocumentDetail) = {
    val payment = Payment(
      reference = Some("reference"),
      amount = Some(amount),
      outstandingAmount = Some(0.00),
      method = Some("method"),
      documentDescription = Some("docDescription"),
      lot = Some("lot"), lotItem = Some("lotItem"),
      dueDate = Some(LocalDate.parse("2022-08-16")),
      documentDate = LocalDate.parse("2022-08-16"),
      transactionId = Some(transactionId)
    )
    val document = FinancialDetailsTestConstants.documentDetailModel(
      transactionId = transactionId,
      effectiveDateOfPayment = paymentDate
    )
    (payment, document)
  }

  private def getFinancialDetails(documentDetails: List[DocumentDetail]): List[FinancialDetailsModel] = {
    List(FinancialDetailsModel(
      balanceDetails = FinancialDetailsTestConstants.balanceDetails,
      documentDetails = documentDetails,
      financialDetails = List.empty
    ))
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
      val (payment1, recentDocument) = paymentAndDocDetail("payment1", BigDecimal(50), Some(within90Days))
      val (payment2, mostRecentDocument) = paymentAndDocDetail("payment2", BigDecimal(100), Some(within90Days.plusDays(5)))

      val result = service.getRecentPaymentActivity(
        List(payment1, payment2),
        getFinancialDetails(List(recentDocument, mostRecentDocument))
      )
      result shouldBe Some(RecentActivityPaymentModel(BigDecimal(100), within90Days.plusDays(5), TaxYear.getTaxYear(within90Days.plusDays(5))))
    }
    "ignore payments made more than 90 days ago" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val (payment1, oldDocument) = paymentAndDocDetail("payment1", BigDecimal(50), Some(outside90Days))

      val result = service.getRecentPaymentActivity(
        List(payment1),
        getFinancialDetails(List(oldDocument))
      )
      result shouldBe None
    }

    "ignore payments without 'effectiveDateOfPayment' field" in {
      when(mockDateService.getCurrentDate).thenReturn(today)
      val (payment1, noDateDocument) = paymentAndDocDetail("payment1", BigDecimal(50), None)

      val result = service.getRecentPaymentActivity(
        List(payment1),
        getFinancialDetails(List(noDateDocument))
      )

      result shouldBe None
    }

  }

  "recentActivityCards" should {

    "return no cards for supporting agents" in {
      implicit val supportingAgentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(supportingAgentUser.isSupportingAgent).thenReturn(true)

      val submissions = RecentActivitySubmissionsModel(None, None)
      val result = service.recentActivityCards(submissions, None)

      result shouldBe RecentActivityViewModel(Seq.empty)
    }

    "return annual and quarterly cards for primary agents" in {
      implicit val agentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(agentUser.isSupportingAgent).thenReturn(false)
      when(agentUser.isAgent).thenReturn(true)

      val annual = obligation("Crystallisation", LocalDate.of(2023, 4, 6), Some(within90Days))
      val quarterly = obligation("Quarterly", LocalDate.of(2023, 4, 6), Some(within90Days))
      val submissions = RecentActivitySubmissionsModel(Some(annual), Some(quarterly))
      val result = service.recentActivityCards(submissions, None)

      result.recentActivityCards.size shouldBe 2
      result.recentActivityCards.head.cardTaxYear.value shouldBe TaxYear.getTaxYear(annual.start)
    }

    "return payment card for primary agents" in {
      implicit val agentUser: MtdItUser[_] = MockitoSugar.mock[MtdItUser[_]]

      when(agentUser.isSupportingAgent).thenReturn(false)
      when(agentUser.isAgent).thenReturn(true)
      val submissions = RecentActivitySubmissionsModel(None, None)
      val payment = RecentActivityPaymentModel(BigDecimal(123.45), within90Days, TaxYear.getTaxYear(within90Days))
      val result = service.recentActivityCards(submissions, Some(payment))

      result.recentActivityCards.size shouldBe 1
      result.recentActivityCards.head.cardDate shouldBe within90Days
    }
  }
}
