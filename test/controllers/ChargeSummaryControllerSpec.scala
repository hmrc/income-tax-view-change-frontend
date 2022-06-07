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

package controllers

import audit.mocks.MockAuditingService
import config.featureswitch.{ChargeHistory, CodingOut, FeatureSwitching, PaymentAllocation}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
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
import services.{FinancialDetailsService, IncomeSourceDetailsService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testTaxYear}
import testConstants.FinancialDetailsTestConstants._
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
              chargeHistory: ChargeHistoryResponseModel = testChargeHistoryModel(),
              isAgent: Boolean = false) {
    val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
    val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

    when(financialDetailsService.getAllFinancialDetails(any(), any(), any()))
      .thenReturn(Future.successful(List((2018, financialDetails))))

    when(incomeTaxViewChangeConnector.getChargeHistory(any(), any())(any()))
      .thenReturn(Future.successful(chargeHistory))

    mockBothIncomeSources()
    if(isAgent){setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)}

    val controller = new ChargeSummaryController(
      MockAuthenticationPredicate,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      financialDetailsService,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler],
      incomeTaxViewChangeConnector,
      app.injector.instanceOf[views.html.ChargeSummary],
      app.injector.instanceOf[NavBarPredicate],
      mockIncomeSourceDetailsService,
      mockAuthService
    )(
      app.injector.instanceOf[FrontendAppConfig],
      languageUtils,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[AgentItvcErrorHandler]
    )
  }

  val errorHeading: String = messages("standardError.heading")
  val successHeading = s"Tax year 6 April 2017 to 5 April 2018 ${messages("chargeSummary.paymentOnAccount1.text")}"
  val dunningLocksBannerHeading: String = messages("chargeSummary.dunning.locks.banner.title")
  val paymentBreakdownHeading: String = messages("chargeSummary.paymentBreakdown.heading")
  val paymentHistoryHeading: String = messages("chargeSummary.chargeHistory.heading")
  val lateInterestSuccessHeading = s"Tax year 6 April 2017 to 5 April 2018 ${messages("chargeSummary.lpi.paymentOnAccount1.text")}"

  "The ChargeSummaryController for Individuals" should {

    "redirect a user back to the home page" when {

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel()) {
        val result: Future[Result] = controller.show(testTaxYear, "fakeId")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
      }
    }

    "redirect a user back to the custom error page" when {

      "coding out exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOut()) {
        disable(CodingOut)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
      }
    }

    "load the page" when {

      "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel()) {
        enable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 1
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }
      "redirect a user back to the custom error page" when {
        "PAYE SA exists but FS is disabled" in new Setup(testFinancialDetailsModelWithPayeSACodingOut()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        }

        "class 2 Nics exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOut()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        }
        "cancelled PAYE exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCancelledPayeSa()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithActiveSession)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        }
      }


      "provided with an id and the late payment interest flag enabled that matches a charge in the financial response" in new Setup(
        financialDetailsModel(lpiWithDunningBlock = None)) {
        enable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentHistoryHeading
      }

      "provided with late payment interest flag and a matching id in the financial response with locks, not showing the locks banner" in new Setup(
        financialDetailsModel(lpiWithDunningBlock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {

        val result: Future[Result] = controller.show(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").size() shouldBe 0
      }

      "provided with a matching id in the financial response with dunning locks, showing the locks banner" in new Setup(
        financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {

        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled and allocations present" in new Setup(
        chargesWithAllocatedPaymentModel()) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled but without allocations" in new Setup(
        chargesWithAllocatedPaymentModel()) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS disabled" in new Setup(
        financialDetailsModel()) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
      }

    }

    "load an error page" when {

      "the charge history response is an error" in new Setup(
        financialDetailsModel(), chargeHistory = ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure")) {
        enable(ChargeHistory)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }

      "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing) {
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }
    }
  }

  "The ChargeSummaryController for Agents" should {

    "redirect a user back to the home page" when {

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel(), isAgent = true) {
        val result: Future[Result] = controller.showAgent(testTaxYear, "fakeId")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
      }
    }

    "redirect a user back to the custom error page" when {

      "coding out exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOut(), isAgent = true) {
        disable(CodingOut)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
      }
    }

    "load the page" when {

      "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel(), isAgent = true) {
        enable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 1
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }
      "redirect a user back to the custom error page" when {
        "PAYE SA exists but FS is disabled" in new Setup(testFinancialDetailsModelWithPayeSACodingOut(), isAgent = true) {
          disable(CodingOut)
          val result: Future[Result] = controller.showAgent(testTaxYear, "CODINGOUT01")(fakeRequestConfirmedClient("AB123456C"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
        }

        "class 2 Nics exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOut(), isAgent = true) {
          disable(CodingOut)
          val result: Future[Result] = controller.showAgent(testTaxYear, "CODINGOUT01")(fakeRequestConfirmedClient("AB123456C"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
        }
        "cancelled PAYE exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCancelledPayeSa(), isAgent = true) {
          disable(CodingOut)
          val result: Future[Result] = controller.showAgent(testTaxYear, "CODINGOUT01")(fakeRequestConfirmedClient("AB123456C"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
        }
      }


      "provided with an id and the late payment interest flag enabled that matches a charge in the financial response" in new Setup(
        financialDetailsModel(lpiWithDunningBlock = None), isAgent = true) {
        enable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentHistoryHeading
      }

      "provided with late payment interest flag and a matching id in the financial response with locks, not showing the locks banner" in new Setup(
        financialDetailsModel(lpiWithDunningBlock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear)), isAgent = true) {

        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").size() shouldBe 0
      }

      "provided with a matching id in the financial response with dunning locks, showing the locks banner" in new Setup(
        financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear)), isAgent = true) {

        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled and allocations present" in new Setup(
        chargesWithAllocatedPaymentModel(), isAgent = true) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS enabled but without allocations" in new Setup(
        chargesWithAllocatedPaymentModel(), isAgent = true) {
        disable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "provided with a matching id with the Charge History FS disabled and the Payment allocation FS disabled" in new Setup(
        financialDetailsModel(), isAgent = true) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
      }

    }

    "load an error page" when {

      "the charge history response is an error" in new Setup(
        financialDetailsModel(), chargeHistory = ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure"), isAgent = true) {
        enable(ChargeHistory)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }

      "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing, isAgent = true) {
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }
    }
  }
}
