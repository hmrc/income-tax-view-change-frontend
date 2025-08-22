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

import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.PoaAmendmentData
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.testPoa1Maybe

import scala.concurrent.Future

class YouCannotGoBackControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[YouCannotGoBackController]

  def setupTest(): Unit = {
    mockSingleBISWithCurrentYearAsMigrationYear()
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the You cannot go back page" when {
            "AdjustPaymentsOnAccount FS is enabled and journeyComplete is true" in {
              setupTest()
              setupMockSuccess(mtdRole)

              setupMockGetPaymentsOnAccount(testPoa1Maybe)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "AdjustPaymentsOnAccount FS is enabled and journeyComplete is false" in {
              setupTest()

              setupMockGetPaymentsOnAccount(testPoa1Maybe)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK

            }
          }

          s"return status $INTERNAL_SERVER_ERROR" when {

            "No POAs can be found" in {
              setupTest()

              setupMockGetPaymentsOnAccount(None)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR

            }

            "No active session can be found" in {
              setupTest()

              setupMockGetPaymentsOnAccount(testPoa1Maybe)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR

            }

            "Call to mongo fails" in {
              setupTest()

              setupMockGetPaymentsOnAccount(testPoa1Maybe)
              setupMockPaymentOnAccountSessionService(Future.failed(new Error("")))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR

            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }
  }
}
