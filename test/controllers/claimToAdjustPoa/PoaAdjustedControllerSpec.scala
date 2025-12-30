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
import mocks.services.{MockCalculationListService, MockClaimToAdjustService, MockDateService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.PoaAmendmentData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.{ClaimToAdjustService, DateService, DateServiceInterface, PaymentOnAccountSessionService}

import java.time.LocalDate
import scala.concurrent.Future

class PoaAdjustedControllerSpec extends MockAuthActions
  with MockClaimToAdjustService
  with MockCalculationListService
  with MockPaymentOnAccountSessionService
  with MockDateService {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
        api.inject.bind[PaymentOnAccountSessionService].toInstance(mockPaymentOnAccountSessionService),
        api.inject.bind[DateService].toInstance(mockDateService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[PoaAdjustedController]

  val startOfTaxYear: LocalDate = LocalDate.of(2023, 4, 7)
  val endOfTaxYear: LocalDate = LocalDate.of(2024, 4, 4)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the POA Adjusted page" when {
            "PaymentOnAccount model is returned successfully with PoA tax year crystallized" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              when(mockDateService.getCurrentDate).thenReturn(startOfTaxYear)
              when(mockPaymentOnAccountSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
              when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))

              setupMockGetPaymentsOnAccount()
              setupMockTaxYearNotCrystallised()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
            "journeyCompleted flag is set to true" in {

              mockSingleBISWithCurrentYearAsMigrationYear()
              mockItsaStatusRetrievalAction()
              when(mockDateService.getCurrentDate).thenReturn(endOfTaxYear)
              when(mockPaymentOnAccountSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Right(())))
              when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(None, newPoaAmount = Some(5000), journeyCompleted = true)))))

              setupMockGetPaymentsOnAccount()
              setupMockTaxYearNotCrystallised()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
          }

          "return an error 500" when {

            "Error setting journey completed flag in mongo session" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))
              when(mockPaymentOnAccountSessionService.setCompletedJourney(any(), any())).thenReturn(Future(Left(new Error(""))))

              setupMockGetPaymentsOnAccount()
              setupMockTaxYearNotCrystallised()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "PaymentOnAccount model is not built successfully" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))
              setupMockGetPaymentsOnAccountBuildFailure()

              setupMockSuccess(mtdRole)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "an Exception is returned from ClaimToAdjustService" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockPaymentOnAccountSessionService.getMongo(any(), any())).thenReturn(Future(Right(Some(PoaAmendmentData(newPoaAmount = Some(1200))))))
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
