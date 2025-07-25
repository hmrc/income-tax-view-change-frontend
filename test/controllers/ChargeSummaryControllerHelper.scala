/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import enums.AmendedReturnReversalReason
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, NIC4_WALES}
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.{MockChargeHistoryService, MockFinancialDetailsService}
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.financialDetail

import java.time.LocalDate

trait ChargeSummaryControllerHelper  extends MockAuthActions
  with ImplicitDateFormatter
  with ChargeConstants
  with MockFinancialDetailsService
  with MockChargeHistoryService {

  def financialDetailsWithLocks(taxYear: Int): List[FinancialDetail] = List(
    financialDetail(taxYear = taxYear, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(taxYear = taxYear, chargeType = NIC4_WALES, dunningLock = Some("Stand over order"))
  )

  def mockGetReviewAndReconcileCredit(transactionType: TransactionType): Unit =
    when(mockChargeHistoryService.getReviewAndReconcileCredit(any(), any(), any()))
      .thenReturn(Some(
        ChargeItem(
          transactionId = "transactionId",
          taxYear = TaxYear(2017, 2018),
          transactionType = transactionType,
          codedOutStatus = None,
          documentDate = LocalDate.of(2018, 1, 1),
          dueDate = Some(LocalDate.of(2018, 1, 1)),
          originalAmount = 1000,
          outstandingAmount = 0,
          interestOutstandingAmount = None,
          latePaymentInterestAmount = None,
          interestFromDate = None,
          interestEndDate = None,
          interestRate = None,
          lpiWithDunningLock = None,
          amountCodedOut = None,
          dunningLock = false,
          poaRelevantAmount = None,
          chargeReference = Some("chargeRef")
        )
      ))

  def testChargeHistoryModel(): ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", None)

  def emptyAdjustmentHistoryModel: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1000, None, AmendedReturnReversalReason), List())

  val errorHeading: String = messages("standardError.heading")
  val successHeadingForPOA1 = s"${messages("chargeSummary.paymentOnAccount1.text")}"
  val successHeadingForBCD = s"${messages("chargeSummary.balancingCharge.text")}"

  def successCaption(startYear: String, endYear: String) = s"$startYear to $endYear tax year"
  def successHeadingForRAR1 = s"${messages("chargeSummary.reviewAndReconcilePoa1.text")}"

  def successHeadingForRAR2 = s"${messages("chargeSummary.reviewAndReconcilePoa2.text")}"

  def successHeadingRAR1Interest = s"${messages("chargeSummary.lpi.reviewAndReconcilePoa1.text")}"

  val dunningLocksBannerHeading: String = messages("chargeSummary.dunning.locks.banner.title")
  val paymentBreakdownHeading: String = messages("chargeSummary.paymentBreakdown.heading")
  val paymentHistoryHeadingForPOA1Charge: String = messages("chargeSummary.chargeHistory.Poa1heading")
  val paymentHistoryHeadingForRARCharge: String = messages("chargeSummary.chargeHistory.heading")
  val lpiHistoryHeading: String = messages("chargeSummary.chargeHistory.lateInterestPayment")
  val lateInterestSuccessHeading = s"${messages("chargeSummary.lpi.paymentOnAccount1.text")}"
  val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.payments-bullet2")}"
  val paymentprocessingbullet1Agent: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2-agent")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.payments-bullet2-agent")}"
  val warningText: String = "Warning " + messages("chargeSummary.reviewAndReconcilePoa.warning")
  val explanationTextForRAR1: String = messages("chargeSummary.reviewAndReconcilePoa.p1") + " " + messages("chargeSummary.reviewAndReconcilePoa1.linkText") + " " + messages("chargeSummary.reviewAndReconcilePoa.p2")
  val explanationTextForRAR2: String = messages("chargeSummary.reviewAndReconcilePoa.p1") + " " + messages("chargeSummary.reviewAndReconcilePoa2.linkText") + " " + messages("chargeSummary.reviewAndReconcilePoa.p2")
  val descriptionTextForRAR1: String = messages("chargeSummary.chargeHistory.created.reviewAndReconcilePoa1.text")
  val descriptionTextForRAR2: String = messages("chargeSummary.chargeHistory.created.reviewAndReconcilePoa2.text")
  val descriptionTextRAR1Interest: String = messages("chargeSummary.poa1ExtraAmountInterest.p1")

  class Setup(financialDetails: FinancialDetailsResponseModel,
              chargeHistoryHasError: Boolean = false
              ) {
    mockGetAllFinancialDetails(List((2018, financialDetails)))

    if(chargeHistoryHasError) {
      setupMockChargeHistoryFailureResp(ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure"))
    } else {
      setupMockChargeHistorySuccessResp(List())
    }

    setupMockGetAdjustmentHistory(emptyAdjustmentHistoryModel)
    mockGetReviewAndReconcileCredit(PoaOneReconciliationCredit)
    mockBothIncomeSources()

  }
}


