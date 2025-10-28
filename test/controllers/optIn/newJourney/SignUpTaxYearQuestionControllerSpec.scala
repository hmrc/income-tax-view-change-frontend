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

package controllers.optIn.newJourney

import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.{OptInOptOutContentUpdateR17, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optin.OptInSessionData
import models.optin.newJourney.SignUpTaxYearQuestionViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optIn.OptInService
import services.optIn.core.CurrentOptInTaxYear
import services.optout._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpTaxYearQuestionControllerSpec extends MockAuthActions with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[SignUpTaxYearQuestionController]
  val currentYear = Some("2025")

  val signUpTaxYear: CurrentOptInTaxYear = CurrentOptInTaxYear(ITSAStatus.Annual, TaxYear(2025, 2026))
  val optOutState = Some(MultiYearOptOutDefault)
  val viewModel = SignUpTaxYearQuestionViewModel(
    signUpTaxYear = signUpTaxYear
  )

  private def reportingObligationsLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/reporting-frequency")
    } else {
      Some("/report-quarterly/income-and-expenses/view/reporting-frequency")
    }
  }

  private def homeLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
    } else {
      Some("/report-quarterly/income-and-expenses/view")
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

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))
          mockFetchSavedChosenTaxYear(Some(signUpTaxYear.taxYear))
          mockFetchSavedOptInSessionData()

          val result = action(fakeRequest)

          status(result) shouldBe OK
        }

        "redirect the user to the reporting obligations page when no view model is returned" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockIsSignUpTaxYearValid(Future.successful(None))
          mockFetchSavedOptInSessionData()

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
        }
        "redirect the user to the home page when the feature switch is disabled" in {
          disable(OptInOptOutContentUpdateR17)
          disable(ReportingFrequencyPage)
          disable(SignUpFs)

          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole)
          mockFetchSavedOptInSessionData()
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe homeLink(isAgent)
        }

        "redirect the user to the reporting obligations page when the sign up feature switch is disabled" in {
          enable(ReportingFrequencyPage)
          enable(OptInOptOutContentUpdateR17)
          disable(SignUpFs)

          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe reportingObligationsLink(isAgent)
        }
      }
    }
    s"submit(isAgent = $isAgent)" when {
      s"the user is authenticated as a $mtdRole" should {
        "redirect the user when they select 'Yes' - to the completion page" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockOptInService.makeOptInCall()(any(), any(), any()))
            .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess()))

          mockFetchSavedOptInSessionData()
          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))

          val formData = Map(
            "sign-up-tax-year-question" -> "Yes",
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe confirmPageLink(isAgent)
        }

        "redirect the user when they select 'Yes' - submit fails" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockOptInService.makeOptInCall()(any(), any(), any()))
            .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure(List())))

          mockFetchSavedOptInSessionData()
          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))

          val formData = Map(
            "sign-up-tax-year-question" -> "Yes",
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optIn.oldJourney.routes.OptInErrorController.show(isAgent).url)
        }

        "redirect to the reporting obligations page when the user selects 'No'" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockFetchSavedOptInSessionData()
          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))

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

          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          mockFetchSavedOptInSessionData()
          mockIsSignUpTaxYearValid(Future.successful(Some(viewModel)))

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
