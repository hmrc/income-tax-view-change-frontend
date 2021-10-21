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

package controllers

import testConstants.FinancialDetailsTestConstants._
import audit.mocks.MockAuditingService
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation}
import config.{FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails.{FinancialDetail, FinancialDetailsResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.FinancialDetailsService
import testUtils.TestSupport

import scala.concurrent.Future

class ChargeSummaryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with TestSupport {

  def financialDetailsWithLocks(taxYear: Int): List[FinancialDetail] = List(
    financialDetail(taxYear = taxYear, chargeType = "ITSA England & NI"),
    financialDetail(taxYear = taxYear, chargeType = "NIC4 Wales", dunningLock = Some("Stand over order"))
  )

	def testChargeHistoryModel(): ChargesHistoryModel = ChargesHistoryModel("MTDBSA", "XAIT000000000", "ITSA", None)

  class Setup(financialDetails: FinancialDetailsResponseModel,
							chargeHistory: ChargeHistoryResponseModel = testChargeHistoryModel()) {
    val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
		val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

    when(financialDetailsService.getAllFinancialDetails(any(), any(), any()))
      .thenReturn(Future.successful(List((2018, financialDetails))))

		when(incomeTaxViewChangeConnector.getChargeHistory(any(), any())(any()))
			.thenReturn(Future.successful(chargeHistory))

    mockBothIncomeSources()

    val controller = new ChargeSummaryController(
      MockAuthenticationPredicate,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      financialDetailsService,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler],
			incomeTaxViewChangeConnector,
      app.injector.instanceOf[views.html.ChargeSummary]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      languageUtils,
      app.injector.instanceOf[MessagesControllerComponents],
      ec
    )
  }

  val errorHeading = "Sorry, there is a problem with the service"
  val successHeading = "Tax year 6 April 2017 to 5 April 2018 Payment on account 1 of 2"
  val dunningLocksBannerHeading = "Important"
  val paymentBreakdownHeading = "Payment breakdown"
  val paymentHistoryHeading = "Payment history"
  val lateInterestSuccessHeading = "Tax year 6 April 2017 to 5 April 2018 Late payment interest on payment on account 1 of 2"

  "The ChargeSummaryController" should {

    "redirect a user back to the home page" when {

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel(2018)) {
        val result = controller.showChargeSummary(2018, "fakeId")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }
    }

    "load the page" when {

      "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel(2018)) {
				enable(ChargeHistory)
				enable(PaymentAllocation)
				val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentBreakdownHeading
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

			"provided with an id and the late payment interest flag enabled that matches a charge in the financial response" in new Setup(financialDetailsModel(2018)) {
				enable(ChargeHistory)
        disable(PaymentAllocation)
				val result = controller.showChargeSummary(2018, "1040000123", isLatePaymentCharge = true)(fakeRequestWithActiveSession)

				status(result) shouldBe Status.OK
				JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentHistoryHeading
			}

      "provided with late payment interest flag and a matching id in the financial response with locks, not showing the locks banner" in new Setup(
        financialDetailsModel(2018).copy(financialDetails = financialDetailsWithLocks(2018))) {

        val result = controller.showChargeSummary(2018, "1040000123", isLatePaymentCharge = true)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").size() shouldBe 0
      }

      "provided with a matching id in the financial response with dunning locks, showing the locks banner" in new Setup(
        financialDetailsModel(2018).copy(financialDetails = financialDetailsWithLocks(2018))) {

        val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled and allocations present" in new Setup(chargesWithAllocatedPaymentModel(2018)) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentBreakdownHeading
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled but without allocations" in new Setup(chargesWithAllocatedPaymentModel(2018)) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentBreakdownHeading
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS disabled" in new Setup(financialDetailsModel(2018)) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentBreakdownHeading
      }

    }

    "load an error page" when {

			"the charge history response is an error" in new Setup(financialDetailsModel(2018), chargeHistory = ChargesHistoryErrorModel(500, "Failure")) {
				enable(ChargeHistory)
				val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

				status(result) shouldBe Status.INTERNAL_SERVER_ERROR
				JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
			}

      "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing) {
        val result = controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }
    }
  }
}
