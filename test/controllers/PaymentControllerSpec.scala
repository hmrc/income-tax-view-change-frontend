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

import audit.models.InitiatePayNowAuditModel
import connectors.{BusinessDetailsConnector, ITSAStatusConnector, PayApiConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import models.core.{PaymentJourneyErrorResponse, PaymentJourneyModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api
import play.api.Application
import play.api.test.Helpers._
import services.DateServiceInterface
import testConstants.BaseTestConstants.{testCredId, testMtditid, testNino, testSaUtr}
import testConstants.PaymentDataTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class PaymentControllerSpec extends MockAuthActions {

  lazy val mockPayApiConnector: PayApiConnector = mock(classOf[PayApiConnector])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[PayApiConnector].toInstance(mockPayApiConnector),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[PaymentController]

  val paymentJourneyModel = PaymentJourneyModel("id", "redirect-url")

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.agentPaymentHandoff(testAmountInPence) else testController.paymentHandoff(testAmountInPence)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"${if (isAgent) "agentP" else "p"}aymentHandoff" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "redirect to the payment api provided by payAPI" when {
            "a successful payments journey is started" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPayApiConnector.startPaymentJourney(ArgumentMatchers.eq(testSaUtr), ArgumentMatchers.eq(BigDecimal(10000)),
                ArgumentMatchers.eq(isAgent))
              (ArgumentMatchers.any[HeaderCarrier])).thenReturn(Future.successful(paymentJourneyModel))
              val result = action(fakeRequest)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("redirect-url")
              verifyExtendedAudit(InitiatePayNowAuditModel(testMtditid, testNino,
                Some(testSaUtr), Some(testCredId),
                Some {
                  if (mtdUserRole == MTDIndividual) Individual else Agent
                }))
            }
          }

          "render the error page" when {
            if(mtdUserRole == MTDIndividual) {
              "an SA UTR is missing from the user" in {
                setupMockUserAuthNoSAUtr
                mockSingleBusinessIncomeSource()
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }
            "an error response is returned by the connector" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPayApiConnector.startPaymentJourney(ArgumentMatchers.eq(testSaUtr), ArgumentMatchers.eq(BigDecimal(10000)),
                ArgumentMatchers.eq(isAgent))
              (ArgumentMatchers.any[HeaderCarrier])).thenReturn(Future.successful(PaymentJourneyErrorResponse(INTERNAL_SERVER_ERROR, "Error Message")))
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "an exception is returned by the connector" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPayApiConnector.startPaymentJourney(ArgumentMatchers.eq(testSaUtr), ArgumentMatchers.eq(BigDecimal(10000)),
                ArgumentMatchers.eq(isAgent))
              (ArgumentMatchers.any[HeaderCarrier])).thenReturn(Future.failed(new Exception("Exception Message")))
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}
