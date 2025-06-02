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

import enums.{MTDIndividual, MTDSupportingAgent}
import models.admin.{ChargeHistory, ReviewAndReconcilePoa, YourSelfAssessmentCharges}
import models.financialDetails.PoaTwoReconciliationCredit
import models.repaymentHistory.RepaymentHistoryUtils
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{ChargeHistoryService, FinancialDetailsService}
import testConstants.BaseTestConstants.testTaxYear
import testConstants.FinancialDetailsTestConstants._

import scala.concurrent.Future

class ChargeSummaryControllerSpec extends ChargeSummaryControllerHelper {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ChargeHistoryService].toInstance(mockChargeHistoryService),
      api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService)
    ).build()

  lazy val testController = app.injector.instanceOf[ChargeSummaryController]

  val endYear: Int = 2018
  val startYear: Int = endYear - 1

  mtdAllRoles.foreach { mtdUserRole =>
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
            "charge history & your self assessment charges feature switch is enabled and there is a user" that {
              "provided with an id associated to a POA1 Debit" in new Setup(financialDetailsModelWithPoaOneAndTwo()) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000125)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account"
                document.select("h1").eq(1).text() shouldBe "Overdue charge: £1,400.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "First payment on account history"
              }
              "provided with an id associated to a POA2 Debit" in new Setup(financialDetailsModelWithPoaOneAndTwo()) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000126)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account"
                document.select("h1").eq(1).text() shouldBe "Overdue charge: £1,400.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "Second payment on account history"
              }
              "provided with an id associated to a POA1 Debit with accruing interest" in new Setup(financialDetailsModelWithPoaOneAndTwoWithLpi()) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000125)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First payment on account"
                document.select("h1").eq(1).text() shouldBe "Overdue charge: £1,400.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on your first payment on account"
                document.getElementById("interestOnCharge.p1").text() shouldBe "The amount of interest you have to pay will increase every day until you pay the overdue charge."
                document.getElementById("howIsInterestCalculated.linkText").text().contains("How is interest calculated?")
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "First payment on account history"
              }
              "provided with an id associated to a POA2 Debit with accruing interest" in new Setup(financialDetailsModelWithPoaOneAndTwoWithLpi()) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000126)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Second payment on account"
                document.select("h1").eq(1).text() shouldBe "Overdue charge: £1,400.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 1 January 2020"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is payment on account?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on your second payment on account"
                document.getElementById("interestOnCharge.p1").text() shouldBe "The amount of interest you have to pay will increase every day until you pay the overdue charge."
                document.getElementById("howIsInterestCalculated.linkText").text().contains("How is interest calculated?")
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "Second payment on account history"
              }
              "provided with an id associated to a Balancing payment" in new Setup(testValidFinancialDetailsModelWithBalancingCharge) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Balancing payment"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Overdue charge: £10.33"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is a balancing payment?"
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "Balancing payment history"
              }
              "provided with an id associated to a Balancing payment with accruing interest" in new Setup(testValidFinancialDetailsModelWithBalancingChargeWithAccruingInterest) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-heading-xl").first().text() should include("Balancing payment")
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Overdue charge: £100.00"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementsByClass("govuk-details__summary-text").first().text() shouldBe "What is a balancing payment?"
                document.getElementById("interest-on-your-charge-heading").text() shouldBe "Interest on your balancing payment"
                document.getElementById("interestOnCharge.p1").text() shouldBe "The amount of interest you have to pay will increase every day until you pay the overdue charge."
                document.getElementById("howIsInterestCalculated.linkText").text().contains("How is interest calculated?")
                document.getElementById("interest-on-your-charge-table").getAllElements.size().equals(0) shouldBe false
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "Balancing payment history"
              }

              "provided with an id associated to a charge for Class 2 National Insurance" in new Setup(testFinancialDetailsModelWithCodingOutNics2()) {
                enable(YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action("CODINGOUT01")(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-heading-xl").first().text() should include("Class 2 National Insurance")
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2020 to 2021 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Overdue charge: £12.34"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 25 August 2021"
                document.getElementsByClass("govuk-table__caption govuk-table__caption--m").text() shouldBe "History of this charge"
              }

              "provided with an id associated to a Late Submission Penalty" in new Setup(testValidFinancialDetailsModelWithLateSubmissionPenalty) {
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "Late submission penalty"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2018 to 2019 tax year")
                document.getElementsByClass("govuk-heading-m").first().text() shouldBe "Overdue charge: £10.33"
                document.getElementById("due-date-text").select("p").text() shouldBe "Due 29 March 2018"
                document.getElementById("LSP-content-1").text() shouldBe "You will get a late submission penalty point every time you send a submission after the deadline. A submission can be a quarterly update or annual tax return."
                document.getElementById("LSP-content-2").text() shouldBe "If you reach 4 points, you’ll have to pay a £200 penalty."
                document.getElementById("LSP-content-3").text() shouldBe "To avoid receiving late submission penalty points in the future, and the potential for a financial penalty, you need to send your submissions on time."
                document.getElementById("LSP-content-4").text() shouldBe "You can view the details about your penalty and find out how to appeal."
                document.getElementsByClass("govuk-heading-l").first().text() shouldBe "Interest on your late submission penalty"
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "Late submission penalty history"
              }

              "provided with an id associated to a Late payment penalty" in new Setup(testValidFinancialDetailsModelWithLatePaymentPenalty){
                enable(ReviewAndReconcilePoa, YourSelfAssessmentCharges, ChargeHistory)

                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.select("h1").first().text() shouldBe "First late payment penalty"
                document.getElementsByClass("govuk-caption-xl").first().text() should include("2020 to 2021 tax year")
                document.getElementById("charge-amount-heading").text() shouldBe "Overdue charge: £200.33"
                document.getElementById("due-date-text").text() shouldBe "Due 29 March 2020"
                document.getElementById("first-payment-penalty-p1").text() shouldBe "You have received this penalty because you are late paying your Income Tax."
                document.getElementById("payment-history-table").getElementsByTag("caption").text() shouldBe "First late payment penalty history"
              }
            }
            "charge history feature is enabled and there is a user" that {
              "provided with an id associated to a Review & Reconcile Debit Charge for POA1" in new Setup(
                testFinancialDetailsModelWithReviewAndReconcileAndPoas) {
                enable(ReviewAndReconcilePoa, ChargeHistory)
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
                document.getElementById("rar-poa1-explanation").text() shouldBe explanationTextForRAR1
                document.getElementById("charge-history-h3").text() shouldBe paymentHistoryHeadingForRARCharge
                document.getElementById("poa1-link").attr("href") shouldBe chargeSummaryUrl

                document
                  .getElementById("payment-history-table")
                  .getElementsByClass("govuk-table__body")
                  .first()
                  .getElementsByClass("govuk-table__cell")
                  .get(1)
                  .text() shouldBe descriptionTextForRAR1
              }

              "provided with an id associated to a Review & Reconcile Debit Charge for POA2" in new Setup(testFinancialDetailsModelWithReviewAndReconcileAndPoas) {
                enable(ReviewAndReconcilePoa, ChargeHistory)
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
                document.getElementById("rar-poa2-explanation").text() shouldBe explanationTextForRAR2
                document.getElementById("charge-history-h3").text() shouldBe paymentHistoryHeadingForRARCharge
                document.getElementById("poa2-link").attr("href") shouldBe chargeSummaryUrl
                document
                  .getElementById("payment-history-table")
                  .getElementsByClass("govuk-table__body")
                  .first()
                  .getElementsByClass("govuk-table__cell")
                  .get(1)
                  .text() shouldBe descriptionTextForRAR2
              }

              "provided with an id associated to interest on a Review & Reconcile Debit Charge for POA" in new Setup(testFinancialDetailsModelWithReviewAndReconcileInterest) {
                enable(ReviewAndReconcilePoa, ChargeHistory)
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

              "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel()) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()
                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                document.select("h1").text() shouldBe successHeadingForPOA1
                document.select("#dunningLocksBanner").size() shouldBe 1
                document.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
                document.select("main h3").text() shouldBe paymentHistoryHeadingForPOA1Charge
              }


              "the late payment interest flag is enabled" in new Setup(
                financialDetailsModel(lpiWithDunningLock = None)) {
                enable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123, isInterestCharge = true)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                document.select("h1").text() shouldBe lateInterestSuccessHeading
                document.select("#dunningLocksBanner").size() shouldBe 0
                document.select("main h2").text() shouldBe lpiHistoryHeading
              }
            }
            "charge history feature is disabled and there is a user" that {
              "provided with dunning locks and late payment interest flag, not showing the locks banner" in new Setup(
                financialDetailsModel(lpiWithDunningLock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {
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
                financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
                document.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
              }

              "allocations present" in new Setup(
                chargesWithAllocatedPaymentModel()) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val doc = JsoupParse(result).toHtmlDocument
                doc.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption("2017", "2018")
                doc.select("h1").text() shouldBe successHeadingForPOA1
                doc.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
                doc.select("main h3").text() shouldBe paymentHistoryHeadingForPOA1Charge

                val allocationsUrl = if(isAgent) {
                  routes.PaymentAllocationsController.viewPaymentAllocationAgent(id1040000124).url
                } else {
                  routes.PaymentAllocationsController.viewPaymentAllocation(id1040000124).url
                }
                val allocationLink = doc.select("#payment-history-table").select("tr").get(1).select("a").attr("href")
                allocationLink shouldBe allocationsUrl
              }

              "without allocations" in new Setup(
                chargesWithAllocatedPaymentModel()) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementsByClass("govuk-caption-xl").text() shouldBe successCaption(startYear.toString, endYear.toString)
                document.select("h1").text() shouldBe successHeadingForPOA1
                document.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
                document.select("main h3").text() shouldBe paymentHistoryHeadingForPOA1Charge
              }

              "displays link to poa extra charge on poa page when reconciliation charge exists" in new Setup(financialDetailsModelWithPoaExtraCharge()) {
                enable(ReviewAndReconcilePoa)
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
              "not display link to poa extra charge if no charge exists" in new Setup(financialDetailsModel()) {
                enable(ReviewAndReconcilePoa)
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                JsoupParse(result).toHtmlDocument.select("#poa-extra-charge-link").attr("href") shouldBe ""
              }

              "display the payment processing info if the charge is not Review & Reconcile" in new Setup(
                financialDetailsModel(documentDescription = Some("ITSA BCD"), mainTransaction = "4910")) {
                disable(ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000123)(fakeRequest)

                status(result) shouldBe Status.OK
                JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeadingForBCD
              }

              "hide payment processing info" in new Setup(financialDetailsReviewAndReconcile) {
                disable(ChargeHistory)
                enable(ReviewAndReconcilePoa)
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
                financialDetailsModelWithPoaOneAndTwoWithRarCredits()) {
                enable(ReviewAndReconcilePoa, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000125)(fakeRequest)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument

                document.getElementById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
                document.getElementById("rar-charge-link").attr("href") shouldBe
                  RepaymentHistoryUtils.getPoaChargeLinkUrl(isAgent = isAgent, testTaxYear, "transactionId")
                document.getElementById("rar-total-amount").text() shouldBe "£1,000.00"
                document.getElementById("rar-due-date").text() shouldBe "1 Jan 2018"
              }

              "display the Review & Reconcile credit for POA2 when present in the user's financial details" in new Setup(
                financialDetailsModelWithPoaOneAndTwoWithRarCredits()) {
                enable(ReviewAndReconcilePoa, ChargeHistory)
                setupMockSuccess(mtdUserRole)
                mockBothIncomeSources()

                val result: Future[Result] = action(id1040000126)(fakeRequest)

                mockGetReviewAndReconcileCredit(PoaTwoReconciliationCredit)

                status(result) shouldBe Status.OK
                val document = JsoupParse(result).toHtmlDocument
                document.getElementById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
                document.getElementById("rar-charge-link").attr("href") shouldBe
                  RepaymentHistoryUtils.getPoaChargeLinkUrl(isAgent = isAgent, testTaxYear, "transactionId")
                document.getElementById("rar-total-amount").text() shouldBe "£1,000.00"
                document.getElementById("rar-due-date").text() shouldBe "1 Jan 2018"
              }

              "the charge is an MFA Debit" in new Setup(
                financialDetailsModelWithMFADebit()) {
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
            "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel()) {
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
              financialDetailsModel(), chargeHistoryHasError = true) {
              enable(ChargeHistory)
              disable(ChargeHistory)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000123)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing) {
              disable(ChargeHistory)
              setupMockSuccess(mtdUserRole)
              mockBothIncomeSources()

              val result: Future[Result] = action(id1040000123)(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
            }

            if (mtdUserRole == MTDIndividual) {
              "no related tax year financial details found" in new Setup(testFinancialDetailsModelWithPayeSACodingOut()) {
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
