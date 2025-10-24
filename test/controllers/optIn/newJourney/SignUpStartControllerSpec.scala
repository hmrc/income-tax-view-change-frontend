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

import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.{OptInOptOutContentUpdateR17, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Voluntary
import models.optin.newJourney.SignUpTaxYearQuestionViewModel
import play.api
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optIn.core.{CurrentOptInTaxYear, OptInTaxYear}
import services.optIn.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpStartControllerSpec extends MockAuthActions with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[SignUpStartController]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent, Some("2025"))
      s"the user is authenticated as a $mtdRole" should {
        "render the sign up start page" in {
          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsSignUpTaxYearValid(Future.successful(Some(SignUpTaxYearQuestionViewModel(CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))))))

          mockSaveIntent(TaxYear(2025, 2026))
          mockFetchSavedChosenTaxYear(Some(TaxYear(2025, 2026)))

          val result = action(fakeRequest)
          status(result) shouldBe OK
        }

        "be redirected to the reporting frequency page if the chosen tax year intent is not found" in {
          enable(ReportingFrequencyPage, SignUpFs, OptInOptOutContentUpdateR17)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsSignUpTaxYearValid(Future.successful(None))

          mockSaveIntent(TaxYear(2025, 2026))
          mockFetchSavedChosenTaxYear(None)

          val result = action(fakeRequest)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.ReportingFrequencyPageController.show(isAgent).url)
        }

        "be redirected to the home page if the feature switch is disabled" in {
          disable(ReportingFrequencyPage)
          disable(SignUpFs)
          enable(OptInOptOutContentUpdateR17)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsSignUpTaxYearValid(Future.successful(Some(SignUpTaxYearQuestionViewModel(CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))))))

          val redirectUrl = if (isAgent) {
            controllers.routes.HomeController.showAgent().url
          } else {
            controllers.routes.HomeController.show().url
          }

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }

        "be redirected to the reporting frequency page if the OptInOptOutContentUpdateR17 feature switch is disabled" in {
          enable(ReportingFrequencyPage)
          disable(OptInOptOutContentUpdateR17)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsSignUpTaxYearValid(Future.successful(Some(SignUpTaxYearQuestionViewModel(CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))))))

          val result = action(fakeRequest)

          val redirectUrl = if (isAgent) {
            controllers.routes.ReportingFrequencyPageController.show(true).url
          } else {
            controllers.routes.ReportingFrequencyPageController.show(false).url
          }

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }

        "be redirected to the reporting frequency page if the Sign Up feature switch is disabled" in {
          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17)
          disable(SignUpFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsSignUpTaxYearValid(Future.successful(Some(SignUpTaxYearQuestionViewModel(CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))))))

          val result = action(fakeRequest)

          val redirectUrl = if (isAgent) {
            controllers.routes.ReportingFrequencyPageController.show(true).url
          } else {
            controllers.routes.ReportingFrequencyPageController.show(false).url
          }

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
    }
  }
}
