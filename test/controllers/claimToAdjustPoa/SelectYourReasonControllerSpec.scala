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
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa._
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.testPoa1Maybe

import scala.concurrent.Future

class SelectYourReasonControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockPaymentOnAccountSessionService
  with MockCalculationListService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[SelectYourReasonController]

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

  val poaTotalLessThanRelevant: Option[PaymentOnAccountViewModel] = poa.map(_.copy(
    totalAmountOne = 1000.0,
    totalAmountTwo = 1000.0))

  def setupTest(sessionResponse: Either[Throwable, Option[PoaAmendmentData]], claimToAdjustResponse: Option[PaymentOnAccountViewModel]): Unit = {
    enable(AdjustPaymentsOnAccount)
    mockSingleBISWithCurrentYearAsMigrationYear()
    setupMockGetPaymentsOnAccount(claimToAdjustResponse)
    setupMockTaxYearCrystallised()
    setupMockPaymentOnAccountSessionService(Future.successful(sessionResponse))
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
            s"render the Select your reason page" that {
              if (mode == CheckMode) {
                "has pre-populated the answer with AllowanceOrReliefHigher" when {
                  "POA tax year crystallized and the user previously selected the AllowanceOrReliefHigher checkbox" in {
                    setupTest(
                      sessionResponse = Right(Some(
                        PoaAmendmentData(newPoaAmount = Some(200.0), poaAdjustmentReason = Some(AllowanceOrReliefHigher))
                      )),
                      claimToAdjustResponse = testPoa1Maybe
                    )

                    setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(AllowanceOrReliefHigher)

                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest)
                    status(result) shouldBe OK
                    Jsoup.parse(contentAsString(result)).select("#value-3[checked]").toArray should have length 1
                  }
                }
              } else {
                "does not have a pre-populated answer" when {
                  "session data has no poaAdjustmentReason data" in {
                    setupTest(
                      sessionResponse = Right(Some(PoaAmendmentData())),
                      claimToAdjustResponse = testPoa1Maybe)

                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest)
                    status(result) shouldBe OK
                    Jsoup.parse(contentAsString(result)).select("#value-3[checked]").toArray should have length 0
                  }
                }

                "has pre-populated answer" when {
                  "session data has valid poaAdjustmentReason data" in {
                    setupTest(
                      sessionResponse = Right(Some(PoaAmendmentData(poaAdjustmentReason = Some(MoreTaxedAtSource)))),
                      claimToAdjustResponse = testPoa1Maybe)
                    setupMockGetSessionDataSuccess()
                    setupMockGetClientDetailsSuccess()
                    setupMockSuccess(mtdRole)
                    val result = action(fakeRequest)
                    status(result) shouldBe OK
                    Jsoup.parse(contentAsString(result)).select("#value-4[checked]").toArray should have length 1
                  }
                }
              }
            }

            "set Reason and redirect to check your answers" when {
              "new amount is greater than current amount" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData(newPoaAmount = Some(20000.0)))),
                  claimToAdjustResponse = testPoa1Maybe)

                setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.show(isAgent).url)
              }
            }

            s"redirect to the home page" when {
              "Adjust Payments On Account FS is Disabled" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData(newPoaAmount = Some(20000.0)))),
                  claimToAdjustResponse = testPoa1Maybe)

                disable(AdjustPaymentsOnAccount)

                setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

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
            "redirect to the You cannot go back page" when {
              "Adjust Payments On Account FS is Enabled but journeyCompleted flag is true" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData(None, newPoaAmount = Some(20000.0), journeyCompleted = true))),
                  claimToAdjustResponse = testPoa1Maybe)

                setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(Increase)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
              }
            }
            s"return status: $INTERNAL_SERVER_ERROR" when {
              "Payment On Account Session data is missing" in {
                setupTest(
                  sessionResponse = Right(None),
                  claimToAdjustResponse = testPoa1Maybe)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

              "POA data is missing" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData())),
                  claimToAdjustResponse = None)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

              "Something goes wrong in payment on account session Service" in {
                setupTest(
                  sessionResponse = Left(new Exception("Something went wrong")),
                  claimToAdjustResponse = testPoa1Maybe)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
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
            s"redirect to check your answers page" when {
              if (mode == CheckMode) {
                "'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {
                  setupTest(
                    sessionResponse = Right(Some(PoaAmendmentData())),
                    claimToAdjustResponse = testPoa1Maybe
                  )

                  setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("value" -> "MainIncomeLower"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.show(isAgent).url)
                }
              }

              "'totalAmount' is less than 'poaRelevantAmount'" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData(newPoaAmount = Some(5000.0)))),
                  claimToAdjustResponse = poaTotalLessThanRelevant
                )

                setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest.withFormUrlEncodedBody("value" -> "MainIncomeLower"))
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.show(isAgent).url)
              }
            }
            if (mode == NormalMode) {
              "redirect to enter POA amount" when {
                "'totalAmount' is equal to or greater than 'poaRelevantAmount'" in {

                  setupTest(
                    sessionResponse = Right(Some(PoaAmendmentData())),
                    claimToAdjustResponse = testPoa1Maybe)

                  setupMockPaymentOnAccountSessionServiceSetAdjustmentReason(MainIncomeLower)

                  setupMockSuccess(mtdRole)
                  val result = action(fakeRequest.withFormUrlEncodedBody("value" -> "MainIncomeLower"))
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(routes.EnterPoaAmountController.show(isAgent, NormalMode).url)
                }
              }
            }

            s"return $BAD_REQUEST" when {
              s"no option is selected" in {
                setupTest(
                  sessionResponse = Right(Some(PoaAmendmentData())),
                  claimToAdjustResponse = testPoa1Maybe)

                setupMockSuccess(mtdRole)
                val result = action(fakeRequest)
                status(result) shouldBe BAD_REQUEST
              }
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole, false)(fakeRequest)
      }
    }
  }
}