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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import exceptions.{IndividualException, RepaymentStartJourneyException}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.admin.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import models.financialDetails.FinancialDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import services.{CreditService, DateService, RepaymentService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testTaxYearTo}
import testConstants.FinancialDetailsTestConstants._
import testConstants.{ANewCreditAndRefundModel, BaseTestConstants}
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import java.time.LocalDate
import scala.concurrent.Future

class CreditAndRefundControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching {


  trait Setup {

    val mockCreditService: CreditService = mock(classOf[CreditService])
    val mockRepaymentService: RepaymentService = mock(classOf[RepaymentService])

    val controller = new CreditAndRefundController(
      authorisedFunctions = mockAuthService,
      creditService = mockCreditService,
      repaymentService = mockRepaymentService,
      auditingService = mockAuditingService,
      auth = testAuthenticator,
      view = app.injector.instanceOf[CreditAndRefunds],
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError]
    )(
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      dateService = app.injector.instanceOf[DateService],
      languageUtils = languageUtils,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    )
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  "The CreditAndRefund Controller" should {
    "show the credit and refund page" when {

      "MFACreditsAndDebits disabled: credit charges are returned" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        status(resultAgent) shouldBe Status.OK

      }

      "MFACreditsAndDebits enabled: credit charges are returned" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)
        enable(MFACreditsAndDebits)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        status(resultAgent) shouldBe Status.OK

      }

      "MFACreditsAndDebits enabled: credit charges are returned in sorted order of credits" in new Setup {
        enable(CreditsRefundsRepay)
        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

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

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
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
        enable(MFACreditsAndDebits)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val expectedContent: String = controller.customNotFoundErrorView().toString()

        when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
          ANewCreditAndRefundModel()
            .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
            .get()))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
        status(resultAgent) shouldBe Status.OK
      }

      "User fails to be authorised" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

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

        recoverToExceptionIf[IndividualException] {
          controller.startRefund()(fakeRequestWithActiveSession)
        }.map( ex => ex.getMessage shouldBe "Agent tried to start refund").futureValue

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
          .thenReturn(Future.successful(Left(RepaymentStartJourneyException(500, "Internal Error"))))

        recoverToExceptionIf[IndividualException] {
          controller.startRefund()(fakeRequestWithActiveSession)
        }.map( ex => ex.getMessage shouldBe "Repayment journey start error with response code: 500 and message: Internal Error")
          .futureValue
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
