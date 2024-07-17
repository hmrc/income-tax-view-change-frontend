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
import connectors.FinancialDetailsConnector
import models.core.ErrorModel
import models.financialDetails._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import testConstants.ANewCreditAndRefundModel
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.FinancialDetailsTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.concurrent.Future

class CreditServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessesAndPropertyIncome.copy(yearOfMigration = Some(s"${dateService.getCurrentTaxYearEnd - 1 }")),
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some(Individual),
    None
  )(FakeRequest())

  val mockFinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])

 class TestCreditService extends CreditService(mockFinancialDetailsConnector, dateService)

  "CreditService.getCreditCharges method" should {
    "return a list of financial details credit charges" when {
      "a successful response is received in all tax year calls" in {

        when(mockFinancialDetailsConnector.getCreditsAndRefund(ArgumentMatchers.eq(2023), any())(any(), any()))
          .thenReturn(Future.successful(Right(
            ANewCreditAndRefundModel()
              .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
              .get())))

        when(mockFinancialDetailsConnector.getCreditsAndRefund(ArgumentMatchers.eq(2024), any())(any(), any()))
          .thenReturn(Future.successful(Right(
            ANewCreditAndRefundModel()
              .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0)
              .get())))

        new TestCreditService().getAllCredits(mtdItUser, headerCarrier).futureValue shouldBe ANewCreditAndRefundModel()
          .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
          .withBalancingChargeCredit(LocalDate.parse("2023-08-16"), 200.0)
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

}
