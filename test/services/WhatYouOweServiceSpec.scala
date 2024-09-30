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
import config.featureswitch.FeatureSwitching
import connectors.FinancialDetailsConnector
import enums.ChargeType.NIC4_WALES
import enums.CodingOutType._
import models.admin.{CodingOut, MFACreditsAndDebits, ReviewAndReconcilePoa}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class WhatYouOweServiceSpec extends TestSupport with FeatureSwitching with ChargeConstants {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = singleBusinessIncomeWithCurrentYear,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some(Individual),
    None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  val mockFinancialDetailsConnector: FinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])
  val currentYearAsInt: Int = 2022
  implicit val headCarrier: HeaderCarrier = headerCarrier

  object mockDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse(s"${currentYearAsInt.toString}-04-01")

    override def getCurrentTaxYearEnd: Int = currentYearAsInt
  }

  object TestWhatYouOweService extends WhatYouOweService(mockFinancialDetailsService, mockFinancialDetailsConnector, mockDateService)

  "The WhatYouOweService.getWhatYouOweChargesList method" when {
    "when both financial details and outstanding charges return success response and valid data of due more than 30 days" should {
      "return a success response back" in {
        when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueInMoreThan30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe whatYouOweDataWithDataDueInMoreThan30Days(
          dueDates = dueDateMoreThan30Days
        )
      }
    }
    "when both financial details and outstanding charges return success response and valid data of due in 30 days" should {
      "return a success response back" in {
        when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueIn30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueIn30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe whatYouOweDataWithDataDueIn30Days()
      }
      "when both financial details and outstanding charges return success response and valid data of overdue" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueDataIt))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsOverdueData())))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe whatYouOweDataWithOverdueDataIt()
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and in future payments" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData1)))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe whatYouOweDataWithMixedData1
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and dueInThirtyDays" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData2)))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe whatYouOweDataWithMixedData2
        }
      }
      "when both financial details return success and outstanding charges return 500" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(500, "test message")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          val res = TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa))

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "Error response while getting outstanding charges"
        }
      }
      "when both financial details return error and outstanding charges return success" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueDataIt))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days(), FinancialDetailsErrorModel(500, "test message"))))

          val res = TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa))

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "Error response while getting Unpaid financial details"
        }
      }
      "when both financial details return success and outstanding charges return 404" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(0.00, 2.00, 2.00, None, None, None, None, Some(100)),
            chargesList = financialDetailsDueInMoreThan30DaysCi()
          )
        }
      }

      "when both financial details return success and with balancing charges returned" should {
        "return a success response back" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsBalancingCharges)))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            chargesList = financialDetailsBalancingChargesCi
          )
        }
      }

      "when both financial details return success and with balancing charges returned with mixed outstanding charges" should {
        "return a success empty response back with both outstanding amount zero and no late payment interest" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0)))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None)
          )
        }
        "return a success empty response with outstanding amount zero and late payment interest amount zero" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0),
              latePaymentInterestAmount = List(Some(0), Some(0)), interestOutstandingAmount = List(Some(0), Some(0))))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None)
          )
        }
        "return a success POA2 only response with outstanding amount zero and late payment interest amount non-zero" in {
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0),
              latePaymentInterestAmount = List(Some(0), Some(10)), interestOutstandingAmount = List(Some(0), Some(10))))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            chargesList = List(poa2))
        }
      }

      "when coding out is enabled" should {
        "return the codedout documentDetail, cancelled coding out and the class2 nics charge" in {
          enable(CodingOut)
          val dd1 = DocumentDetail(taxYear = 2021, transactionId = id1040000124, documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-24")),
            documentDueDate = Some(LocalDate.parse("2021-08-24")))
          val dd2 = DocumentDetail(taxYear = 2021, transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CANCELLED), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
            documentDueDate = Some(LocalDate.parse("2021-08-25")))
          val dd3 = dd1.copy(transactionId = id1040000126, documentText = Some(CODING_OUT_ACCEPTED), amountCodedOut = Some(2500.00))
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(dd1, dd2, dd3),
              financialDetails = List(
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out")))))
              )
            ))))
          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            chargesList = List(
              balancingChargeNics2.copy(dueDate = Some(LocalDate.parse("2021-08-24"))),
              balancingChargeCancelled.copy(dueDate =Some(LocalDate.parse("2021-08-25")))),
            outstandingChargesModel = None,
            codedOutDocumentDetail = Some(balancingChargePaye)
          )
        }
      }

      "when coding out is disabled" should {

        "not return any coding out details" in {

          disable(CodingOut)
          val dd1 = DocumentDetail(taxYear = 2021, transactionId = id1040000124, documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None, latePaymentInterestId = None,
            interestFromDate = Some(LocalDate.parse("2019-05-25")), interestEndDate = Some(LocalDate.parse("2019-06-25")),
            latePaymentInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-24")),
            documentDueDate = Some(LocalDate.parse("2021-08-24")))
          val dd2 = DocumentDetail(taxYear = 2021, transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CANCELLED), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None, latePaymentInterestId = None,
            interestFromDate = Some(LocalDate.parse("2019-05-25")), interestEndDate = Some(LocalDate.parse("2019-06-25")),
            latePaymentInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
            documentDueDate = Some(LocalDate.parse("2021-08-25")))
          val dd3 = dd1.copy(transactionId = id1040000126, documentText = Some(CODING_OUT_ACCEPTED))
          when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(dd1, dd2, dd3),
              financialDetails = List(
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
              )
            ))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            chargesList =  List(),
