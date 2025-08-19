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
import mocks.services.{MockClaimToAdjustPoaCalculationService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PaymentOnAccountViewModel, PoaAmendmentData}
import models.incomeSourceDetails.TaxYear
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.claimToAdjustPoa.ClaimToAdjustPoaCalculationService
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockClaimToAdjustPoaCalculationService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[ClaimToAdjustPoaCalculationService].toInstance(mockClaimToAdjustPoaCalculationService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[CheckYourAnswersController]

  val poa: Option[PaymentOnAccountViewModel] = Some(
    PaymentOnAccountViewModel(
      poaOneTransactionId = "poaOne-Id",
      poaTwoTransactionId = "poaTwo-Id",
      taxYear = TaxYear.makeTaxYearWithEndYear(2024),
      totalAmountOne = 5000.00,
      totalAmountTwo = 5000.00,
      relevantAmountOne = 5000.00,
      relevantAmountTwo = 5000.00,
      partiallyPaid = false,
      fullyPaid = false,
      previouslyAdjusted = None
    ))

  val emptySession: PoaAmendmentData = PoaAmendmentData(None, None)
  val validSession: PoaAmendmentData = PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val validSessionIncrease: PoaAmendmentData = PoaAmendmentData(Some(Increase), Some(BigDecimal(1000.00)))

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the check your answers page" when {
            "PoA tax year crystallized and the session contains the new POA Amount and reason" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
              contentAsString(result).contains("Confirm and continue") shouldBe true
            }

            "PoA tax year crystallized and the session contains the new POA Amount and reason, but the reason is INCREASE" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSessionIncrease))))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
              contentAsString(result).contains("Confirm and save") shouldBe true
            }
          }

          s"return status $SEE_OTHER and redirect to the You Cannot Go Back page" when {
            "the journeyCompleted flag is set to true in session" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
            }
          }

          s"return status: $INTERNAL_SERVER_ERROR" when {
            "Payment On Account Session is missing" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "Payment On Account data is missing from session" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(emptySession))))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "POA data is missing" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(None)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "POA adjustment reason is missing from the session" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(None)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession.copy(poaAdjustmentReason = None)))))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "the new POA amount is missing from the session" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(None)
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession.copy(newPoaAmount = None)))))
              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "Something goes wrong in payment on account session Service" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Something went wrong"))))
              setupMockSuccess(mtdRole)

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "Failed future returned when retrieving mongo data" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPaymentsOnAccount(poa)
              setupMockPaymentOnAccountSessionService(Future.failed(new Error("Error getting mongo session")))

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }

    s"submit(isAgent = $isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"redirect to POA adjusted page" when {
            "data to API 1773 successfully sent" in {
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockGetPaymentsOnAccount()
              setupMockRecalculateSuccess()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              val result = action(fakeRequest)
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent).url)
            }
          }
          "redirect to API error page" when {
            "data to API 1773 failed to be sent" in {
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockGetPaymentsOnAccount()
              setupMockRecalculateFailure()
              setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(validSession))))

              val result = action(fakeRequest)
              redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent).url)
            }
          }

          "redirect an error 500" when {
            "Payment On Account Session data is missing" in {
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockGetPaymentsOnAccount(None)

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "an Exception is returned from ClaimToAdjustService" in {
              setupMockSuccess(mtdRole)
              mockSingleBISWithCurrentYearAsMigrationYear()

              setupMockGetPaymentsOnAccountBuildFailure()

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
