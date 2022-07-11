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

import audit.AuditingService
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import config.featureswitch.{FeatureSwitching, PaymentHistoryRefunds}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.core.Nino
import models.financialDetails.Payment
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, RepaymentItem, RepaymentSupplementItem}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString, matches}
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.PaymentHistoryService
import services.PaymentHistoryService.PaymentHistoryError
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.{PaymentHistory, RefundToTaxPayer}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class RefundToTaxPayerControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with MockItvcErrorHandler
  with MockIncomeTaxViewChangeConnector with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val repaymentRequestNumber: String = "023942042349"
  val testNino: String = "AY888881A"

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"),
      "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"),
      "2007-03-23", Some("DOCID02"))
  )

  val testRepaymentHistoryModel: RepaymentHistoryModel = RepaymentHistoryModel(
    List(RepaymentHistory(
      Some(705.2),
      705.2,
      "BACS",
      12345,
      Vector(
        RepaymentItem(
          Vector(
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.78),
              Some(LocalDate.of(2021, 7, 31)),
              Some(LocalDate.of(2021, 9, 15)),
              Some(2.01)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(2.63),
              Some(LocalDate.of(2021, 9, 15)),
              Some(LocalDate.of(2021, 10, 24)),
              Some(1.76)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.26),
              Some(LocalDate.of(2021, 10, 24)),
              Some(LocalDate.of(2021, 11, 30)),
              Some(2.01))
          )
        )
      ), LocalDate.of(2021, 7, 23), LocalDate.of(2021, 7, 21), "000000003135")
    )
  )

  trait Setup {

    val controller = new RefundToTaxPayerController(
      app.injector.instanceOf[RefundToTaxPayer],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      mockIncomeTaxViewChangeConnector,
      MockIncomeSourceDetailsPredicate,
      mockIncomeSourceDetailsService,
      mockAuthService,
      mockAuditingService,
      app.injector.instanceOf[NavBarPredicate],
      app.injector.instanceOf[ItvcErrorHandler]
    )(app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[FrontendAppConfig],
      mockItvcErrorHandler
    )

  }

  "The RefundToTaxPayerController.show function" when {

    "obtaining a users repayments when PaymentHistoryRefunds FS is on" should {
      "send the user to the refund to tax payer page with data" in new Setup {
        enable(PaymentHistoryRefunds)
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())
        mockSingleBusinessIncomeSource()

        setupGetRepaymentHistoryByRepaymentId(testNino, repaymentRequestNumber)(testRepaymentHistoryModel)

        val result: Future[Result] = controller.show(repaymentRequestNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        //        result.futureValue.session.get(gatewayPage) shouldBe Some("paymentHistory")
      }

    }

    /*"Failing to retrieve a user's payments" should {
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
    }*/
  }

  "PaymentHistoryRefunds feature switch is disabled" should {
    "not obtaining a users repayments" should {
      "redirect the user to the home page" in new Setup {
        disable(PaymentHistoryRefunds)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = controller.show(repaymentRequestNumber)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }

  }


  "The RefundToTaxPayerController.showAgent function" when {

    "obtaining a users repayments when PaymentHistoryRefunds FS is on" should {
      "send the user to the refund to tax payer page with data" in new Setup {
        enable(PaymentHistoryRefunds)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        setupGetRepaymentHistoryByRepaymentId(testNino, repaymentRequestNumber)(testRepaymentHistoryModel)

        val result = controller.showAgent(repaymentRequestNumber)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
//        result.futureValue.session.get(gatewayPage) shouldBe Some("paymentHistory")
      }

    }

    "Failing to retrieve a user's payments - left" should {
      "send the user to the internal service error page" in new Setup {
        enable(PaymentHistoryRefunds)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()
        setupGetRepaymentHistoryByRepaymentId(testNino, repaymentRequestNumber)(testRepaymentHistoryModel)
        /*when(mockIncomeTaxViewChangeConnector.getRepaymentHistoryByRepaymentId(any(), matches(repaymentRequestNumber))(any()))
          .thenReturn(Future.successful(Left(PaymentHistoryError)))*/

        val result: Future[Result] = controller.showAgent(repaymentRequestNumber)(fakeRequestConfirmedClient())
        result.failed.futureValue shouldBe an[InternalServerException]

      }

    }

    "Failing to retrieve income sources" should {
      "send the user to internal server error page" in new Setup {
        enable(PaymentHistoryRefunds)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()

        val result: Future[Result] = controller.showAgent(repaymentRequestNumber)(fakeRequestConfirmedClient())
        result.failed.futureValue shouldBe an[InternalServerException]
      }
    }

    "User fails to be authorised" in new Setup {
      enable(PaymentHistoryRefunds)
      setupMockAgentAuthorisationException(withClientPredicate = false)

      val result: Future[Result] = controller.showAgent(repaymentRequestNumber)(fakeRequestWithActiveSession)

      status(result) shouldBe Status.SEE_OTHER
    }
  }

}
