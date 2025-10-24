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

package controllers.optIn.oldJourney

import enums.MTDIndividual
import forms.optIn.ChooseTaxYearForm
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.{SignUpFs, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.optIn.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class ChooseYearControllerSpec extends MockAuthActions
  with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[ChooseYearController]

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "render the check your answers page" in {
          enable(ReportingFrequencyPage, SignUpFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
          mockFetchSavedChosenTaxYear(Some(taxYear2023))

          val result = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        "render the reporting obligations page when the sign up feature switch is disabled" in {
          enable(ReportingFrequencyPage)
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

        "render the home page when the feature switch is disabled" in {
          disable(ReportingFrequencyPage)
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

    s"submit(isAgent = $isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "redirect to Check Your Answers" in {
          enable(ReportingFrequencyPage, SignUpFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
          mockSaveIntent(taxYear2023)
          val result = action(fakeRequest.withFormUrlEncodedBody(
            ChooseTaxYearForm.choiceField -> taxYear2023.toString
          ))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optIn.oldJourney.routes.OptInCheckYourAnswersController.show(isAgent).url)
        }

        "return a BadRequest" when {
          "the form is invalid" in {
            enable(ReportingFrequencyPage, SignUpFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
            mockSaveIntent(taxYear2023)
            val result = action(fakeRequest.withFormUrlEncodedBody(
              ChooseTaxYearForm.choiceField -> ""
            ))

            status(result) shouldBe Status.BAD_REQUEST
          }
        }

        s"render the error page" when {
          "failed save intent" in {
            enable(ReportingFrequencyPage, SignUpFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockAvailableOptInTaxYear(List(taxYear2023, taxYear2023.nextYear))
            mockSaveIntent(taxYear2023, isSuccessful = false)

            val result = action(fakeRequest.withFormUrlEncodedBody(
              ChooseTaxYearForm.choiceField -> taxYear2023.toString
            ))
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "render the reporting obligations page" when {
          "the Sign Up feature switch is disabled" in {
            enable(ReportingFrequencyPage)
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
          "the ReportingFrequencyPage feature switch is disabled" in {
            disable(ReportingFrequencyPage)
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