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
import org.mockito.Mockito.when
import play.api.test.FakeRequest
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

  val mockFinancialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]

  object service extends CreditService(mockFinancialDetailsService)

  "CreditService.getCreditCharges method" should {
    "return a list of financial details credit charges" when {
      "a successful response is received in all tax year calls" in {
       val  financialDetailCreditCharge = FinancialDetailsModel(
         balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(6.00), Some(2.00), Some(4.00)),
         codingDetails = None,
         documentDetails = creditDocumentDetailList,
         financialDetails = List(
           FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000124), Some("transactionDate"), Some("type"), Some(100), Some(100),
             Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-24"))))),
           FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000125), Some("transactionDate"), Some("type"), Some(100), Some(100),
             Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out"))))),
           FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000126), Some("transactionDate"), Some("type"), Some(100), Some(100),
             Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out"))))),
         )
       )

        when(mockFinancialDetailsService.getAllCreditFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditCharge)))

        service.getCreditCharges()(headerCarrier, mtdItUser).futureValue shouldBe List(financialDetailCreditCharge)
      }
    }

    "handle an error" when {
      "the financial service has returned an error in all tax year calls" in {
        when(mockFinancialDetailsService.getAllCreditFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(500, "INTERNAL_SERVER ERROR"))))

        val result = service.getCreditCharges()(headerCarrier, mtdItUser).failed.futureValue

        result shouldBe an[Exception]
        result.getMessage shouldBe "[CreditService][getCreditCharges] Error response while getting Unpaid financial details"
      }
    }
  }
}
