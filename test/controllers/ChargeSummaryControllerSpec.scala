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

import audit.mocks.MockAuditingService
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.{ChargeHistoryConnector, FinancialDetailsConnector}
import controllers.predicates.{FeatureSwitchPredicate, NavBarPredicate, SessionTimeoutPredicate}
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, NIC4_WALES}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.admin.{ChargeHistory, CodingOut, MFACreditsAndDebits, PaymentAllocation}
import models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails.{DocumentDetail, FinancialDetail, FinancialDetailsResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{ChargeHistoryService, DateService, FinancialDetailsService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testTaxYear}
import testConstants.FinancialDetailsTestConstants._
import testUtils.TestSupport
import utils.AuthenticatorPredicate

import java.time.LocalDate
import scala.concurrent.Future


class ChargeSummaryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with TestSupport {

  def financialDetailsWithLocks(taxYear: Int): List[FinancialDetail] = List(
    financialDetail(taxYear = taxYear, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(taxYear = taxYear, chargeType = NIC4_WALES, dunningLock = Some("Stand over order"))
  )

  def testChargeHistoryModel(): ChargesHistoryModel = ChargesHistoryModel("NINO", "AB123456C", "ITSA", None)

  def emptyAdjustmentHistoryModel: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1000, None, "amend"), List())

  class Setup(financialDetails: FinancialDetailsResponseModel,
              adjustmentHistoryModel: AdjustmentHistoryModel = emptyAdjustmentHistoryModel,
              chargeHistoryResponse: Either[ChargesHistoryErrorModel, List[ChargeHistoryModel]] = Right(List()),
              isAgent: Boolean = false) {
    val financialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
    val mockChargeHistoryService: ChargeHistoryService = mock(classOf[ChargeHistoryService])


    when(financialDetailsService.getAllFinancialDetails(any(), any(), any()))
      .thenReturn(Future.successful(List((2018, financialDetails))))

    when(mockChargeHistoryService.chargeHistoryResponse(any(), any(), any(), any(), any())(any(),any(),any()))
      .thenReturn(Future.successful(chargeHistoryResponse))

    when(mockChargeHistoryService.getAdjustmentHistory(any(),any()))
      .thenReturn(adjustmentHistoryModel)

    mockBothIncomeSources()
    if (isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    }

    val controller = new ChargeSummaryController(
      MockAuthenticationPredicate,
      testAuthenticator,
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockIncomeSourceDetailsPredicate,
      financialDetailsService,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[views.html.ChargeSummary],
      app.injector.instanceOf[NavBarPredicate],
      mockIncomeSourceDetailsService,
      mockChargeHistoryService,
      mockAuthService,
      app.injector.instanceOf[views.html.errorPages.CustomNotFoundError],
      app.injector.instanceOf[FeatureSwitchPredicate]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[DateService],
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
  val paymentHistoryHeading: String = messages("chargeSummary.chargeHistory.Poa1heading")
  val lateInterestSuccessHeading = s"Tax year 6 April 2017 to 5 April 2018 ${messages("chargeSummary.lpi.paymentOnAccount1.text")}"
  val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.payments-bullet2")}"
  val paymentprocessingbullet1Agent: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2-agent")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.payments-bullet2-agent")}"

  "The ChargeSummaryController for Individuals" should {

    "redirect a user back to the home page" when {

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel()) {
        val result: Future[Result] = controller.show(testTaxYear, "fakeId")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
      }
    }

    "redirect a user back to the custom error page" when {

      "coding out exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOutNics2()) {
        disable(CodingOut)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
      }
    }

    "load the page" when {

      "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel()) {
        enable(ChargeHistory)
        enable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 1
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
      }

      "redirect a user back to the custom error page" when {

        "PAYE SA exists but FS is disabled" in new Setup(testFinancialDetailsModelWithPayeSACodingOut()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
        }

        "class 2 Nics exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOutNics2()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
        }
        "cancelled PAYE exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCancelledPayeSa()) {
          disable(CodingOut)
          val result: Future[Result] = controller.show(testTaxYear, "CODINGOUT01")(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.errors.routes.NotFoundDocumentIDLookupController.show.url)
        }
      }


      "the late payment interest flag is enabled" in new Setup(
        financialDetailsModel(lpiWithDunningLock = None)) {
        enable(ChargeHistory)
        disable(PaymentAllocation)

        val result: Future[Result] = controller.show(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentHistoryHeading
      }

      "provided with dunning locks and late payment interest flag, not showing the locks banner" in new Setup(
        financialDetailsModel(lpiWithDunningLock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {

        val result: Future[Result] = controller.show(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").size() shouldBe 0
      }

      "provided with dunning locks, showing the locks banner" in new Setup(
        financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear))) {

        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
      }

      "has Payment allocation FS" that {
        "is enabled" when {
          "allocations present" in new Setup(
            chargesWithAllocatedPaymentModel()) {
            disable(ChargeHistory)
            enable(PaymentAllocation)
            val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

            status(result) shouldBe Status.OK
            val doc = JsoupParse(result).toHtmlDocument
            doc.select("h1").text() shouldBe successHeading
            doc.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
            doc.select("main h3").text() shouldBe paymentHistoryHeading

            val allocationLink = doc.select("#payment-history-table").select("tr").get(1).select("a").attr("href")
            allocationLink shouldBe s"/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=${id1040000124}"
          }

          "without allocations" in new Setup(
            chargesWithAllocatedPaymentModel()) {
            disable(ChargeHistory)
            enable(PaymentAllocation)
            val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

            status(result) shouldBe Status.OK
            JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
            JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
            JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
          }
        }

        "is disabled" in new Setup(
          financialDetailsModel()) {
          disable(ChargeHistory)
          disable(PaymentAllocation)
          val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

          status(result) shouldBe Status.OK
          JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
          JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        }
      }

      "display any payments you make" in new Setup(
        financialDetailsModel()) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#payment-processing-bullets").text() shouldBe s"$paymentprocessingbullet1"
      }
    }

    "load an error page" when {

      "the charge history response is an error" in new Setup(
        financialDetailsModel(), chargeHistoryResponse = Left(ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure"))) {
        enable(ChargeHistory)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }

      "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing) {
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }

      "no related tax year financial details found" in new Setup(testFinancialDetailsModelWithPayeSACodingOut()) {
        val result: Future[Result] = controller.show(2020, "CODINGOUT01")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }
    }

    "Displays an MFA Debit charge" when {
      "the charge is an MFA Debit and MFACreditsAndDebits FS is Enabled" in new Setup(
        financialDetailsModelWithMFADebit()) {
        enable(MFACreditsAndDebits)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe "Tax year 6 April 2017 to 5 April 2018 " +
          messages("chargeSummary.hmrcAdjustment.text")
      }
    }
    "Redirects to Not Found Page" when {
      "the charge is an MFA Debit and MFACreditsAndDebits FS is Disabled" in new Setup(
        financialDetailsModelWithMFADebit()) {
        disable(MFACreditsAndDebits)
        val result: Future[Result] = controller.show(testTaxYear, "1040000123")(fakeRequestWithNinoAndOrigin("PTA"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe messages("error.custom.heading")
      }
    }
  }

  "The ChargeSummaryController for Agents" should {

    "redirect a user back to the home page" when {

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel(), isAgent = true) {
        val result: Future[Result] = controller.showAgent(testTaxYear, "fakeId")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show.url)
      }
    }

    "redirect a user back to the custom error page" when {

      "coding out exists but FS is disabled" in new Setup(testFinancialDetailsModelWithCodingOutNics2(), isAgent = true) {
        disable(CodingOut)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show.url)
      }

      "display any payments you make with contents for agent" in new Setup(
        financialDetailsModel(), isAgent = true) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("#payment-processing-bullets").text() shouldBe s"$paymentprocessingbullet1Agent"
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

      "the late payment interest flag is enabled" in new Setup(
        financialDetailsModel(lpiWithDunningLock = None), isAgent = true) {
        enable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe lateInterestSuccessHeading
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe paymentHistoryHeading
      }


      "provided with dunning locks, showing the locks banner" in new Setup(
        financialDetailsModel().copy(financialDetails = financialDetailsWithLocks(testTaxYear)), isAgent = true) {

        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner h2").text() shouldBe dunningLocksBannerHeading
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").text() shouldBe paymentBreakdownHeading
      }

      "provided with dunning locks and a late payment interest flag, not showing the locks banner" in new Setup(
        financialDetailsModel(lpiWithDunningLock = None).copy(financialDetails = financialDetailsWithLocks(testTaxYear)), isAgent = true) {

        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123", isLatePaymentCharge = true)(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("#dunningLocksBanner").size() shouldBe 0
        JsoupParse(result).toHtmlDocument.select("#heading-payment-breakdown").size() shouldBe 0
      }


      "the Charge History FS disabled and the Payment allocation FS enabled" when {
        "allocations present" in new Setup(
          chargesWithAllocatedPaymentModel(), isAgent = true) {
          disable(ChargeHistory)
          enable(PaymentAllocation)
          val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

          status(result) shouldBe Status.OK
          JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
          JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
          JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
        }

        // TODO: Setup is same as above test - needs a model without payment allocations, and paymentHistoryHeading should be absent?
        "allocations not present" in new Setup(
          chargesWithAllocatedPaymentModel(), isAgent = true) {
          disable(ChargeHistory)
          enable(PaymentAllocation)
          val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

          status(result) shouldBe Status.OK
          JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
          JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
          JsoupParse(result).toHtmlDocument.select("main h3").text() shouldBe paymentHistoryHeading
        }
      }

      "with the Charge History FS disabled and the Payment allocation FS disabled" in new Setup(
        financialDetailsModel(), isAgent = true) {
        disable(ChargeHistory)
        disable(PaymentAllocation)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
        JsoupParse(result).toHtmlDocument.select("main h2").text() shouldBe s"$dunningLocksBannerHeading $paymentBreakdownHeading"
        JsoupParse(result).toHtmlDocument.select("main h3").size() shouldBe 0
      }
    }

    "load an error page" when {

      "the charge history response is an error" in new Setup(
        financialDetailsModel(), chargeHistoryResponse = Left(ChargesHistoryErrorModel(INTERNAL_SERVER_ERROR, "Failure")), isAgent = true) {
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

    "Display an MFA Debit charge" when {
      "the charge is an MFA Debit and MFACreditsAndDebits FS is Enabled" in new Setup(
        financialDetailsModelWithMFADebit(), isAgent = true) {
        enable(MFACreditsAndDebits)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe "Tax year 6 April 2017 to 5 April 2018 " +
          messages("chargeSummary.hmrcAdjustment.text")
      }
    }
    "Redirect to Not Found Page" when {
      "the charge is an MFA Debit and MFACreditsAndDebits FS is Disabled" in new Setup(
        financialDetailsModelWithMFADebit(), isAgent = true) {
        disable(MFACreditsAndDebits)
        val result: Future[Result] = controller.showAgent(testTaxYear, "1040000123")(fakeRequestConfirmedClient("AB123456C"))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe messages("error.custom.heading")
      }
    }
  }
}
