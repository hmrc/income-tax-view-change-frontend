/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.views

import common.config.featureswitch.FeatureSwitching
import common.testUtils.ViewSpec
import financials.models.*
import financials.models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel}
import financials.models.chargeSummary.{ChargeSummaryViewModel, PaymentHistoryAllocations}
import financials.testConstants.ChargeConstants
import financials.views.html.YourSelfAssessmentChargeSummaryView
import financials.controllers.routes as financialsRoutes
import financials.enums.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html

import java.time.LocalDate

class YourSelfAssessmentChargeSummaryViewSpec extends ViewSpec with ChargeConstants with FeatureSwitching {

  val yourSelfAssessmentChargeSummaryView: YourSelfAssessmentChargeSummaryView = app.injector.instanceOf[YourSelfAssessmentChargeSummaryView]

  val itsaEnquiryAmendmentHeading: String = "Extra amount to pay due to HMRC enquiry amendment"
  val itsaEnquiryAmendmentParagraph1: (Int, Int) => String = (taxYear1, taxYear2) => s"Following a compliance check, HMRC made a change to your tax " +
    s"return known as an ‘enquiry amendment’. This changed your tax calculation for ${taxYear1.toString} to " +
    s"${taxYear2.toString}, resulting in an extra amount to pay towards your tax bill."
  val warningText: String = "Warning Pay this charge to stop this interest from increasing daily."
  val enquiryAmendmentDescriptionText: String = "Extra amount created when HMRC amended your tax return"
  val itsaEnquiryAmendmentCreditHeading: String = "Credit from HMRC enquiry amendment"

  def subItemWithClearingSapDocument(clearingSAPDocument: String): SubItem = SubItem(dueDate = Some(LocalDate.parse("2017-08-07")), clearingSAPDocument = Some(clearingSAPDocument), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))

  val defaultAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1400, Some(LocalDate.of(2018, 3, 29)), AdjustmentReversalReason), List())

  val payments: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(9999, "PAYID01", Some("Payment on Account"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, None, Some("lotItem"), Some("lot")),
      DocumentDetail(2025, "123456789", Some("Reconciliation Credit"), Some("documentText"), 1200, 5000, LocalDate.of(2025, 2, 15), None, None, None, None, None, None, None, None, None, None, None, None, None, None)),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYID01"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))),
      FinancialDetail("2025", Some("Reconciliation Credit"), Some("4905"), Some("123456789"), None, Some("1234"), None, Some(3800.00), Some(5000.00), None, Some(3800.00), Some("NIC4-GB"), None, None))
  )

  class TestSetup(chargeItem: ChargeItem = chargeItemModel(),
                  dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                  paymentBreakdown: List[FinancialDetail] = List(),
                  paymentAllocations: List[PaymentHistoryAllocations] = List(),
                  reviewAndReconcileCredit: Option[ChargeItem] = None,
                  payments: FinancialDetailsModel = payments,
                  chargeHistoryEnabled: Boolean = true,
                  latePaymentInterestCharge: Boolean = false,
                  isAgent: Boolean = false,
                  adjustmentHistory: AdjustmentHistoryModel = defaultAdjustmentHistory,
                  poaExtraChargeLink: Option[String] = None,
                  whatYouOweUrl: String = financialsRoutes.WhatYouOweController.show().url,
                  taxYearSummaryUrl: Int => String = (taxYear: Int) => appConfig.taxYearSummaryUrl(false, 2018, returnsEnabled = true)) {

    val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
      currentDate = dateService.getCurrentDate,
      chargeItem = chargeItem.copy(dueDate = dueDate),
      backUrl = "testBackURL",
      gatewayPage = None,
      paymentBreakdown = paymentBreakdown,
      paymentAllocations = paymentAllocations,
      reviewAndReconcileCredit = reviewAndReconcileCredit,
      payments = payments,
      chargeHistoryEnabled = chargeHistoryEnabled,
      latePaymentInterestCharge = latePaymentInterestCharge,
      penaltiesEnabled = true,
      isAgent = isAgent,
      poaOneChargeUrl = "testUrl1",
      poaTwoChargeUrl = "testUrl2",
      adjustmentHistory = adjustmentHistory,
      poaExtraChargeLink = poaExtraChargeLink,
      LSPUrl = "testLSPUrl",
      LPPUrl = "testLPPUrl",
      taxYearSummaryUrl = taxYearSummaryUrl
    )

    val view: Html = yourSelfAssessmentChargeSummaryView(viewModel, None, whatYouOweUrl)
    val document: Document = Jsoup.parse(view.toString())
  }

  "YourSelfAssessmentChargeSummaryView" when {
    "charge is an ITSAReturnAmendment type and has a charge classification of 'RA'" should {
      "display the correct content" in new TestSetup(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendment, chargeClassification = Some("RA"))) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe itsaEnquiryAmendmentHeading
        document.getElementById("itsaEnquiryAmendment.p1").text() shouldBe itsaEnquiryAmendmentParagraph1(2017, 2018)
        document.getElementById("itsaEnquiryAmendment.link").attr("href") shouldBe viewModel.taxYearSummaryUrl(2018)
        document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
        document.getElementById("charge-history-caption").text() shouldBe "This extra amount goes towards your 2017 to 2018 tax bill."
        document.select("#payment-history-table > tbody > tr > td:nth-child(2)").text() shouldBe enquiryAmendmentDescriptionText
      }
    }
    "charge is an ITSAReturnAmendmentCredit type and has a charge classification of 'RA'" should {
      "display the correct content" in new TestSetup(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendmentCredit, chargeClassification = Some("RA"))){
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe itsaEnquiryAmendmentCreditHeading
        Option(document.getElementById("itsa-enquiry-amendment-credit-p1")).isDefined shouldBe true
        Option(document.getElementById("itsa-enquiry-amendment-credit-p2")).isDefined shouldBe true
      }
    }
  }
}