// TODO: Is this test misnamed? These shouldn't be here?
//              List(
//              balancingChargeNics2.copy(dueDate = Some(LocalDate.parse("2021-08-24"))),
//             balancingChargeCancelled.copy(dueDate =Some(LocalDate.parse("2021-08-25")))),
            outstandingChargesModel = None,
            codedOutDocumentDetail = None
          )
        }
      }
    }
    "with MFA Debits and non-MFA Debits charges" should {
      def testGetWhatYouOweChargesList(MFADebitsEnabled: Boolean, financialDetails: FinancialDetailsModel, expectedResult: WhatYouOweChargesList): Unit = {
        if (MFADebitsEnabled) enable(MFACreditsAndDebits) else disable(MFACreditsAndDebits)
        when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(OutstandingChargesModel(List())))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetails)))
        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe expectedResult
      }

      "return MFA Debits and non-MFA debits with FS ENABLED" in {
        testGetWhatYouOweChargesList(MFADebitsEnabled = true, financialDetails = financialDetailsMFADebits, expectedResult = whatYouOweDataWithMFADebitsData)
        testGetWhatYouOweChargesList(MFADebitsEnabled = true, financialDetails = financialDetailsWithMixedData1, expectedResult = whatYouOweDataWithMixedData1)
      }
      "return non-MFA debits and no MFA debits with FS DISABLED" in {
        testGetWhatYouOweChargesList(MFADebitsEnabled = false, financialDetails = financialDetailsWithMixedData1, expectedResult = whatYouOweDataWithMixedData1)
        testGetWhatYouOweChargesList(MFADebitsEnabled = false, financialDetails = financialDetailsMFADebits, expectedResult = whatYouOweEmptyMFA)
      }
    }

    "with ReviewAndReconcilePoa" should {
      def testGetWhatYouOweChargesList(ReviewReconcileEnabled: Boolean, financialDetails: FinancialDetailsModel, expectedResult: WhatYouOweChargesList): Unit = {
        if (ReviewReconcileEnabled) enable(ReviewAndReconcilePoa) else disable(ReviewAndReconcilePoa)
        when(mockFinancialDetailsConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(OutstandingChargesModel(List())))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetails)))
        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(ReviewAndReconcilePoa)).futureValue shouldBe expectedResult
      }
      "return list including POA extra charges" in {
//        testGetWhatYouOweChargesList(ReviewReconcileEnabled = true, financialDetails = financialDetailsReviewAndReconcile, expectedResult = whatYouOweWithReviewReconcileData)
//        testGetWhatYouOweChargesList(ReviewReconcileEnabled = true, financialDetails = financialDetailsWithMixedData4, expectedResult = whatYouOweDataWithMixedData4Unfiltered)
      }
      "return list excluding POA extra charges" in {
        testGetWhatYouOweChargesList(ReviewReconcileEnabled = false, financialDetails = financialDetailsReviewAndReconcile, expectedResult = whatYouOweEmptyRandR)
        testGetWhatYouOweChargesList(ReviewReconcileEnabled = false, financialDetails = financialDetailsWithMixedData4, expectedResult = whatYouOweDataWithMixedData4Filtered)
      }
    }
  }

  "WhatYouOweService.getCreditCharges method" should {
    "return a list of credit charges" when {
      "a successful response is received in all tax year calls" in {
        when(mockFinancialDetailsService.getAllCreditFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = creditDocumentDetailList,
            financialDetails = List(
              FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
              FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
              FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"))))),
            )
          ))))

        TestWhatYouOweService.getCreditCharges().futureValue shouldBe creditDocumentDetailList
      }
    }
    "handle an error" when {
      "the financial service has returned an error in all tax year calls" in {
        when(mockFinancialDetailsService.getAllCreditFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(500, "INTERNAL_SERVER ERROR"))))

        val result = TestWhatYouOweService.getCreditCharges().failed.futureValue

        result shouldBe an[Exception]
        result.getMessage shouldBe "Error response while getting Unpaid financial details"
      }
    }
  }
  "WhatYouOweService.validChargeType val" should {

    def testValidChargeType(chargeItems: List[ChargeItem], expectedResult: Boolean): Unit = {
      assertResult(expected = expectedResult)(actual = chargeItems.forall(chargeItem => TestWhatYouOweService.validChargeTypeCondition(chargeItem)))
    }

    "validate Class 2 National Insurance charges" in {
      TestWhatYouOweService.validChargeTypeCondition(chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Nics2)))
    }

    "validate Payment on Accounts" in {
      testValidChargeType(
        List(
          chargeItemModel(transactionType = PaymentOnAccountOne),
          chargeItemModel(transactionType = PaymentOnAccountTwo)),true)
    }

    "validate Payment on Account Review and Reconcile" in {
      testValidChargeType(
        List(
          chargeItemModel(transactionType = PaymentOnAccountOneReviewAndReconcile),
          chargeItemModel(transactionType = PaymentOnAccountTwoReviewAndReconcile)), true)
    }

    "validate any balancing charges" in {

      testValidChargeType(
        List(
          chargeItemModel(transactionType = BalancingCharge, subTransactionType = None),
          chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Accepted)),
          chargeItemModel(transactionType = BalancingCharge, subTransactionType = Some(Cancelled))
        ), true)
    }

    "not validate any MFA" in {
      TestWhatYouOweService.validChargeTypeCondition(chargeItemModel(transactionType = MfaDebitCharge))
    }
  }
}
