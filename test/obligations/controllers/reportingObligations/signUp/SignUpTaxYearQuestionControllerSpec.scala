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
import ITSAStatus.{Mandated, Voluntary}
import obligations.connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import obligations.mocks.services.MockSignUpService
import obligations.models.reportingObligations.signUp.SignUpTaxYearQuestionViewModel
import obligations.services.reportingObligations.optOut.MultiYearOptOutDefault
import obligations.services.reportingObligations.signUp.core.{CurrentSignUpTaxYear, SignUpProposition}
import obligations.services.reportingObligations.signUp.{SignUpService, SignUpSubmissionService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpTaxYearQuestionControllerSpec extends MockAuthActions with MockSignUpService {

  lazy val mockOptInUpdateService: SignUpSubmissionService = mock(classOf[SignUpSubmissionService])

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[SignUpService].toInstance(mockSignUpService),
        api.inject.bind[SignUpSubmissionService].toInstance(mockOptInUpdateService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[SignUpTaxYearQuestionController]

  val currentYear = Some("2025")
  val signUpTaxYear: CurrentSignUpTaxYear = CurrentSignUpTaxYear(ITSAStatus.Annual, TaxYear(2025, 2026))
  val optOutState = Some(MultiYearOptOutDefault)
  val viewModel = SignUpTaxYearQuestionViewModel(signUpTaxYear = signUpTaxYear)

  private def reportingObligationsLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/reporting-frequency")
    } else {
      Some("/report-quarterly/income-and-expenses/view/reporting-frequency")
    }
  }

  private def homeLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some(hub.controllers.routes.HomeController.showAgent().url)
    } else {
      Some(hub.controllers.routes.HomeController.show().url)
    }
  }

  private def confirmPageLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/sign-up/completed")
    } else {
      Some("/report-quarterly/income-and-expenses/view/sign-up/completed")
    }
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {

      s"the user is authenticated as a $mtdRole" should {

        s"render the Sign Up Tax Year Question page when returning a valid view model" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future(Some(viewModel)))
          mockFetchSavedChosenTaxYear(Some(signUpTaxYear.taxYear))
          mockFetchSavedSignUpSessionData()

          val result = action(fakeRequest)

          status(result) shouldBe OK
        }

        "redirect the user to the reporting obligations page when no view model is returned" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future(None))

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
        }
        "redirect the user to the home page when the sign up and opt in opt out feature switches are disabled" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe homeLink(isAgent)
        }

        "redirect the user to the home page when the sign up feature switch is disabled" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole, false, List())
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe homeLink(isAgent)
        }
      }
    }
    s"submit(isAgent = $isAgent)" when {

      s"the user is authenticated as a $mtdRole" should {

        "redirect the user when they select 'Yes' - to the completion page" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          val optInProposition: SignUpProposition =
            SignUpProposition.createSignUpProposition(
              currentYear = TaxYear(2025, 2026),
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockSignUpService.fetchSignUpProposition()(any(), any(), any()))
            .thenReturn(Future(optInProposition))

          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))

          when(mockOptInUpdateService.triggerSignUpRequest()(any(), any(), any()))
            .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

          mockIsSignUpTaxYearValid(Future(Some(viewModel)))

          val formData = Map("sign-up-tax-year-question" -> "Yes")

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe confirmPageLink(isAgent)
        }

        "redirect the user when they select 'Yes' - submit fails" in {

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockOptInUpdateService.triggerSignUpRequest()(any(), any(), any()))
            .thenReturn(Future(ITSAStatusUpdateResponseFailure.defaultFailure()))

          mockIsSignUpTaxYearValid(Future(Some(viewModel)))

          val formData = Map("sign-up-tax-year-question" -> "Yes")

          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(obligations.controllers.errors.routes.CannotUpdateReportingObligationsController.show(isAgent).url)
        }

        "redirect to the reporting obligations page when the user selects 'No'" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future(Some(viewModel)))

          val formData = Map(
            "sign-up-tax-year-question" -> "No"
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe reportingObligationsLink(isAgent)
        }

        "return an error when an invalid form response is submitted" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole, false, List(SignUpFs))
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future(Some(viewModel)))

          val formData = Map(
            "opt-out-tax-year-question" -> ""
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
