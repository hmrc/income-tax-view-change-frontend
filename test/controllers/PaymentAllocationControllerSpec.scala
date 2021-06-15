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

import config.featureswitch.{FeatureSwitching, PaymentAllocation}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.redirectLocation
import connectors.IncomeTaxViewChangeConnector
import models.paymentAllocationCharges.DocumentDetail

import scala.concurrent.Future

class PaymentAllocationControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentAllocation)
  }

  val testPaymentAllocation: List[DocumentDetail] = List(
    DocumentDetail(Some("2019") ,Some("1040000872"), Some("1482"), Some(10.90), Some(5.90),Some("2019-09-28"))
  )

  trait Setup {

    val paymentAllocation: IncomeTaxViewChangeConnector = mock[IncomeTaxViewChangeConnector]

    val controller = new PaymentAllocationsController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      paymentAllocation,
      app.injector.instanceOf[ImplicitDateFormatterImpl]
    )(app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[FrontendAppConfig])

  }

  "The PaymentAllocationsControllerSpec.viewPaymentAllocation function" when {

    "obtaining a users payments allocations" should {
      "send the user to the paymentAllocation page with data" in new Setup {
        mockSingleBusinessIncomeSource()
        when(paymentAllocation.getPaymentAllocation(any(), any()))
          .thenReturn(Future.successful(Right(testPaymentAllocation)))

        val result = await(controller.viewPaymentAllocation(fakeRequestWithActiveSession))

        status(result) shouldBe Status.OK
      }

    }

    "Failing to retrieve a user's payment allocation" should {
      "send the user to the internal service error page" in new Setup {
        mockSingleBusinessIncomeSource()
        when(paymentAllocation.getPaymentAllocation(any(), any()))
          .thenReturn(Future.successful(Left()))

        val result = await(controller.viewPaymentAllocation(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        mockErrorIncomeSource()
        val result = await(controller.viewPaymentAllocation(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" should {
      "redirect the user to the login page" in new Setup {
        setupMockAuthorisationException()
        val result = await(controller.viewPaymentAllocation(fakeRequestWithActiveSession))

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }

}
