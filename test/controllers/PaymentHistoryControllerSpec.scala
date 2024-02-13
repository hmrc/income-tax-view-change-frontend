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

import config.featureswitch.{FeatureSwitching, PaymentHistoryRefunds}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.financialDetails.Payment
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.PaymentHistoryService.PaymentHistoryError
import services.{DateService, PaymentHistoryService, RepaymentService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import uk.gov.hmrc.http.InternalServerException
import views.html.{CreditAndRefunds, PaymentHistory}
import views.html.errorPages.CustomNotFoundError

import scala.concurrent.Future

class PaymentHistoryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter
  with MockItvcErrorHandler with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with FeatureSwitching{

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"),
      "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"),
      "2007-03-23", Some("DOCID02"))
  )

  trait Setup {
    val paymentHistoryService: PaymentHistoryService = mock(classOf[PaymentHistoryService])
    val mockRepaymentService: RepaymentService = mock(classOf[RepaymentService])

    val controller = new PaymentHistoryController(
      app.injector.instanceOf[PaymentHistory],
      mockAuthService,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler],
      paymentHistoryService,
      testAuthenticator,
      MockNavBarPredicate,
      MockAuthenticationPredicate,
      app.injector.instanceOf[SessionTimeoutPredicate],
      mockRepaymentService,
      MockIncomeSourceDetailsPredicate
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

  "The PaymentHistoryControllerSpec.viewPaymentsHistory function" when {

    "obtaining a users payments" should {
      "send the user to the paymentsHistory page with data" in new Setup {
        disableAllSwitches()
        mockSingleBusinessIncomeSource()
        when(paymentHistoryService.getPaymentHistory(any(), any()))
          .thenReturn(Future.successful(Right(testPayments)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("paymentHistory")
      }

    }

    "Failing to retrieve a user's payments" should {
      "send the user to the internal service error page" in new Setup {
        mockSingleBusinessIncomeSource()
        when(paymentHistoryService.getPaymentHistory(any(), any()))
          .thenReturn(Future.successful(Left(PaymentHistoryError)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        mockErrorIncomeSource()
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" should {
      "redirect the user to the login page" in new Setup {
        setupMockAuthorisationException()
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }

    "return the refund status" when {
      "RepaymentJourneyModel is returned for an Individual user" in new Setup {
        disableAllSwitches()
        enable(PaymentHistoryRefunds)

        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockRepaymentService.view(any())(any()))
          .thenReturn(Future.successful(Right("/test/url")))

        val result: Future[Result] = controller.refundStatus(false)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
      }
    }

  }


  "The PaymentHistoryControllerSpec showAgent function" when {

    "obtaining a users payments - right" should {
      "send the user to the paymentsHistory page with data" in new Setup {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBusinessIncomeSource()
        when(paymentHistoryService.getPaymentHistory(any(), any()))
          .thenReturn(Future.successful(Right(testPayments)))
        val result = controller.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("paymentHistory")
      }

    }

    "Failing to retrieve a user's payments - left" should {
      "send the user to the internal service error page" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        when(paymentHistoryService.getPaymentHistory(any(), any()))
          .thenReturn(Future.successful(Left(PaymentHistoryError)))
        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        result.failed.futureValue shouldBe an[InternalServerException]

      }

    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        result.failed.futureValue shouldBe an[InternalServerException]
      }
    }

    "User fails to be authorised" in new Setup {
      setupMockAgentAuthorisationException(withClientPredicate = false)
      val result: Future[Result] = controller.showAgent()(fakeRequestWithActiveSession)
      status(result) shouldBe Status.SEE_OTHER

    }

    "RepaymentJourneyModel is returned for an Agent user" in new Setup {
      disableAllSwitches()
      enable(PaymentHistoryRefunds)

      mockSingleBISWithCurrentYearAsMigrationYear()
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

      val result: Future[Result] = controller.refundStatus(true)(fakeRequestConfirmedClient())

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }


  }

}
