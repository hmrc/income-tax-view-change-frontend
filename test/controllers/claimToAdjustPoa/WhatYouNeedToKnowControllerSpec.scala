/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.claimToAdjustPoa

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.PoaAmendmentData
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, DateServiceInterface, PaymentOnAccountSessionService}

import scala.concurrent.Future

class WhatYouNeedToKnowControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockPaymentOnAccountSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[WhatYouNeedToKnowController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the Amendable POA page" when {
            "PaymentOnAccount model is returned successfully with PoA tax year crystallized and relevantAmount = totalAmount" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              setupMockGetPaymentsOnAccount()
              setupMockTaxYearNotCrystallised()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
            "PaymentOnAccount model is returned successfully with PoA tax year crystallized and relevantAmount > totalAmount" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              setupMockGetPaymentsOnAccount(Some(previouslyReducedPaymentOnAccountModel))
              setupMockTaxYearNotCrystallised()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
          }

          "redirect to the You Cannot Go Back page" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
            mockItsaStatusRetrievalAction()
              setupMockGetPaymentsOnAccount()
              setupMockTaxYearNotCrystallised()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
          }
          "return an error 500" when {
            "PaymentOnAccount model is not built successfully" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
              setupMockGetPaymentsOnAccountBuildFailure()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "an Exception is returned from ClaimToAdjustService" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
              setupMockGetAmendablePoaViewModelFailure()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }
  }
}
