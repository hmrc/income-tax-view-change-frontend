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
import models.itsaStatus.ITSAStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optIn.OptInService
import services.optIn.core.OptInProposition.createOptInProposition
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpCompletedControllerSpec extends MockAuthActions with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService),
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
            enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchOptInProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            when(mockOptInService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
          "is for next year" in {
            enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchOptInProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023.nextYear))

            when(mockOptInService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
              .thenReturn(Future.successful(true))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
        }
        "render the error page" when {
          "no proposition returned" in {
            enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockFetchOptInProposition(None)
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            val result = action(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
          "FetchSavedChosenTaxYear fails" in {
            enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockFetchOptInProposition(Some(createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)))
            mockFetchSavedChosenTaxYear(None)

            val result = action(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
        "render the reporting obligations page" when {
          "the sign up feature switch is disabled" in {
            enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17)
            disable(SignUpFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
            } else {
              "/report-quarterly/income-and-expenses/view/reporting-frequency"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        "render the home page" when {
          "the reporting frequency and opt in opt out content R17 feature switches are disabled" in {
            disable(ReportingFrequencyPage)
            disable(OptInOptOutContentUpdateR17)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
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
