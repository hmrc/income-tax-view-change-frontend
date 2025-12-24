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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import models.admin.PaymentHistoryRefunds
import models.financialDetails.{ChargeItem, Payment}
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.PaymentHistoryService.PaymentHistoryError
import services.{DateServiceInterface, PaymentHistoryService, RepaymentService}

import scala.concurrent.Future

class PaymentHistoryControllerSpec extends MockAuthActions
  with ImplicitDateFormatter {

  lazy val paymentHistoryService: PaymentHistoryService = mock(classOf[PaymentHistoryService])
  lazy val mockRepaymentService: RepaymentService = mock(classOf[RepaymentService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[PaymentHistoryService].toInstance(paymentHistoryService),
      api.inject.bind[RepaymentService].toInstance(mockRepaymentService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[PaymentHistoryController]


  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"), "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"), "2007-03-23", Some("DOCID02"))
  )

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgent() else testController.show()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the payment history page" when {
            "the user has payment history but no repayment history" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(paymentHistoryService.getPaymentHistory(any(), any()))
                .thenReturn(Future.successful(Right(testPayments)))
              when(paymentHistoryService.getRepaymentHistory(any())(any(), any()))
                .thenReturn(Future.successful(Right(List.empty[RepaymentHistory])))
              when(paymentHistoryService.getChargesWithUpdatedDocumentDateIfChargeHistoryExists()(any(), any()))
                .thenReturn(Future.successful(List.empty[ChargeItem]))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              result.futureValue.session.get(gatewayPage) shouldBe Some("paymentHistory")
            }
          }

          "render the error page" when {
            "payment history returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(paymentHistoryService.getPaymentHistory(any(), any()))
                .thenReturn(Future.successful(Right(testPayments)))

              when(paymentHistoryService.getRepaymentHistory(any())(any(), any()))
                .thenReturn(Future.successful(Left(RepaymentHistoryErrorModel)))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "repayment history returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(paymentHistoryService.getPaymentHistory(any(), any()))
                .thenReturn(Future.successful(Left(PaymentHistoryError)))

              when(paymentHistoryService.getRepaymentHistory(any())(any(), any()))
                .thenReturn(Future.successful(Right(List.empty[RepaymentHistory])))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }

  s"refundStatus" when {
    s"the is an authenticated Individual" should {
      "redirect to next url" when {
        "repayment service call is successful and PaymentHistoryRefunds Fs enabled" in {
          enable(PaymentHistoryRefunds)
          setupMockSuccess(MTDIndividual)
          mockItsaStatusRetrievalAction()
          mockSingleBISWithCurrentYearAsMigrationYear()

          when(mockRepaymentService.view(any())(any()))
            .thenReturn(Future.successful(Right("/test/url")))

          val result = testController.refundStatus(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some("/test/url")
        }
      }

      "render the custom not found view" when {
        "PaymentHistoryRefunds Fs is disabled" in {
          disable(PaymentHistoryRefunds)
          setupMockSuccess(MTDIndividual)
          mockItsaStatusRetrievalAction()
          mockSingleBISWithCurrentYearAsMigrationYear()
          val result = testController.refundStatus(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
          JsoupParse(result).toHtmlDocument.title() shouldBe s"There is a problem - Manage your Self Assessment - GOV.UK"
        }
      }

      "render the error page" when {
        "the repayment service returns an error" in {
          enable(PaymentHistoryRefunds)
          setupMockSuccess(MTDIndividual)
          mockItsaStatusRetrievalAction()
          mockSingleBISWithCurrentYearAsMigrationYear()

          when(mockRepaymentService.view(any())(any()))
            .thenReturn(Future.successful(Left(new Exception("ERROR"))))

          val result = testController.refundStatus(fakeRequestWithActiveSession)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
testMTDAuthFailuresForRole(testController.refundStatus, MTDIndividual)(fakeRequestWithActiveSession)
  }
}
