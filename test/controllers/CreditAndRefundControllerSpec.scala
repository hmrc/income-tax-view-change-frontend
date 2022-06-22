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

import config.featureswitch.{CreditsRefundsRepay, FeatureSwitching, MFACreditsAndDebits}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.financialDetails.FinancialDetailsModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import services.CreditService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.FinancialDetailsTestConstants._
import views.html.errorPages.CustomNotFoundError
import views.html.CreditAndRefunds

import scala.concurrent.Future

class CreditAndRefundControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with FeatureSwitching {


  trait Setup {

    val mockCreditService: CreditService = mock[CreditService]

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
      languageUtils = languageUtils,
      mcc =  app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      view = app.injector.instanceOf[CreditAndRefunds],
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError]
    )
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  "The CreditAndRefund Controller" should {
    "show the credit and refund page" when {

      "MFACreditsAndDebits disabled: credit charges are returned" in new Setup {
        enable(CreditsRefundsRepay)
        disable(MFACreditsAndDebits)
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

      "redirect to the custom not found error page" in new Setup {
        disable(CreditsRefundsRepay)
        // redirect even in case MFACreditsAndDebits is ON
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
        contentAsString(result) shouldBe  expectedContent
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
