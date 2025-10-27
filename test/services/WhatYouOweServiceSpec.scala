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

import audit.AuditingService
import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.featureswitch.FeatureSwitching
import connectors.{FinancialDetailsConnector, OutstandingChargesConnector}
import enums.ChargeType.NIC4_WALES
import enums.CodingOutType._
import models.admin.{ClaimARefundR18, FilterCodedOutPoas, PenaltiesAndAppeals}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class WhatYouOweServiceSpec extends TestSupport with FeatureSwitching with ChargeConstants {

  implicit val mtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncomeWithCurrentYear)

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  val mockClaimToAdjustService: ClaimToAdjustService = mock(classOf[ClaimToAdjustService])
  val mockSelfServeTimeToPayService: SelfServeTimeToPayService = mock(classOf[SelfServeTimeToPayService])
  val mockFinancialDetailsConnector: FinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])
  val mockOutstandingChargesConnector: OutstandingChargesConnector = mock(classOf[OutstandingChargesConnector])
  val mockAuditingService: AuditingService = mock(classOf[AuditingService])
  val currentYearAsInt: Int = 2022
  implicit val headCarrier: HeaderCarrier = headerCarrier

  object mockDateService extends DateService {
    override def getCurrentDate: LocalDate = LocalDate.parse(s"${currentYearAsInt.toString}-04-01")

    override def getCurrentTaxYearEnd: Int = currentYearAsInt
  }

  object TestWhatYouOweService extends WhatYouOweService(
    auditingService = mockAuditingService,
    financialDetailsService = mockFinancialDetailsService,
    claimToAdjustService = mockClaimToAdjustService,
    selfServeTimeToPayService = mockSelfServeTimeToPayService,
    financialDetailsConnector = mockFinancialDetailsConnector,
    outstandingChargesConnector = mockOutstandingChargesConnector,
    dateService = mockDateService)

  "The WhatYouOweService.getWhatYouOweChargesList method" when {
    "when both financial details and outstanding charges return success response and valid data of due more than 30 days" should {
      "return a success response back" in {
        when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueInMoreThan30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
          isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
          mainChargeIsNotPaidFilter,
          claimARefundR18Enabled = true).futureValue shouldBe whatYouOweDataWithDataDueInMoreThan30Days(dueDates = dueDateMoreThan30Days)
      }
    }
    "when both financial details and outstanding charges return success response and valid data of due in 30 days" should {
      "return a success response back" in {
        when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(outstandingChargesDueIn30Days))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetailsDueIn30Days())))

        TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
          isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
          mainChargeIsNotPaidFilter,
          claimARefundR18Enabled = true).futureValue shouldBe whatYouOweDataWithDataDueIn30Days()
      }
      "when both financial details and outstanding charges return success response and valid data of overdue" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueDataIt))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsOverdueData())))

          TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe whatYouOweDataWithOverdueDataIt()
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and in future payments" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData1)))

          TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe whatYouOweDataWithMixedData1()
        }
      }
      "when both financial details and outstanding charges return success response and valid data of mixed due dates of overdue and dueInThirtyDays" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesModel(List())))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithMixedData2)))

          TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe whatYouOweDataWithMixedData2
        }
      }
      "when both financial details return success and outstanding charges return 500" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(500, "test message")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          val res = TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true)

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "Error response while getting outstanding charges"
        }
      }
      "when both financial details return error and outstanding charges return success" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(outstandingChargesOverdueDataIt))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days(), FinancialDetailsErrorModel(500, "test message"))))

          val res = TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true)

          val ex = res.failed.futureValue
          ex shouldBe an[Exception]
          ex.getMessage shouldBe "Error response while getting Unpaid financial details"
        }
      }
      "when both financial details return success and outstanding charges return 404" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsDueInMoreThan30Days())))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(0.00, 2.00, 2.00, Some(100), None, None, Some(350), None, None, Some(100)),
            chargesList = financialDetailsDueInMoreThan30DaysCi()
          )
        }
      }

      "when both financial details return success and with balancing charges returned" should {
        "return a success response back" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsBalancingCharges)))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
            chargesList = financialDetailsBalancingChargesCi
          )
        }
      }

      "when both financial details return success and with balancing charges returned with mixed outstanding charges" should {
        "return a success empty response back with both outstanding amount zero and no late payment interest" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0)))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None)
          )
        }
        "return a success empty response with outstanding amount zero and accruing interest amount zero" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0),
              accruingInterestAmount = List(Some(0), Some(0)), interestOutstandingAmount = List(Some(0), Some(0))))))

          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None)
          )
        }
        "return a success POA2 only response with outstanding amount zero and accruing interest amount non-zero" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(0, 0),
              accruingInterestAmount = List(Some(0), Some(10)), interestOutstandingAmount = List(Some(0), Some(10))))))

          TestWhatYouOweService.getWhatYouOweChargesList(
            isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
            chargesList = List(poa2))
        }
      }

      "when coding out is enabled" should {
        "return the codedout documentDetail, cancelled coding out and the class2 nics charge" in {
          val dd1 = DocumentDetail(taxYear = 2021, transactionId = id1040000124, documentDescription = Some("TRM New Charge"),
            documentText = Some(CODING_OUT_CLASS2_NICS), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-24")),
            documentDueDate = Some(LocalDate.parse("2021-08-24")))
          val dd2 = DocumentDetail(taxYear = 2021, transactionId = id1040000125, documentDescription = Some("TRM New Charge"),
            documentText = Some("Document Text"), outstandingAmount = 43.21,
            originalAmount = 43.21, documentDate = LocalDate.of(2018, 3, 29),
            interestOutstandingAmount = None, interestRate = None,
            latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
            interestEndDate = Some(LocalDate.parse("2019-06-25")), accruingInterestAmount = None,
            effectiveDateOfPayment = Some(LocalDate.parse("2021-08-25")),
            documentDueDate = Some(LocalDate.parse("2021-08-25")))
          val dd3 = dd1.copy(transactionId = id1040000126, documentText = Some("Document Text"), amountCodedOut = Some(2500.00))
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
              codingDetails = List(CodingDetails(Some(2500.00), Some("2021"))),
              documentDetails = List(dd1, dd2, dd3),
              financialDetails = List(
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000124), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000125), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-25")), dunningLock = Some("Coding out"),codedOutStatus = Some("C"))))),
                FinancialDetail("2021", Some("SA Balancing Charge"), Some("4910"), Some(id1040000126), None, Some("ABCD1234"), Some("type"), Some(100), Some(100),
                  Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(dueDate = Some(LocalDate.parse("2021-08-24")), dunningLock = Some("Coding out"), codedOutStatus = Some("I")))))
              )
            ))))
          TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
            isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
            chargesList = List(
              balancingChargeNics2.copy(dueDate = Some(LocalDate.parse("2021-08-24")), chargeReference = Some("ABCD1234")),
              balancingChargeCancelled.copy(dueDate =Some(LocalDate.parse("2021-08-25")), chargeReference = Some("ABCD1234"))),
            outstandingChargesModel = None,
            codedOutDetails = Some(balancingCodedOut)
          )
        }
        "not return PoA charges if they have an amount coded out and filtering FS enabled" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(1000, 400),
              amountCodedOut = List(Some(30), Some(70))))))

          TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled = true,
            isPenaltiesEnabled = true,
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
            chargesList = List())
        }
        "return PoA charges if they have an amount coded out and filtering FS disabled" in {
          when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
            .thenReturn(Future.successful(OutstandingChargesErrorModel(404, "NOT_FOUND")))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(financialDetailsWithOutstandingChargesAndLpi(outstandingAmount = List(1000, 400),
              amountCodedOut = List(Some(30), Some(70))))))

          TestWhatYouOweService.getWhatYouOweChargesList(isFilterCodedOutPoasEnabled =  false,
            isPenaltiesEnabled = true,
            mainChargeIsNotPaidFilter,
            claimARefundR18Enabled = true).futureValue shouldBe WhatYouOweChargesList(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
            chargesList = List(poa2WithCodedOut, poa1WithCodedOut))
        }
      }
    }

    "with MFA Debits and non-MFA Debits charges" should {
      def testGetWhatYouOweChargesList(financialDetails: FinancialDetailsModel, expectedResult: WhatYouOweChargesList): Unit = {
        when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(OutstandingChargesModel(List())))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetails)))
        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
          isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
          mainChargeIsNotPaidFilter,
          claimARefundR18Enabled = true).futureValue shouldBe expectedResult
      }

      "return MFA Debits and non-MFA debits" in {
        testGetWhatYouOweChargesList(financialDetails = financialDetailsMFADebits, expectedResult = whatYouOweDataWithMFADebitsData)
        testGetWhatYouOweChargesList(financialDetails = financialDetailsWithMixedData1, expectedResult = whatYouOweDataWithMixedData1())
      }
    }

    "with ReviewAndReconcilePoa" should {
      def testGetWhatYouOweChargesList(financialDetails: FinancialDetailsModel,
                                       expectedResult: WhatYouOweChargesList): Unit = {
        when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(OutstandingChargesModel(List())))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetails)))
        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
          isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
          mainChargeIsNotPaidFilter,
          claimARefundR18Enabled = true).futureValue shouldBe expectedResult
      }

      "return list including POA extra charges" in {
        //testGetWhatYouOweChargesList(financialDetails = financialDetailsReviewAndReconcile, expectedResult = whatYouOweWithReviewReconcileData)
        //testGetWhatYouOweChargesList(financialDetails = financialDetailsWithMixedData4, expectedResult = whatYouOweDataWithMixedData4Unfiltered)
      }
      }

    "with Penalties And Accruals" should {
      def testGetWhatYouOweChargesList(penaltiesEnabled: Boolean, financialDetails: FinancialDetailsModel, expectedResult: WhatYouOweChargesList): Unit = {
        enable(ClaimARefundR18)
        if (penaltiesEnabled) enable(PenaltiesAndAppeals) else disable(PenaltiesAndAppeals)
        when(mockOutstandingChargesConnector.getOutstandingCharges(any(), any(), any())(any()))
          .thenReturn(Future.successful(OutstandingChargesModel(List())))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List(financialDetails)))
        TestWhatYouOweService.getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas),
          isPenaltiesEnabled = penaltiesEnabled,
          mainChargeIsNotPaidFilter,
          claimARefundR18Enabled = true).futureValue shouldBe expectedResult
      }

      "return list including penalties" in {
        testGetWhatYouOweChargesList(penaltiesEnabled = true, financialDetails = financialDetailsModelLatePaymentPenalties, expectedResult = whatYouOweLatePaymentPenalties)
        testGetWhatYouOweChargesList(penaltiesEnabled = true, financialDetails = financialDetailsWithMixedData4Penalties, expectedResult = whatYouOweDataWithMixedDate4PenaltiesUnfilered)
      }
      "return list excluding penalties" in {
        testGetWhatYouOweChargesList(penaltiesEnabled = false, financialDetails = financialDetailsModelLatePaymentPenalties, expectedResult = whatYouOweEmpty)
        testGetWhatYouOweChargesList(penaltiesEnabled = false, financialDetails = financialDetailsWithMixedData4Penalties, expectedResult = whatYouOweDataWithMixedData4Filtered)
      }
    }
  }

  "WhatYouOweService.validChargeType val" should {

    def testValidChargeType(chargeItems: List[ChargeItem], expectedResult: Boolean): Unit = {
      assertResult(expected = expectedResult)(actual = chargeItems.forall(chargeItem => ChargeItem.isAKnownTypeOfCharge(chargeItem)))
    }

    "validate Class 2 National Insurance charges" in {
      ChargeItem.isAKnownTypeOfCharge(chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2)))
    }

    "validate Payment on Accounts" in {
      testValidChargeType(
        List(
          chargeItemModel(transactionType = PoaOneDebit),
          chargeItemModel(transactionType = PoaTwoDebit)),true)
    }

    "validate Payment on Account Review and Reconcile" in {
      testValidChargeType(
        List(
          chargeItemModel(transactionType = PoaOneReconciliationDebit),
          chargeItemModel(transactionType = PoaTwoReconciliationDebit)), true)
    }

    "validate any balancing charges" in {

      testValidChargeType(
        List(
          chargeItemModel(transactionType = BalancingCharge, codedOutStatus = None),
          chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted)),
          chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled))
        ), true)
    }

    "not validate any MFA" in {
      ChargeItem.isAKnownTypeOfCharge(chargeItemModel(transactionType = MfaDebitCharge))
    }
  }
}

