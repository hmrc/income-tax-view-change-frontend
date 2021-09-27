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



import assets.PaymentAllocationsTestConstants._
import config.{FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, PaymentAllocation}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.core.Nino
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import services.PaymentAllocationsService
import services.PaymentAllocationsService.PaymentAllocationError
import views.html.PaymentAllocation

import scala.concurrent.Future

class PaymentAllocationControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching with TestSupport  {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  trait Setup {
    val docNumber = "docNumber1"

    val paymentAllocation: PaymentAllocationsService = mock[PaymentAllocationsService]

    val controller = new PaymentAllocationsController(
      app.injector.instanceOf[PaymentAllocation],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      paymentAllocation,
      mockAuditingService
    )(app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[FrontendAppConfig])

  }


  "The PaymentAllocationsControllerSpec.viewPaymentAllocation function" should {
    val successfulResponse = Right(paymentAllocationViewModel)

    "behave appropriately when the feature switch is on" when {
      "Successfully retrieving a user's payment allocation" in new Setup {
        enable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(paymentAllocation.getPaymentAllocation(Nino(any()), any())(any()))
          .thenReturn(Future.successful(successfulResponse))

        val result = await(controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession))

        status(result) shouldBe Status.OK
      }

      "Failing to retrieve a user's payment allocation" in new Setup {
        enable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(paymentAllocation.getPaymentAllocation(Nino(any()), any())(any()))
          .thenReturn(Future.successful(Left(PaymentAllocationError)))

        val result = await(controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "behave appropriately when the feature switch is off" when {
      "trying to access payments allocation " in new Setup {
        disable(PaymentAllocation)
        mockSingleBusinessIncomeSource()
        when(paymentAllocation.getPaymentAllocation(Nino(any()), any())(any()))
          .thenReturn(Future.successful(successfulResponse))

        val result = await(controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession))

        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        mockErrorIncomeSource()
        val result = await(controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" should {
      "redirect the user to the login page" in new Setup {
        setupMockAuthorisationException()
        val result = await(controller.viewPaymentAllocation(documentNumber = docNumber)(fakeRequestWithActiveSession))

        status(result) shouldBe Status.SEE_OTHER

        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }
}
