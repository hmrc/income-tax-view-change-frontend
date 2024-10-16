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

import auth.authV2.actions._
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import mocks.auth.{MockAuthActions, MockFrontendAuthorisedFunctions}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.admin.CreditsRefundsRepay
import models.financialDetails.FinancialDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{CreditService, RepaymentService}
import testConstants.BaseTestConstants.{testAuthAgentSuccessWithSaUtrResponse, testTaxYearTo}
import testConstants.FinancialDetailsTestConstants._
import testConstants.{ANewCreditAndRefundModel, BaseTestConstants}
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import java.time.LocalDate
import scala.concurrent.Future

class CreditAndRefundControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with FeatureSwitching with MockAuthActions {


  trait Setup {

    val mockCreditService: CreditService = mock(classOf[CreditService])
    val mockRepaymentService: RepaymentService = mock(classOf[RepaymentService])

    val controller = new CreditAndRefundController(
      authActions = mockAuthActions,
      creditService = mockCreditService,
      repaymentService = mockRepaymentService,
      auditingService = mockAuditingService,
      view = app.injector.instanceOf[CreditAndRefunds],
      controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
    )(
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      languageUtils = languageUtils,
      ec = ec,
      individualErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      agentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError]
    )
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  "The CreditAndRefund Controller" should {
    "show the credit and refund page" when {

      "MFACreditsAndDebits disabled: credit charges are returned" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()


        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK

        setupMockAuthRetrievalSuccess(testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe Status.OK

      }

      "credit charges are returned" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK

        setupMockAuthRetrievalSuccess(testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe Status.OK

      }

      "credit charges are returned in sorted order of credits" in new Setup {
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 250.0)
            .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 125.0)
            .withPayment(LocalDate.parse("2022-08-16"), 100.0)
            .withPayment(LocalDate.parse("2022-08-16"), 500.0)
            .withPayment(LocalDate.parse("2022-08-16"), 300.0)
            .withMfaCredit(LocalDate.of(2019, 5, 15), 100.0)
            .withMfaCredit(LocalDate.of(2019, 5, 15), 1000.0)
            .withMfaCredit(LocalDate.of(2019, 5, 15), 800.0)
            .withCutoverCredit(LocalDate.of(2019, 5, 15), 200.0)
            .withCutoverCredit(LocalDate.of(2019, 5, 15), 2000.0)
            .withCutoverCredit(LocalDate.of(2019, 5, 15), 700.0)
            .withFirstRefund(4.0)
            .withSecondRefund(2.0)
            .get()))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK

        setupMockAuthRetrievalSuccess(testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(resultAgent) shouldBe Status.OK

        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.select("#main-content").select("li:nth-child(1)")
          .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
        doc.select("#main-content").select("li:nth-child(2)")
          .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
        doc.select("#main-content").select("li:nth-child(3)")
          .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
        doc.select("#main-content").select("li:nth-child(4)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(5)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(6)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(7)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(8)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(9)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-balancing-charge-prt-1") + " " +
          messages("credit-and-refund.credit-from-balancing-charge-prt-2") + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(10)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-balancing-charge-prt-1") + " " +
          messages("credit-and-refund.credit-from-balancing-charge-prt-2") + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(11)")
          .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
        doc.select("#main-content").select("li:nth-child(12)")
          .select("p").first().text() shouldBe "£4.00 " + messages("credit-and-refund.refundProgress-prt-2")
        doc.select("#main-content").select("li:nth-child(13)")
          .select("p").first().text() shouldBe "£2.00 " + messages("credit-and-refund.refundProgress-prt-2")
      }

      "redirect to the custom not found error page" in new Setup {
        disableAllSwitches()

        mockSingleBISWithCurrentYearAsMigrationYear()

        val expectedContent: String = controller.customNotFoundErrorView().toString()

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent

        setupMockAuthRetrievalSuccess(testAuthAgentSuccessWithSaUtrResponse())
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(resultAgent) shouldBe Status.OK
      }

      "User fails to be authorised" in new Setup {
        setupMockEnroledAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "start refund process" when {
      "RepaymentJourneyModel is returned for an Individual user" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        when(mockRepaymentService.start(any(), any())(any()))
          .thenReturn(Future.successful(Right("/test/url")))
        val result: Future[Result] = controller.startRefund()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "not start refund process" when {
      "RepaymentJourneyModel is returned for an Agent user" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthAgentSuccessWithSaUtrResponse())

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        when(mockRepaymentService.start(any(), any())(any()))
          .thenReturn(Future.successful(Right("/test/url")))

        val result: Future[Result] = controller.startRefund()(fakePostRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "RepaymentJourneyErrorResponse is returned" in  new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        when(mockRepaymentService.start(any(), any())(any()))
          .thenReturn(Future.successful(Left(new InternalError)))

        val result: Future[Result] = controller.startRefund()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "CreditsRefundsRepay FS is disabled" in new Setup {
        disableAllSwitches()

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = controller.startRefund()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
      }
    }
  }
}
