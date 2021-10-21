/*
 * Copyright 2021 HM Revenue & Customs
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

import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.FinancialDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import auth.MtdItUser
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails.{BalanceDetails, FinancialDetailsErrorModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import testUtils.TestSupport

import scala.concurrent.Future

class WhatYouOweServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = singleBusinessIncomeWithCurrentYear,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some("Individual"),
    None
  )(FakeRequest())

  val mockFinancialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

  object TestWhatYouOweService extends WhatYouOweService(mockFinancialDetailsService, mockIncomeTaxViewChangeConnector)

  "The WhatYouOweService.getWhatYouOweChargesList method" when {
    "when both financial details and outstanding charges return success response and valid data of due more than 30 days" should {
      "return a success response back" in {
        when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueInMoreThan30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe whatYouOweDataWithDataDueInMoreThan30Days()
      }
    }
    "when both financial details and outstanding charges return success response and valid data of due in 30 days" should {
      "return a success response back" in {
        when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueIn30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueIn30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe whatYouOweDataWithDataDueIn30Days()
      }
      "when both financial details and outstanding charges return success response and valid data of overdue" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueData))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsOverdueData())))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe whatYouOweDataWithOverdueData()
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and in future payments" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData1)))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe whatYouOweDataWithMixedData1
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and dueInThirtyDays" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData2)))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe whatYouOweDataWithMixedData2
        }
      }
      "when both financial details return success and outstanding charges return 500" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(500, "test message")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          val res = TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "[WhatYouOweService][callOutstandingCharges] Error response while getting outstanding charges"
        }
      }
      "when both financial details return error and outstanding charges return success" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueData))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days(), FinancialDetailsErrorModel(500, "test message"))))

          val res = TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "[WhatYouOweService][getWhatYouOweChargesList] Error response while getting Unpaid financial details"
        }
      }
      "when both financial details return success and outstanding charges return 404" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(0.00, 2.00, 2.00),
            futurePayments = financialDetailsDueInMoreThan30Days().getAllDocumentDetailsWithDueDates
          )
        }
      }

			"when both financial details return success and with balancing charges returned" should {
				"return a success response back" in {
					when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
						.thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
					when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
						.thenReturn(Future.successful(List(financialDetailsBalancingCharges)))

					TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
						overduePaymentList = financialDetailsBalancingCharges.getAllDocumentDetailsWithDueDates
					)
				}
			}
    }
  }
}
