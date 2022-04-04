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
import config.featureswitch.{CodingOut, FeatureSwitching}
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.FinancialDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport

import java.time.LocalDate
import scala.concurrent.Future

class WhatYouOweServiceSpec extends TestSupport with FeatureSwitching {

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
  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]
  val currentYearAsInt: Int = LocalDate.now.getYear

  object mockDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse(s"${currentYearAsInt.toString}-04-01")
    override def getCurrentTaxYearEnd(currentDate: LocalDate): Int = currentYearAsInt
  }

  object TestWhatYouOweService extends WhatYouOweService(mockFinancialDetailsService, mockIncomeTaxViewChangeConnector, mockDateService)

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
            chargesList = financialDetailsDueInMoreThan30Days().getAllDocumentDetailsWithDueDates()
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
            chargesList = financialDetailsBalancingCharges.getAllDocumentDetailsWithDueDates()
          )
        }
      }

      "when both financial details return success and with balancing charges returned with mixed outstanding charges" should {
        "return a success empty response back with both outstanding amount zero and no late payment interest" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(Some(0), Some(0))))))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00)
          )
        }
        "return a success empty response with outstanding amount zero and late payment interest amount zero" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(Some(0), Some(0)),
              latePaymentInterestAmount = List(Some(0), Some(0)), interestOutstandingAmount = List(Some(0), Some(0))))))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00)
          )
        }
        "return a success POA2 only response with outstanding amount zero and late payment interest amount non-zero" in {
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(Some(0), Some(0)),
              latePaymentInterestAmount = List(Some(0), Some(10)), interestOutstandingAmount = List(Some(0), Some(10))))))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            chargesList = List(DocumentDetailWithDueDate(
              DocumentDetail(currentYear, "1040000124", Some("ITSA - POA 2"), Some("documentText"), Some(0), Some(12.34), LocalDate.of(2018, 3, 29), Some(10), Some(100),
                Some("latePaymentInterestId"), Some(LocalDate.of(2018, 3, 29)),
                Some(LocalDate.of(2018, 3, 29)), Some(10), Some(100), Some("paymentLotItem"), Some("paymentLot")),
              Some(LocalDate.of(2018, 3, 29)), true)))
        }
      }

      "when coding out is enabled" should {
        "return the codedout documentDetail, cancelled coding out and the class2 nics charge" in {
          enable(CodingOut)
          val dd1 = DocumentDetail(taxYear = "2021", transactionId = id1040000124, documentDescription = Some("TRM New Charge"),
            documentText = Some("Class 2 National Insurance"), outstandingAmount = Some(43.21),
            originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)
          val dd2 = DocumentDetail(taxYear = "2021", transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), outstandingAmount = Some(43.21),
            originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)
          val dd3 = dd1.copy(transactionId = id1040000126, documentText = Some("PAYE Self Assessment"))
          val cd1 = CodingDetails(taxYearReturn = "2021", amountCodedOut = 999.99, taxYearCoding = "2020")
          val cd2 = CodingDetails(taxYearReturn = "2020", amountCodedOut = 99.99, taxYearCoding = "2019")
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
              codingDetails = Some(List(cd1, cd2)),
              documentDetails = List(dd1, dd2, dd3),
              financialDetails = List(
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000124), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-24"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000125), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000126), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out")))))
              )
            ))))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            chargesList = List(DocumentDetailWithDueDate(documentDetail = dd1, dueDate = Some(LocalDate.parse("2021-08-24")), codingOutEnabled = true),
              DocumentDetailWithDueDate(documentDetail = dd2, dueDate = Some(LocalDate.parse("2021-08-25")), codingOutEnabled = true)),
            outstandingChargesModel = None,
            codedOutDocumentDetail = Some(DocumentDetailWithCodingDetails(dd3, cd1))
          )
        }
      }

      "when coding out is disabled" should {
        "not return any coding out details" in {
          disable(CodingOut)
          val dd1 = DocumentDetail(taxYear = "2021", transactionId = id1040000124, documentDescription = Some("TRM New Charge"),
            documentText = Some("Class 2 National Insurance"), outstandingAmount = Some(43.21),
            originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)
          val dd2 = DocumentDetail(taxYear = "2021", transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
            documentText = Some("Cancelled PAYE Self Assessment"), outstandingAmount = Some(43.21),
            originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)
          val dd3 = dd1.copy(transactionId = id1040000126, documentText = Some("PAYE Self Assessment"))
          val cd1 = CodingDetails(taxYearReturn = "2021", amountCodedOut = 999.99, taxYearCoding = "2020")
          when(mockIncomeTaxViewChangeConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
              codingDetails = Some(List(cd1)),
              documentDetails = List(dd1, dd2, dd3),
              financialDetails = List(
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000124), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-24"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000125), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some(id1040000126), Some("transactionDate"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(dueDate = Some("2021-08-25"), dunningLock = Some("Coding out"))))),
              )
            ))))

          TestWhatYouOweService.getWhatYouOweChargesList()(headerCarrier, mtdItUser).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            chargesList = List(DocumentDetailWithDueDate(documentDetail = dd1, dueDate = Some(LocalDate.parse("2021-08-24"))),
              DocumentDetailWithDueDate(documentDetail = dd2, dueDate = Some(LocalDate.parse("2021-08-25")))),
            outstandingChargesModel = None,
            codedOutDocumentDetail = None
          )
        }
      }

    }
  }
}
