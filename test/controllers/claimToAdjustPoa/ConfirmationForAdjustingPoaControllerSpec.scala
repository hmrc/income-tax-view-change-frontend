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
import mocks.services._
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{MainIncomeLower, PoaAmendmentData}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.claimToAdjustPoa.ClaimToAdjustPoaCalculationService
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}

import scala.concurrent.Future

class ConfirmationForAdjustingPoaControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockClaimToAdjustPoaCalculationService {

  val poa: PoaAmendmentData = PoaAmendmentData(
    None,
    Some(20.0)
  )

  val validSession: PoaAmendmentData = PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val emptySession: PoaAmendmentData = PoaAmendmentData(None, None)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[ClaimToAdjustPoaCalculationService].toInstance(mockClaimToAdjustPoaCalculationService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[ConfirmationForAdjustingPoaController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the conformation for adjusting POA page" when {
            "PoA tax year crystallized" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))
              setupMockGetPaymentsOnAccount()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
          }
          "redirect to the home page" when {
            "FS is disabled" in {
              disable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockSuccess(mtdRole)
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
          "redirect to the You Cannot Go Back page" when {
            "FS is enabled and the journeyCompleted flag is set to true in session" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
              setupMockGetPaymentsOnAccount()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
            }
          }
          "return an error 500" when {
            "Payment On Account Session is missing" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(None)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "Payment On Account data is missing from session" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(emptySession))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "an Exception is returned from ClaimToAdjustService" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetAmendablePoaViewModelFailure()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
    }

    s"submit(isAgent = $isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "redirect to PoaAdjustedController page" when {
            "data to API 1773 successfully sent" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount()
              setupMockRecalculateSuccess()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent = isAgent).url)
            }
          }

          "redirect to the home page" when {
            "FS is disabled" in {
              disable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockSuccess(mtdRole)
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
          "redirect to API error page" when {
            "data to API 1773 failed to be sent" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount()
              setupMockRecalculateFailure()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent).url)
            }
          }
          "redirect an error 500" when {
            "Payment On Account Session data is missing" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(None)

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "an Exception is returned from ClaimToAdjustService" in {
              enable(AdjustPaymentsOnAccount)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccountBuildFailure()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
    }
  }
}
