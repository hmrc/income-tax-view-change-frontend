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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{CreditsRefundsRepay, CutOverCredits, FeatureSwitching, MFACreditsAndDebits}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.financialDetails.FinancialDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import services.{CreditService, DateService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.FinancialDetailsTestConstants._
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import scala.concurrent.Future

class CreditAndRefundControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with FeatureSwitching {


  trait Setup {

    val mockCreditService: CreditService = mock(classOf[CreditService])

    val controller = new CreditAndRefundController(
      authorisedFunctions = mockAuthService,
      retrieveBtaNavBar = MockNavBarPredicate,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      authenticate = MockAuthenticationPredicate,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      creditService = mockCreditService
    )(
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      dateService = app.injector.instanceOf[DateService],
      languageUtils = languageUtils,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      view = app.injector.instanceOf[CreditAndRefunds],
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError]
    )
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  "The CreditAndRefund Controller" should {
    "show the credit and refund page" when {

      "MFACreditsAndDebits disabled: credit charges are returned" in new Setup {
        disableAllSwitches()
        enable(CreditsRefundsRepay)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditAndRefundCharge)))

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
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditAndRefundCharge)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        status(resultAgent) shouldBe Status.OK

      }

      "MFACreditsAndDebits enabled: credit charges are returned (descending values) and sorted according to credit/refund/payment type" in new Setup {
        enable(CreditsRefundsRepay)
        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(mockCreditService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditAndRefundChargeAllCreditTypes)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        status(resultAgent) shouldBe Status.OK

        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.select("#main-content").select("li:nth-child(1)")
          .select("p").first().text() shouldBe "£1,000.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credit-and-refund.credit-from-hmrc-title-prt-2") + " 0"
        doc.select("#main-content").select("li:nth-child(2)")
          .select("p").first().text() shouldBe "£800.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credit-and-refund.credit-from-hmrc-title-prt-2") + " 1"
        doc.select("#main-content").select("li:nth-child(3)")
          .select("p").first().text() shouldBe "£100.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credit-and-refund.credit-from-hmrc-title-prt-2") + " 2"
        doc.select("#main-content").select("li:nth-child(4)")
          .select("p").first().text() shouldBe "£1.34 " + messages("credit-and-refund.credit-interest-accrued-prt-1") + " " +
          messages("credit-and-refund.credit-interest-accrued-prt-2") + " 2a"
        doc.select("#main-content").select("li:nth-child(5)")
          .select("p").first().text() shouldBe "£2,000.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 3"
        doc.select("#main-content").select("li:nth-child(6)")
          .select("p").first().text() shouldBe "£700.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 4"
        doc.select("#main-content").select("li:nth-child(7)")
          .select("p").first().text() shouldBe "£200.00 " + messages("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
          messages("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 5"
        doc.select("#main-content").select("li:nth-child(8)")
          .select("p").first().text() shouldBe "£500.00 " + messages("credit-and-refund.payment") + " 15 June 2018"
        doc.select("#main-content").select("li:nth-child(9)")
          .select("p").first().text() shouldBe "£300.00 " + messages("credit-and-refund.payment") + " 15 June 2018"
        doc.select("#main-content").select("li:nth-child(10)")
          .select("p").first().text() shouldBe "£100.00 " + messages("credit-and-refund.payment") + " 15 June 2018"
        doc.select("#main-content").select("li:nth-child(11)")
          .select("p").first().text() shouldBe "£4.00 " + messages("credit-and-refund.refundProgress-prt-2")
        doc.select("#main-content").select("li:nth-child(12)")
          .select("p").first().text() shouldBe "£2.00 " + messages("credit-and-refund.refundProgress-prt-2")
      }

      "redirect to the custom not found error page" in new Setup {
        disableAllSwitches()
        enable(MFACreditsAndDebits)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        val expectedContent: String = controller.customNotFoundErrorView().toString()

        when(mockCreditService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List(financialDetailCreditCharge)))

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
  }
}
