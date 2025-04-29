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

package controllers.claimToAdjustPoa

import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoaAmendmentData
import play.api
import play.api.Application
import play.api.test.Helpers._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}

import scala.concurrent.Future

class AmendablePoaControllerSpec
  extends MockAuthActions
    with MockClaimToAdjustService
    with MockCalculationListService
    with MockPaymentOnAccountSessionService {

  val getMongoResponseJourneyIncomplete: Option[PoaAmendmentData] = Some(PoaAmendmentData())
  val getMongoResponseJourneyComplete: Option[PoaAmendmentData] = Some(PoaAmendmentData(None, None, journeyCompleted = true))

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[AmendablePoaController]

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
            "PoA tax year crystallized and no active session" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future(Right(None)))
              setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))
              setupMockGetPaymentOnAccountViewModel()
              setupMockTaxYearNotCrystallised()

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "PoA data is all fine, and we have an active session but is journey completed flag is false" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyComplete)))
              setupMockPaymentOnAccountSessionServiceCreateSession(Future(Right(())))
              setupMockGetPaymentOnAccountViewModel()
              setupMockTaxYearNotCrystallised()

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "PoA data is all fine, and we have an active session but is journey completed flag is true" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)

              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future(Right(getMongoResponseJourneyIncomplete)))
              setupMockGetPaymentOnAccountViewModel()
              setupMockTaxYearNotCrystallised()

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
          }
          s"redirect to the home page" when {
            "AdjustPaymentsOnAccount FS is disabled" in {
              disable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)

              mockSingleBISWithCurrentYearAsMigrationYear()

              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val expectedRedirectUrl = if (isAgent) {
                controllers.routes.HomeController.showAgent().url
              } else {
                controllers.routes.HomeController.show().url
              }
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          s"return status: $INTERNAL_SERVER_ERROR" when {
            "an Exception is returned from ClaimToAdjustService" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)

              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData()))))
              setupMockGetAmendablePoaViewModelFailure()

              val result = action(fakeRequest)
              result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
            }

            "Error creating mongo session" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)

              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
              setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Left(new Error("Error"))))
              setupMockGetPaymentOnAccountViewModel()
              setupMockTaxYearNotCrystallised()

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR

              verifyMockCreateSession(1)
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
    }
  }
}
