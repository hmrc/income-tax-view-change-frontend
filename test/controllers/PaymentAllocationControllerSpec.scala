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
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockPaymentAllocationsService
import models.core.Nino
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, PaymentAllocationError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{DateServiceInterface, PaymentAllocationsService}
import testConstants.PaymentAllocationsTestConstants._

import scala.concurrent.Future

class PaymentAllocationControllerSpec extends MockAuthActions with ImplicitDateFormatter with MockPaymentAllocationsService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[PaymentAllocationsService].toInstance(mockPaymentAllocationsService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[PaymentAllocationsController]

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  val docNumber = "docNumber1"

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.viewPaymentAllocationAgent(docNumber) else testController.viewPaymentAllocation(docNumber)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the payment allocation page" when {
            "the user has payment allocations" in {
              val successfulResponse = Right(paymentAllocationViewModel)
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
                .thenReturn(Future.successful(successfulResponse))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
            "the user has late payment charges" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
                .thenReturn(Future.successful(Right(paymentAllocationViewModelLpi)))
              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }

            "the user has no late payment charges (HMRC adjustment)" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
                .thenReturn(Future.successful(Right(paymentAllocationViewModelHmrcAdjustment)))
              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "render the error page" when {
            "retrieving the users payment allocation fails" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()
              when(mockPaymentAllocationsService.getPaymentAllocation(Nino(any()), any())(any(), any()))
                .thenReturn(Future.successful(Left(PaymentAllocationError())))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
       testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}
