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

package controllers.agent

import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.FinancialDetailsTestConstants._
import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService}
import mocks.views.MockChargeSummary
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{FinancialDetail, FinancialDetailsModel}
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{never, verify}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.Html
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ChargeSummary

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockFinancialDetailsService
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with MockChargeSummary {

	def financialDetailsWithLocks(taxYear: Int): List[FinancialDetail] = List(
		financialDetail(taxYear = taxYear, chargeType = "ITSA England & NI"),
		financialDetail(taxYear = taxYear, chargeType = "NIC4 Wales", dunningLock = Some("Stand over order"))
	)

	val injectChargeSummaryView = app.injector.instanceOf[views.html.ChargeSummary]

  class Setup(chargeSummaryView: ChargeSummary = chargeSummary) {

    disable(ChargeHistory)
		disable(PaymentAllocation)

    mockChargeSummary()(Html("<html><head><title>Test title</title></head></html>"))

    val chargeSummaryController: ChargeSummaryController = new ChargeSummaryController(
			chargeSummaryView,
      mockAuthService,
      mockFinancialDetailsService,
      mockIncomeSourceDetailsService,
      mockAuditingService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      app.injector.instanceOf[ItvcErrorHandler]
    )

    val currentYear: Int = LocalDate.now().getYear
    val errorHeading = "Sorry, there is a problem with the service"
    val currentFinancialDetails: FinancialDetailsModel = chargesWithAllocatedPaymentModel(currentYear)
    val currentFinancialDetailsWithoutDunningLock: FinancialDetailsModel = chargesWithAllocatedPaymentModel(currentYear, lpiWithDunningBlock = None)
		val successHeading = s"Tax year 6 April ${currentYear - 1} to 5 April $currentYear Payment on account 1 of 2"
		val lateInterestSuccessHeading = s"Tax year 6 April ${currentYear - 1} to 5 April $currentYear Late payment interest on payment on account 1 of 2"
		val dunningLocksBannerHeading = "Important"
		val paymentBreakdownHeading = "Payment breakdown"
		val paymentHistoryHeading = "Payment history"
  }

  ".showChargeSummary" when {
		"getFinancialDetails returns a FinancialDetailsModel" when {
			"the model contains a transaction for the charge provided" should {
				"show a charge summary with the correct title" in new Setup(injectChargeSummaryView) {

					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))

					mockSingleBusinessIncomeSource()

					val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					status(result) shouldBe OK
					JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
					JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 1
					JsoupParse(result).toHtmlDocument.select("main h2").get(0).text() shouldBe dunningLocksBannerHeading
					JsoupParse(result).toHtmlDocument.select("main h2").get(1).text() shouldBe paymentBreakdownHeading
				}
			}

			"the model contains a transaction for the charge with LPI set to true" should {
				"show a charge summary with the correct title without dunning lock" in new Setup(injectChargeSummaryView) {

					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetailsWithoutDunningLock)))

					mockSingleBusinessIncomeSource()

					val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123, isLatePaymentCharge = true)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					status(result) shouldBe OK
					JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
					JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
				}
			}

			"the model contains a transaction for the charge with dunning locks" in new Setup(injectChargeSummaryView) {

				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

				mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails.copy(financialDetails = financialDetailsWithLocks(currentYear)))))

				mockSingleBusinessIncomeSource()

				val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
					.apply(fakeRequestConfirmedClient("AB123456C"))

				status(result) shouldBe OK
				JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
				JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
			}

			"the model does not contains a transaction for the charge provided" should {
				"redirect to home page" in new Setup() {

					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))

					mockSingleBusinessIncomeSource()

					val result = chargeSummaryController.showChargeSummary(currentYear, id1040000124)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					status(result) shouldBe SEE_OTHER
					redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/income-tax-account")

				}
			}
		}

		"getFinancialDetails does not returns a successful FinancialDetailsModel" when {

			"show an internal server error with the correct title" in new Setup() {

				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

				mockGetAllFinancialDetails(List((currentYear, testFinancialDetailsErrorModelParsing)))

				mockSingleBusinessIncomeSource()

				val result = chargeSummaryController.showChargeSummary(currentYear, "testid")
					.apply(fakeRequestConfirmedClient("AB123456C"))

				status(result) shouldBe INTERNAL_SERVER_ERROR
				JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading

			}

		}

		"the ChargeHistory feature switch is enabled and the page can be viewed" should {
			"pass to the view a retrieved charge history list in ascending order" in new Setup() {
				val chargeHistoryListInAscendingOrder: List[ChargeHistoryModel] = List(
					ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 54321, LocalDate.of(2017, 8, 12), "Customer Request"),
					ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 12345, LocalDate.of(2018, 7, 6), "amended return")
				)

				enable(ChargeHistory)
				enable(PaymentAllocation)
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
				mockSingleBusinessIncomeSource()

				mockGetChargeHistoryDetails(Future.successful(Some(chargeHistoryListInAscendingOrder.reverse)))

				val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
					.apply(fakeRequestConfirmedClient("AB123456C"))

				status(result) shouldBe OK
				verify(chargeSummary).apply(any(), any(), any(),ameq(chargeHistoryListInAscendingOrder), any(),any(), any(), any(), any(), any(), ameq(true))(any(), any(), any())
				verify(mockFinancialDetailsService).getChargeHistoryDetails(ameq("XAIT00000000015"), ameq(id1040000123))(any())
			}

			"pass to the view an empty charge history list" when {
				"charge history list does not exist" in new Setup() {
					enable(ChargeHistory)
					enable(PaymentAllocation)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
					mockSingleBusinessIncomeSource()

					mockGetChargeHistoryDetails(response = Future.successful(None))

					val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					status(result) shouldBe OK
					verify(chargeSummary).apply(any(), any(),any(), ameq(Nil), any(), any(), any(), any(), any(), any(), ameq(true))(any(), any(), any())
				}

				"viewing a Late Payment Interest summary" in new Setup() {
					enable(ChargeHistory)
					enable(PaymentAllocation)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
					mockSingleBusinessIncomeSource()

					val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123, isLatePaymentCharge = true)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					status(result) shouldBe OK
					verify(chargeSummary).apply(any(), any(),any(), ameq(Nil), any(), any(), any(), any(), any(), any(), ameq(true))(any(), any(), any())
					verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
				}
			}

			"raise an error" when {
				"call for charge history list fails" in new Setup() {
					enable(ChargeHistory)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
					mockSingleBusinessIncomeSource()

					val emulatedServiceError = new IllegalStateException("Emulated service error")
					mockGetChargeHistoryDetails(response = Future.failed(emulatedServiceError))

					val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
						.apply(fakeRequestConfirmedClient("AB123456C"))

					result.failed.futureValue shouldBe an[Throwable]
					result.failed.futureValue shouldBe emulatedServiceError
				}
			}
		}

		"the ChargeHistory feature switch is not enabled and the page can be viewed" should {
			"not pass any charge history list to the view" in new Setup() {
				disable(ChargeHistory)
				disable(PaymentAllocation)
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
				mockSingleBusinessIncomeSource()

				val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123)
					.apply(fakeRequestConfirmedClient("AB123456C"))

				status(result) shouldBe OK
				verify(chargeSummary).apply(any(), any(),any(),ameq(Nil), any(), any(), any(), any(), any(), any(), ameq(true))(any(), any(), any())
				verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
			}

			"not pass any charge history list to the view when viewing a Late Payment Interest summary" in new Setup() {
				disable(ChargeHistory)
				disable(PaymentAllocation)
				setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
				mockGetAllFinancialDetails(List((currentYear, currentFinancialDetails)))
				mockSingleBusinessIncomeSource()

				val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, id1040000123, isLatePaymentCharge = true)
					.apply(fakeRequestConfirmedClient("AB123456C"))

				status(result) shouldBe OK
				verify(chargeSummary).apply(any(),any(),any(),ameq(Nil), any(), any(), any(), any(), any(), any(), ameq(true))(any(), any(), any())
				verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
			}
		}
  }

  ".backUrl" when {

    "the user comes from what you owe page" should {
      "return what you owe page url string result" in new Setup() {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("paymentDue"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"

      }
    }

    "the user comes from tax overview page payment tab" should {
      "return payment tab url string result" in new Setup() {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("taxYearOverview"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2021#payments"

      }
    }

    "the user comes is not from tax overview payment tab and is not from what you owe page" should {
      "return agent home page url string result" in new Setup() {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("_"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/income-tax-account"

      }
    }
  }
}
