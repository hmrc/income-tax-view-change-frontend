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

import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.financialDetails.Payment
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.PaymentHistoryService
import services.PaymentHistoryService.PaymentHistoryError
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory

import scala.concurrent.Future

class PaymentHistoryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with MockItvcErrorHandler {

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
    val paymentHistoryService: PaymentHistoryService = mock[PaymentHistoryService]

    val controller = new PaymentHistoryController(
      app.injector.instanceOf[PaymentHistory],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      mockIncomeSourceDetailsService,
      mockAuthService,
      mockAuditingService,
      app.injector.instanceOf[NavBarPredicate],
      app.injector.instanceOf[ItvcErrorHandler],
      mockItvcErrorHandler,
      paymentHistoryService,
      languageUtils
    )(app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[FrontendAppConfig])

  }

  "The PaymentHistoryControllerSpec.viewPaymentsHistory function" when {

    "obtaining a users payments" should {
      "send the user to the paymentsHistory page with data" in new Setup {
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
  }


  "The PaymentHistoryControllerSpec showAgent function" when {

    "obtaining a users payments - right" should {
      "send the user to the paymentsHistory page with data" in new Setup {
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
  }

}
