/*
 * Copyright 2022 HM Revenue & Customs
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
import models.financialDetails._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import services.CreditService.maybeBalanceDetails
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.FinancialDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport

import scala.concurrent.Future

class CreditServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = singleBusinessIncomeWithCurrentYear,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some("Individual"),
    None
  )(FakeRequest())

  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])

  object service extends CreditService(mockFinancialDetailsService)

  "CreditService.getCreditCharges method" should {
    "return a list of financial details credit charges" when {
      "a successful response is received in all tax year calls" in {

        when(mockFinancialDetailsService.getAllCreditChargesandPaymentsFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditAndRefundCharge)))

        service.getCreditCharges()(headerCarrier, mtdItUser).futureValue shouldBe List(financialDetailCreditAndRefundCharge)
      }
    }

    "handle an error" when {
      "the financial service has returned an error in all tax year calls" in {
        when(mockFinancialDetailsService.getAllCreditChargesandPaymentsFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(500, "INTERNAL_SERVER ERROR"))))

        val result = service.getCreditCharges()(headerCarrier, mtdItUser).failed.futureValue

        result shouldBe an[Exception]
        result.getMessage shouldBe "[CreditService][getCreditCharges] Error response while getting Unpaid financial details"
      }
    }
  }

  "CreditService.maybeBalanceDetails method" should {
    "return an some of balance details" when {
      "a successful response is received in all tax year calls" in {

        maybeBalanceDetails(List(financialDetailCreditAndRefundCharge)) shouldBe Some(financialDetailCreditAndRefundCharge.balanceDetails)
      }
    }

    "return none" when {
      "a successful response is received in all tax year calls but it returns an empty list" in {

        maybeBalanceDetails(List.empty) shouldBe None
      }
    }
  }
}
