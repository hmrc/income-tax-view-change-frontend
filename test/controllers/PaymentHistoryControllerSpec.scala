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

import config.featureswitch.{FeatureSwitching, PaymentHistory}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.financialDetails.Payment
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import services.PaymentHistoryService
import services.PaymentHistoryService.PaymentHistoryError

import scala.concurrent.Future

class PaymentHistoryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentHistory)
  }

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), Some("Payment"), Some("lot"), Some("lotitem"), Some("2019-12-25")),
    Payment(Some("BBBBB"), Some(5000), Some("tnemyap"), Some("lot"), Some("lotitem"), Some("2007-03-23"))
  )

  trait Setup {

    val paymentHistoryService: PaymentHistoryService = mock[PaymentHistoryService]

    val controller = new PaymentHistoryController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler],
      paymentHistoryService,
      app.injector.instanceOf[ImplicitDateFormatterImpl]
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

        val result = await(controller.viewPaymentHistory(fakeRequestWithActiveSession))

        status(result) shouldBe Status.OK
      }

    }

    "Failing to retrieve a user's payments" should {
      "send the user to the internal service error page" in new Setup {
        mockSingleBusinessIncomeSource()
        when(paymentHistoryService.getPaymentHistory(any(), any()))
          .thenReturn(Future.successful(Left(PaymentHistoryError)))

        val result = await(controller.viewPaymentHistory(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        mockErrorIncomeSource()
        val result = await(controller.viewPaymentHistory(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" should {
      "redirect the user to the login page" in new Setup {
        setupMockAuthorisationException()
        val result = await(controller.viewPaymentHistory(fakeRequestWithActiveSession))

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }

}
