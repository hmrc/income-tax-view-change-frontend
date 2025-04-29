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

import controllers.agent.sessionUtils
import enums.{MTDIndividual, MTDSupportingAgent}
import generators.PoaGenerator
import mocks.auth.MockAuthActions
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PaymentOnAccountViewModel, PoaAmendmentData}
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants

import scala.concurrent.Future

class EnterPoaAmountControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with PoaGenerator {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[EnterPoaAmountController]


  val poaViewModelDecreaseJourney = PaymentOnAccountViewModel(
    previouslyAdjusted = None,
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000,
    totalAmountTwo = 5000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000,
    partiallyPaid = false,
    fullyPaid = false
  )

  val poaViewModelIncreaseJourney = PaymentOnAccountViewModel( //Increase OR Decrease journey
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    previouslyAdjusted = None,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 4000,
    totalAmountTwo = 4000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000,
    partiallyPaid = false,
    fullyPaid = false
  )

  def getPostRequest(isAgent: Boolean, mode: Mode, poaAmount: String) = {
    if (isAgent) {
      FakeRequest(POST, routes.EnterPoaAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession(
          sessionUtils.SessionKeys.clientFirstName -> "Test",
          sessionUtils.SessionKeys.clientLastName -> "User",
          sessionUtils.SessionKeys.clientUTR -> "1234567890",
          sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
          sessionUtils.SessionKeys.clientNino -> "AA111111A",
          sessionUtils.SessionKeys.confirmedClient -> "true"
        )
    }
    else {
      FakeRequest(POST, routes.EnterPoaAmountController.submit(false, mode).url)
        .withFormUrlEncodedBody("poa-amount" -> poaAmount)
        .withSession("nino" -> BaseTestConstants.testNino, "origin" -> "PTA")
    }
  }

  List(NormalMode, CheckMode).foreach { mode =>
    mtdAllRoles.foreach { mtdRole =>
      val isAgent = mtdRole != MTDIndividual
      s"show(isAgent = $isAgent, mode = $mode)" when {
        val action = testController.show(isAgent, mode)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          if (mtdRole == MTDSupportingAgent) {
            testSupportingAgentDeniedAccess(action)(fakeRequest)
          } else {
            s"render the POA amount page" when {
              if (mode == NormalMode) {
                "PoA tax year crystallized does not exist in session" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower))))))
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest)
                  status(result) shouldBe OK
                  Jsoup.parse(contentAsString(result)).select("#poa-amount").attr("value") shouldBe ""
                }
                "Empty PoA amount input when no existing PoA tax year in session" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower))))))
                  forAll(poaViewModelGen) { generatedPoaViewModel =>
                    setupMockGetPoaAmountViewModel(Right(generatedPoaViewModel))

                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest)
                    status(result) shouldBe OK
                    Jsoup.parse(contentAsString(result)).select("#poa-amount").attr("value") shouldBe ""
                  }
                }
              }
              "PoA tax year crystallized and newPoaAmount exists in session" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1111.22)))))))
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe OK
                Jsoup.parse(contentAsString(result)).select("#poa-amount").attr("value") shouldBe "1111.22"
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
                setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), None, journeyCompleted = true)))))
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
              }
            }
            "return an error 500" when {
              "Error retrieving mongo session" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future(Left(new Error(""))))
                setupMockGetPaymentOnAccountViewModel()

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
              "Retrieving mongo session fails" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future.failed(new Error("")))
                setupMockGetPaymentOnAccountViewModel()

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
              "User does not have an active mongo session" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future(Right(None)))
                setupMockGetPaymentOnAccountViewModel()

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
              "an Exception is returned from ClaimToAdjustService" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData()))))
                setupMockGetPoaAmountViewModelFailure()

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                result.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
      }

      s"submit(isAgent = $isAgent, mode = $mode)" when {
        val action = testController.submit(isAgent, mode)
        val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          if (mtdRole == MTDSupportingAgent) {
            testSupportingAgentDeniedAccess(action)(fakeRequest)
          } else {
            "redirect to the check your answers page" when {
              "The user is on the decrease only journey and form returned with no errors" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelDecreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1234.56"))
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url)
              }

              if (mode == NormalMode) {
                "The user is on the increase/decrease journey and chooses to increase" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                  when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "4500"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url)
                }

              } else if (mode == CheckMode) {

                "The user is on the increase/decrease journey and has come from CYA" when {
                  "They had previously decreased, and are now increasing" in {
                    enable(AdjustPaymentsOnAccount)
                    mockSingleBISWithCurrentYearAsMigrationYear()
                    setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                    when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100))))))
                    when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                    when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "4500"))
                    status(result) shouldBe SEE_OTHER
                    redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url)
                  }

                  "They had previously decreased, and are still decreasing" in {
                    enable(AdjustPaymentsOnAccount)
                    mockSingleBISWithCurrentYearAsMigrationYear()

                    setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                    when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100))))))
                    when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                    when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1000"))
                    status(result) shouldBe SEE_OTHER
                    redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url)
                  }
                }

                "They had previously increased, and are still increasing" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                  when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(4600))))))
                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                  when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "4500"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url)
                }
              }
            }

            "redirect to the Select Reason page" when {
              if (mode == NormalMode) {
                "The user is on the increase/decrease journey and chooses to decrease" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1234.56"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, mode).url)
                }
              } else {
                "They had previously increased, and are now decreasing" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                  when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(Some(Increase), Some(4500))))))
                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                  when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1234.56"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, mode).url)
                }
              }
            }
            "redirect back to the Enter PoA Amount page with a 500 response" when {
              "No PoA Amount is input" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> ""))
                status(result) shouldBe BAD_REQUEST
                redirectLocation(result) shouldBe None
              }
              "Fails when view model is a negative amount" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockPaymentOnAccountSessionService(Future(Right(Some(PoaAmendmentData(Some(MainIncomeLower))))))
                forAll(poaViewModelGen) { generatedPoaViewModel =>
                  setupMockGetPoaAmountViewModel(Right(generatedPoaViewModel))
                  setupMockSuccess(mtdRole)

                  val inputAmount = -100
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> inputAmount.toString))
                  status(result) shouldBe BAD_REQUEST
                  redirectLocation(result) shouldBe None
                }
              }
              "Fails when input amount exceeds relevant amount from view model" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                forAll(poaViewModelGen) { generatedPoaViewModel =>
                  setupMockGetPoaAmountViewModel(Right(generatedPoaViewModel))
                  setupMockSuccess(mtdRole)

                  val inputAmount = generatedPoaViewModel.relevantAmountOne + 100
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> inputAmount.toString))
                  status(result) shouldBe BAD_REQUEST
                  redirectLocation(result) shouldBe None
                }
              }
              "Input PoA Amount is not a valid number" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "test"))
                status(result) shouldBe BAD_REQUEST
                redirectLocation(result) shouldBe None
              }
              "Input PoA Amount is higher than relevant amount" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "6000"))
                status(result) shouldBe BAD_REQUEST
                redirectLocation(result) shouldBe None
              }
              "Input PoA Amount is equal to previous PoA amount" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "4000"))
                status(result) shouldBe BAD_REQUEST
                redirectLocation(result) shouldBe None
              }

              "Error setting new poa amount in mongo" in {
                enable(AdjustPaymentsOnAccount)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaAmountViewModel(Right(poaViewModelDecreaseJourney))
                when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Left(new Error("Error setting poa amount"))))

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1234.56"))
                status(result) shouldBe INTERNAL_SERVER_ERROR
                redirectLocation(result) shouldBe None
              }

              if (mode == NormalMode) {
                "Error setting adjustment reason in mongo" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))
                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                  when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Left(new Error("Error setting adjustment reason"))))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "4500"))
                  status(result) shouldBe INTERNAL_SERVER_ERROR
                  redirectLocation(result) shouldBe None
                }
              } else {
                "Error getting adjustment reason from mongo" in {
                  enable(AdjustPaymentsOnAccount)
                  mockSingleBISWithCurrentYearAsMigrationYear()
                  setupMockGetPoaAmountViewModel(Right(poaViewModelIncreaseJourney))

                  when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Left(new Error("Error getting mongo data"))))
                  when(mockPaymentOnAccountSessionService.setNewPoaAmount(any())(any(), any())).thenReturn(Future(Right(())))
                  when(mockPaymentOnAccountSessionService.setAdjustmentReason(any())(any(), any())).thenReturn(Future(Right(())))

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("poa-amount" -> "1000"))
                  status(result) shouldBe INTERNAL_SERVER_ERROR
                  redirectLocation(result) shouldBe None
                }
              }
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
      }
    }
  }
}
