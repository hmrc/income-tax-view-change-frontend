/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import connectors.FinancialDetailsConnector
import models.core.ErrorModel
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import testConstants.ANewCreditAndRefundModel
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.concurrent.Future

class CreditServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), businessesAndPropertyIncome.copy(yearOfMigration = Some(s"${dateService.getCurrentTaxYearEnd - 1 }")))

  val mockFinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])

 class TestCreditService extends CreditService(mockFinancialDetailsConnector, dateService)

  //Remove once deprecated method removed
  "CreditService.getAllCredits method" should {
    "return a list of financial details credit charges" when {

      "a successful response is received in all tax year calls" in {

        when(mockFinancialDetailsConnector.getCreditsAndRefund(ArgumentMatchers.eq(TaxYear.forYearEnd(2023)), any())(any(), any()))
          .thenReturn(Future.successful(Right(
            ANewCreditAndRefundModel()
              .withFirstRefund(10)
              .withSecondRefund(20)
              .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0, "balancing01")
              .get())))

        when(mockFinancialDetailsConnector.getCreditsAndRefund(ArgumentMatchers.eq(TaxYear.forYearEnd(2024)), any())(any(), any()))
          .thenReturn(Future.successful(Right(
            ANewCreditAndRefundModel()
              .withFirstRefund(10)
              .withSecondRefund(20)
              .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0, "balancing02")
              .get())))

        new TestCreditService().getAllCredits(mtdItUser, headerCarrier).futureValue shouldBe ANewCreditAndRefundModel()
          .withFirstRefund(10)
          .withSecondRefund(20)
          .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0, "balancing01")
          .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0, "balancing02")
          .get()
      }
    }

    "handle an error" when {
      "the financial service has returned an error in all tax year calls" in {
        when(mockFinancialDetailsConnector.getCreditsAndRefund(any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(ErrorModel(500, "INTERNAL_SERVER ERROR"))))

        val result = new TestCreditService().getAllCredits(mtdItUser, headerCarrier).failed.futureValue

        result shouldBe an[Exception]
        result.getMessage shouldBe "Error response while getting Unpaid financial details"
      }
    }
  }

  "CreditService.getAllCreditsV2 method" should {
    "return a list of financial details credit charges" when {

      "a successful response is received in all tax year calls" in {

        when(mockFinancialDetailsConnector.getCreditsAndRefund(ArgumentMatchers.eq(TaxYear.forYearEnd(2023)), ArgumentMatchers.eq(TaxYear.forYearEnd(2024)), any())(any(), any()))
          .thenReturn(Future.successful(Right(
            ANewCreditAndRefundModel()
              .withFirstRefund(10)
              .withSecondRefund(20)
              .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
              .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0)
              .get())))

        new TestCreditService().getAllCreditsV2(mtdItUser, headerCarrier).futureValue shouldBe ANewCreditAndRefundModel()
          .withFirstRefund(10)
          .withSecondRefund(20)
          .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
          .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0)
          .get()
      }
    }

    "handle an error" when {
      "the financial service has returned an error in all tax year calls" in {
        when(mockFinancialDetailsConnector.getCreditsAndRefund(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(ErrorModel(500, "INTERNAL_SERVER ERROR"))))

        val result = new TestCreditService().getAllCreditsV2(mtdItUser, headerCarrier).failed.futureValue

        result shouldBe an[Exception]
        result.getMessage shouldBe "Error response while getting Unpaid financial details"
      }
    }
  }
}
