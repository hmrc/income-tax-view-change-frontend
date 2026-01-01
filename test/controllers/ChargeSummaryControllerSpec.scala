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

package controllers

import enums.{AdjustmentReversalReason, AmendedReturnReversalReason, MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import models.admin.{ChargeHistory, PenaltiesAndAppeals}
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel}
import models.financialDetails.PoaTwoReconciliationCredit
import models.repaymentHistory.RepaymentHistoryUtils
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{ChargeHistoryService, FinancialDetailsService, PaymentAllocationsService}
import testConstants.BaseTestConstants.testTaxYear
import testConstants.FinancialDetailsTestConstants._

import java.time.LocalDate
import scala.concurrent.Future

class ChargeSummaryControllerSpec extends ChargeSummaryControllerHelper {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ChargeHistoryService].toInstance(mockChargeHistoryService),
      api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService),
      api.inject.bind[PaymentAllocationsService].toInstance(mockPaymentAllocationsService)
    ).build()

  lazy val testController = app.injector.instanceOf[ChargeSummaryController]

  val endYear: Int = 2018
  val startYear: Int = endYear - 1

  def codedOutAdjustmentHistory: AdjustmentHistoryModel =
    AdjustmentHistoryModel(AdjustmentModel(2500.00, Some(LocalDate.of(2018,3,29)), AdjustmentReversalReason), List(AdjustmentModel(2000.00, Some(LocalDate.of(2019,3,30)), AmendedReturnReversalReason)))

  def setupMtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  setupMtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual

    def action(id: String, isInterestCharge: Boolean = false) = if (isAgent) {
      testController.showAgent(testTaxYear, id, isInterestCharge)
    } else {
      testController.show(testTaxYear, id, isInterestCharge)
    }

    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"is an authenticated $mtdUserRole " should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action(id1040000123))(fakeRequest)
        } else {
          "render the charge summary page" when {
            "charge history feature switch is enabled and there is a user" that {
              "provided with an id associated to a POA1 Debit" in new Setup(financialDetailsModelWithPoaOneAndTwo(), docId = id1040000125) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000125)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £1,400.00 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              }
              "provided with an id associated to a POA2 Debit" in new Setup(financialDetailsModelWithPoaOneAndTwo(), docId = id1040000126) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000126)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £1,400.00 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              }
              "provided with an id associated to a POA1 Debit that has been paid in full" in new Setup(financialDetailsModelWithPoaOneAndTwoFullyPaid(), docId = id1040000125) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("1040000125")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £0.00"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              }
              "provided with an id associated to a POA2 Debit that has been paid in full" in new Setup(financialDetailsModelWithPoaOneAndTwoFullyPaid(), docId = id1040000126) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("1040000126")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £0.00"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              }
              "provided with an id associated to a POA1 Debit that has been coded out" in new Setup(testFinancialDetailsModelWithPayeSACodingOutPOA1(), adjustmentHistoryModel = codedOutAdjustmentHistory, docId = codingout){
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account collected through PAYE tax code"
                document.getElementById("charge-amount-heading").text() shouldBe "Amount due to be collected: £12.34"
                document.getElementById("codedOutPOAExplanation").text() shouldBe "This is the tax you owe for the 2020 to 2021 tax year. It will be collected in the 2022 to 2023 tax year through your PAYE tax code."
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2020 to 2021 tax bill."
              }
              "provided with an id associated to a POA2 Debit that has been coded out" in new Setup(testFinancialDetailsModelWithPayeSACodingOutPOA2(), adjustmentHistoryModel = codedOutAdjustmentHistory, docId = codingout){
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account collected through PAYE tax code"
                document.getElementById("charge-amount-heading").text() shouldBe "Amount due to be collected: £12.34"
                document.getElementById("codedOutPOAExplanation").text() shouldBe "This is the tax you owe for the 2020 to 2021 tax year. It will be collected in the 2022 to 2023 tax year through your PAYE tax code."
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2020 to 2021 tax bill."
              }
              "provided with an id associated to a POA1 Debit with accruing interest" in new Setup(financialDetailsModelWithPoaOneWithLpi(), docId = codingout) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £1,400.00 (not including estimated interest)"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on this charge"
                document.getElementById("interestOnCharge.p2").text() shouldBe "Interest will be estimated until the charge it is related to is paid in full."
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
                document.getElementById("guidance.p1").text() shouldBe "The interest on a charge you owe can go up and down. See guidance on the interest rate set by HMRC (opens in new tab)."
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe "Warning Pay this charge to stop this interest from increasing daily."

              }
              "provided with an id associated to a POA2 Debit with accruing interest" in new Setup(financialDetailsModelWithPoaTwoWithLpi(), docId = codingout) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account"
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £1,400.00 (not including estimated interest)"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on this charge"
                document.getElementById("interestOnCharge.p2").text() shouldBe "Interest will be estimated until the charge it is related to is paid in full."
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
                document.getElementById("guidance.p1").text() shouldBe "The interest on a charge you owe can go up and down. See guidance on the interest rate set by HMRC (opens in new tab)."
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe "Warning Pay this charge to stop this interest from increasing daily."

              }
              "provided with an id associated to a Balancing payment" in new Setup(testValidFinancialDetailsModelWithBalancingCharge, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Balancing payment"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £10.33 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is a balancing payment?"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2018 to 2019 tax bill."

              }
              "provided with an id associated to a Balancing payment with accruing interest" in new Setup(testValidFinancialDetailsModelWithBalancingChargeWithAccruingInterest, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-heading-xl").first().text() should include("Balancing payment")
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £100.00 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is a balancing payment?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on this charge"
                document.getElementById("interestOnCharge.p2").text() shouldBe "Interest will be estimated until the charge it is related to is paid in full."
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2018 to 2019 tax bill."
                document.getElementById("guidance.p1").text() shouldBe "The interest on a charge you owe can go up and down. See guidance on the interest rate set by HMRC (opens in new tab)."
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe "Warning Pay this charge to stop this interest from increasing daily."

              }

              "provided with an id associated to a charge for Class 2 National Insurance" in new Setup(testFinancialDetailsModelWithCodingOutNics2(), docId = codingout) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-heading-xl").first().text() should include("Class 2 National Insurance")
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2020 to 2021 tax year")
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £12.34"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 25 August 2021"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"

              }

              "provided with an id associated to a coded out Balancing Payment" in new Setup(testFinancialDetailsModelWithPayeSACodingOut(), adjustmentHistoryModel = codedOutAdjustmentHistory, docId = codingout){
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-heading-xl").first().text() should include("Balancing payment collected through PAYE tax code")
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2020 to 2021 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Amount due to be collected: £12.34"
                Option(document.getElementById("due-date-text")) shouldBe None
                document.getElementById("codedOutBCDExplanation").text() shouldBe "This is the remaining tax you owe for the 2020 to 2021 tax year. It will be collected in the 2022 to 2023 tax year through your PAYE tax code."
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2020 to 2021 tax bill."
                document.getElementById("payment-history-table").select("tr").get(1).text() shouldBe s"29 Mar 2018 Amount to be collected through your PAYE tax code in 2022 to 2023 tax year. £2,500.00"
                document.getElementById("payment-history-table").select("tr").get(3).text() shouldBe s"30 Mar 2019 Amount adjusted to be collected through your PAYE tax code in 2022 to 2023 tax year £2,000.00"
              }

              "provided with an id associated to an ITSA Return Amendment charge" in new Setup(testValidFinancialDetailsModelWithITSAReturnAmendment, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Balancing payment: extra amount due to amended return"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "You owe: £10.33 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementById("itsa-return-amendment-p1").text() shouldBe "You owe this extra tax because of a change you made to your return."
                document.getElementById("interestOnCharge.p1").text() shouldBe "Interest is charged from the date your balancing payment was originally due."
                document.getElementById("interestOnCharge.p2").text() shouldBe "Interest will be estimated until the charge it is related to is paid in full."
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is a balancing payment?"
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe "Warning Pay this charge to stop this interest from increasing daily."
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }

              "provided with an id associated to outstanding interest on a paid ITSA Return Amendment charge" in new Setup(testValidFinancialDetailsModelWithITSAReturnAmendment, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123, isInterestCharge = true)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Late payment interest on balancing payment: extra amount due to amended return"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "You owe: £100.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementById("lpi-itsa1").text() shouldBe "You owe HMRC interest because you paid your balancing payment late."
                document.getElementById("lpi-itsa2").text() shouldBe "Late payment interest is charged from the first day your payment is overdue until the day it’s paid in full. It’s calculated at the Bank of England base rate (opens in new tab) plus 2.5%."
                document.getElementById("lpi-itsa3").text() shouldBe "See guidance on the interest rates set by HMRC (opens in new tab)."
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }

              "provided with an id associated to an ITSA Return Amendment credit" in new Setup(
                financialDetailsModelWithPoaOneAndTwoWithRarAndAmendmentCredits(), paymentAllocations = Right(paymentAllocationResponse), docId = id1040000127) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000127)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.select("h1").first().text() shouldBe "Credit from your amended tax return"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2017 to 2018 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Total credit amount: £100.00"
                document.getElementById("credit-created-text").text() shouldBe "This credit was created on 29 March 2018"
                document.getElementById("itsa-return-amendment-credit-p1").text() shouldBe "HMRC has added a credit to your account because your amended tax return shows that your 2017 to 2018 tax bill was too high."
                document.getElementById("itsa-return-amendment-credit-p2").text() shouldBe "This credit may be used automatically by HMRC to cover your future tax bills when they become due."
                document.getElementById("allocation-section").text() shouldBe "Where the credit was applied"
              }

              "provided with an id associated to a Late Submission Penalty" in new Setup(testValidFinancialDetailsModelWithLateSubmissionPenalty, docId = id1040000123) {
                enable(ChargeHistory, PenaltiesAndAppeals)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Late submission penalty"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "You owe: £10.33 (not including estimated interest)"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementById("LSP-content-1").text() shouldBe "You will get a late submission penalty point every time you send a submission after the deadline. A submission can be a quarterly update or annual tax return."
                document.getElementById("LSP-content-2").text() shouldBe "If you reach 4 points, you’ll have to pay a £200 penalty."
                document.getElementById("LSP-content-3").text() shouldBe "To avoid receiving late submission penalty points in the future, and the potential for a financial penalty, you need to send your submissions on time."
                document.getElementById("interestOnCharge.p2").text() shouldBe "Interest will be estimated until the charge it is related to is paid in full."
                document.getElementById("LSP-content-4").text() shouldBe "You can view the details about your penalty and find out how to appeal."
                document.getElementsByClass("govuk-heading-m").get(1).text() shouldBe "Interest on this charge"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
                document.getElementById("guidance.p1").text() shouldBe "The interest on a charge you owe can go up and down. See guidance on the interest rate set by HMRC (opens in new tab)."
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe "Warning Pay this charge to stop this interest from increasing daily."

              }

              "provided with an id associated to a Late payment penalty" in new Setup(testValidFinancialDetailsModelWithLatePaymentPenalty, docId = id1040000123){
                enable(ChargeHistory, PenaltiesAndAppeals)

                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First late payment penalty"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2020 to 2021 tax year")
                document.getElementById("charge-amount-heading").text() shouldBe "You owe: £200.33 (not including estimated interest)"
                document.getElementById("due-date-text").text() shouldBe "Due 29 March 2020"
                document.getElementById("first-payment-penalty-p1").text() shouldBe "You have received this penalty because you are late paying your Income Tax."
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }

              "provided with an id associated to a Review & Reconcile Debit Charge for POA1" in new Setup(
                testFinancialDetailsModelWithReviewAndReconcileAndPoas, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)
                val chargeSummaryUrl = if(isAgent) {
                  routes.ChargeSummaryController.showAgent(testTaxYear, id1040000123).url
                } else {
                  routes.ChargeSummaryController.show(testTaxYear, id1040000123).url
                }

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption(startYear.toString, endYear.toString)
                document.select("h1").text() shouldBe successHeadingForRAR1
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
                document.getElementById("rar-poa1-explanation-p1").text() + document.getElementById("rar-poa-explanation-p2").text() shouldBe explanationTextForRAR1
                document.getElementById("charge-history-heading").text() shouldBe paymentHistoryHeadingForRARCharge
                document.getElementById("poa1-link").attr("href") shouldBe chargeSummaryUrl

                document
                  .getElementById("payment-history-table")
                  .getElementsByClass("govuk-table__body")
                  .first()
                  .getElementsByClass("govuk-table__cell")
                  .get(1)
                  .text() shouldBe descriptionTextForRAR1
              }

              "provided with an id associated to a Review & Reconcile Debit Charge for POA2" in new Setup(testFinancialDetailsModelWithReviewAndReconcileAndPoas, docId = id1040000124) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()
                val result: Future[Result] = action(id1040000124)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                val chargeSummaryUrl = if(isAgent) {
                  routes.ChargeSummaryController.showAgent(testTaxYear, id1040000124).url
                } else {
                  routes.ChargeSummaryController.show(testTaxYear, id1040000124).url
                }

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption(startYear.toString, endYear.toString)
                document.select("h1").text() shouldBe successHeadingForRAR2
                document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
                document.getElementById("rar-poa2-explanation-p1").text() + document.getElementById("rar-poa-explanation-p2").text() shouldBe explanationTextForRAR2
                document.getElementById("charge-history-heading").text() shouldBe paymentHistoryHeadingForRARCharge
                document.getElementById("poa2-link").attr("href") shouldBe chargeSummaryUrl
                document
                  .getElementById("payment-history-table")
                  .getElementsByClass("govuk-table__body")
                  .first()
                  .getElementsByClass("govuk-table__cell")
                  .get(1)
                  .text() shouldBe descriptionTextForRAR2
              }

              "provided with an id associated to interest on a Review & Reconcile Debit Charge for POA" in new Setup(testFinancialDetailsModelWithReviewAndReconcileInterest, docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val endYear: Int = 2018
                val startYear: Int = endYear - 1

                val result: Future[Result] = action(id1040000123, isInterestCharge = true)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption(startYear.toString, endYear.toString)
                document.select("h1").text() shouldBe successHeadingRAR1Interest
                document.getElementById("poa1-extra-charge-p1").text() shouldBe descriptionTextRAR1Interest
              }

              "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel(accruingInterestAmount = Some(0.0)), docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()
                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                document.select("h1").text() shouldBe successHeadingForPOA1
                document.select("#dunningLocksBanner").size() shouldBe 1
                document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe s"$dunningLocksBannerHeading"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }


              "the late payment interest flag is enabled" in new Setup(
                financialDetailsModel(lpiWithDunningLock = None, outstandingAmount = 0), docId = id1040000123) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123, isInterestCharge = true)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                document.select("h1").text() shouldBe lateInterestSuccessHeading
                document.select("#dunningLocksBanner").size() shouldBe 0
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }
            }
            "charge history feature is disabled and there is a user" that {
              "provided with dunning locks and late payment interest flag, not showing the locks banner" in new Setup(
                financialDetailsModel(lpiWithDunningLock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear)), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()
                val result: Future[Result] = action(id1040000123, isInterestCharge = true)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.select("#dunningLocksBanner").size() shouldBe 0
                document.select("#heading-payment-breakdown").size() shouldBe 0
              }

              "provided with dunning locks, showing the locks banner" in new Setup(
                financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear)), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
              }

              "allocations present" in new Setup(
                chargesWithAllocatedPaymentModel(), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val doc = JsoupParse(result).toHtmlDocument
                doc.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                doc.select("h1").text() shouldBe successHeadingForPOA1
                doc.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe s"$dunningLocksBannerHeading"
                doc.getElementById("charge-history-heading").text() shouldBe "History of this charge"

                val allocationsUrl = if(isAgent) {
                  routes.PaymentAllocationsController.viewPaymentAllocationAgent(id1040000124).url
                } else {
                  routes.PaymentAllocationsController.viewPaymentAllocation(id1040000124).url
                }
                val allocationLink = doc.select("#payment-history-table").select("tr").get(1).select("a").attr("href")
                allocationLink shouldBe allocationsUrl
              }

              "without allocations" in new Setup(
                chargesWithAllocatedPaymentModel(), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption(startYear.toString, endYear.toString)
                document.select("h1").text() shouldBe successHeadingForPOA1
                document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe s"$dunningLocksBannerHeading"
                document.getElementById("charge-history-heading").text() shouldBe "History of this charge"
              }

              "displays link to poa extra charge on poa page when reconciliation charge exists" in new Setup(financialDetailsModelWithPoaExtraCharge(), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val chargeSummaryUrl = if(isAgent) {
                  routes.ChargeSummaryController.showAgent(testTaxYear, "123456").url
                } else {
                  routes.ChargeSummaryController.show(testTaxYear, "123456").url
                }
                JsoupParse(result).toHtmlDocument.select("#poa-extra-charge-link").attr("href") shouldBe chargeSummaryUrl
              }
              "not display link to poa extra charge if no charge exists" in new Setup(financialDetailsModel(), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                JsoupParse(result).toHtmlDocument.select("#poa-extra-charge-link").attr("href") shouldBe ""
              }

              "display the payment processing info if the charge is not Review & Reconcile" in new Setup(
                financialDetailsModel(mainTransaction = "4910"), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeadingForBCD
              }

              "hide payment processing info" in new Setup(financialDetailsReviewAndReconcile, docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2022", "2023")
                document.select("h1").text() shouldBe successHeadingForRAR1
                document.select("#payment-processing-bullets").isEmpty shouldBe true
              }

              "display the Review & Reconcile credit for POA1 when present in the user's financial details" in new Setup(
                financialDetailsModelWithPoaOneAndTwoWithRarAndAmendmentCredits(), docId = id1040000125) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000125)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
                document.getElementById("rar-charge-link").attr("href") shouldBe
                  RepaymentHistoryUtils.getChargeLinkUrl(isAgent = isAgent, testTaxYear, "transactionId")

              }

              "display the Review & Reconcile credit for POA2 when present in the user's financial details (old view)" in new Setup(
                financialDetailsModelWithPoaOneAndTwoWithRarAndAmendmentCredits(), docId = id1040000126) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000126)(fakeRequest)

                mockGetReviewAndReconcileCredit(PoaTwoReconciliationCredit)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
                document.getElementById("rar-charge-link").attr("href") shouldBe
                  RepaymentHistoryUtils.getChargeLinkUrl(isAgent = isAgent, testTaxYear, "transactionId")
              }

              "the charge is an MFA Debit" in new Setup(
                financialDetailsModelWithMFADebit(), docId = id1040000123) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
              }
            }
          }
          "Redirect the user to NotFoundDocumentIDLookup" when {
            "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel(), docId = "fakeId") {
              disable(ChargeHistory)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action("fakeId")(fakeRequest)

              val notFoundDocumentIDUrl = if(isAgent) {
                controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url
              } else {
                controllers.errors.routes.NotFoundDocumentIDLookupController.show().url
              }
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(notFoundDocumentIDUrl)
            }
          }

          "render the error page" when {
            "the charge history response is an error" in new Setup(
              financialDetailsModel(), chargeHistoryHasError = true, docId = id1040000123) {
              enable(ChargeHistory)
              disable(ChargeHistory)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000123)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing, docId = id1040000123) {
              disable(ChargeHistory)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000123)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            "the financial details response does not contain a chargeReference" in new Setup(financialDetailsModelWithPoaOneNoChargeRef(), docId = id1040000125) {
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000125)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            "the charge type is forbidden by current feature switches" in new Setup(testValidFinancialDetailsModelWithLateSubmissionPenalty, docId = id1040000123) {
              disable(PenaltiesAndAppeals)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000123)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            if (mtdUserRole == MTDIndividual) {
              "no related tax year financial details found" in new Setup(testFinancialDetailsModelWithPayeSACodingOut(), docId = codingout) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = testController.show(2020, "CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
                JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
              }
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action(id1040000123), mtdUserRole, false)(fakeRequest)
    }
  }
}
