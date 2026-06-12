/*
 * Copyright 2025 HM Revenue & Customs
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

package obligations.controllers.reportingObligations.signUp

import common.connectors.ITSAStatusConnector
import common.enums.MTDIndividual
import common.mocks.auth.MockAuthActions
import common.models.admin.SignUpFs
import common.models.itsaStatus.ITSAStatus
import common.services.DateServiceInterface
import models.incomeSourceDetails.TaxYear
import obligations.mocks.services.MockSignUpService
import obligations.services.reportingObligations.signUp.SignUpService
import obligations.services.reportingObligations.signUp.core.SignUpProposition.createSignUpProposition
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import common.testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpCompletedControllerSpec extends MockAuthActions with MockSignUpService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SignUpService].toInstance(mockSignUpService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[SignUpCompletedController]

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        s"render the signUpCompleted page" that {
          "is for the current year" in {
            setupMockSuccess(mtdRole, false, List(SignUpFs))
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createSignUpProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchSignUpProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            when(mockSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
          "is for next year" in {
            setupMockSuccess(mtdRole, false, List(SignUpFs))
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createSignUpProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchSignUpProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023.nextYear))

            when(mockSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
        }
        "render the error page" when {
          "no proposition returned" in {
            setupMockSuccess(mtdRole, false, List(SignUpFs))
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockFetchSignUpProposition(None)
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            val result = action(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
          "FetchSavedChosenTaxYear fails" in {
            setupMockSuccess(mtdRole, false, List(SignUpFs))
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockFetchSignUpProposition(Some(createSignUpProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)))
            mockFetchSavedChosenTaxYear(None)

            val result = action(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
        "redirect to the home page" when {
          "the sign up feature switch is disabled" in {
            setupMockSuccess(mtdRole, false, List())
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              hub.controllers.routes.HomeController.showAgent().url
            } else {
              hub.controllers.routes.HomeController.show().url
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        "redirect to the home page" when {
          "the opt in opt out content R17 feature switch is disabled" in {
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              hub.controllers.routes.HomeController.showAgent().url
            } else {
              hub.controllers.routes.HomeController.show().url
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
