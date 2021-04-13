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

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.FinancialDetailsTestConstants.{testFinancialDetailsModel, testFinancialDetailsModelWithChargesOfSameType}
import assets.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import auth.MtdItUser
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails.{FinancialDetailsErrorModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesErrorModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import testUtils.TestSupport

import java.time.LocalDate
import scala.concurrent.Future

class PaymentDueServiceSpec extends TestSupport {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = singleBusinessIncomeWithCurrentYear,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some("individual")
  )(FakeRequest())

  val mockFinancialDetailsService = mock[FinancialDetailsService]
  val mockIncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

  object TestPaymentDueService extends PaymentDueService(mockFinancialDetailsService, mockIncomeTaxViewChangeConnector)

  def outstandingChargesModel(dueDate: String) = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val financialDetailsDueInMoreThan30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesDueInMoreThan30Days = outstandingChargesModel(LocalDate.now().plusDays(35).toString)
  val whatYouOweDataWithDataDueInMoreThan30Days = WhatYouOweChargesList(futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    outstandingChargesModel = Some(outstandingChargesDueInMoreThan30Days))

  val financialDetailsDueIn30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesDueIn30Days = outstandingChargesModel(LocalDate.now().plusDays(30).toString)
  val whatYouOweDataWithDataDueIn30Days = WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    outstandingChargesModel = Some(outstandingChargesDueIn30Days))

  val financialDetailsOverdueData = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)
  val outstandingChargesOverdueData = outstandingChargesModel(LocalDate.now().minusDays(30).toString)
  val whatYouOweDataWithOverdueData = WhatYouOweChargesList(overduePaymentList = financialDetailsOverdueData.financialDetails,
    outstandingChargesModel = Some(outstandingChargesOverdueData))

  val financialDetailsWithMixedData = testFinancialDetailsModelWithChargesOfSameType(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(25), Some(50), Some(75)), LocalDate.now().getYear.toString)
  val whatYouOweDataWithMixedData = WhatYouOweChargesList(overduePaymentList = List(financialDetailsWithMixedData.financialDetails(2)),
    dueInThirtyDaysList = List(financialDetailsWithMixedData.financialDetails(1)),
    futurePayments = List(financialDetailsWithMixedData.financialDetails(0)), outstandingChargesModel = Some(OutstandingChargesModel(List())))

  "The PaymentDueService.getWhatYouOweChargesList method" when {
    "when both financial details and outstanding charges return success response and valid data of due more than 30 days" should {
      "return a success response back" in {
        when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueInMoreThan30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days)))

        await(TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)) shouldBe whatYouOweDataWithDataDueInMoreThan30Days
      }
    }
    "when both financial details and outstanding charges return success response and valid data of due in 30 days" should {
      "return a success response back" in {
        when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueIn30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueIn30Days)))

        await(TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)) shouldBe whatYouOweDataWithDataDueIn30Days
      }
      "when both financial details and outstanding charges return success response and valid data of overdue" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueData))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsOverdueData)))

          await(TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)) shouldBe whatYouOweDataWithOverdueData
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData)))

          await(TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)) shouldBe whatYouOweDataWithMixedData
        }
      }
      "when both financial details return success and outstanding charges return 500" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(500, "test message")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days)))

          val res = TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)

          val ex = intercept[Exception](await(res))
          ex.getMessage shouldBe "[PaymentDueService][callOutstandingCharges] Error response while getting outstanding charges"
        }
      }
      "when both financial details return error and outstanding charges return success" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueData))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days, FinancialDetailsErrorModel(500, "test message"))))

          val res = TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)

          val ex = intercept[Exception](await(res))
          ex.getMessage shouldBe "[PaymentDueService][getWhatYouOweChargesList] Error response while getting Unpaid financial details"
        }
      }
      "when both financial details return success and outstanding charges return 404" should {
        "return a success response back" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days)))

          await(TestPaymentDueService.getWhatYouOweChargesList()(headerCarrier, mtdItUser)) shouldBe WhatYouOweChargesList(
            futurePayments = financialDetailsDueInMoreThan30Days.financialDetails)
        }
      }
    }
  }
}
