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

import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import controllers.routes
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.{ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.optin.MultiYearCheckYourAnswersViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.optIn.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends MockAuthActions
  with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[OptInCheckYourAnswersController]

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

          when(mockOptInService.getMultiYearCheckYourAnswersViewModel(any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(MultiYearCheckYourAnswersViewModel(
              taxYear2023,
              isAgent, routes.ReportingFrequencyPageController.show(isAgent).url,
              intentIsNextYear = true)
            )))

          val result = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        s"return result with $INTERNAL_SERVER_ERROR status" when {
          "there is no check your answers view model" in {
            enable(ReportingFrequencyPage, SignUpFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            when(mockOptInService.getMultiYearCheckYourAnswersViewModel(any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "render the reporting obligations page" when {
          "the sign up feature switch is disabled" in {
            disable(SignUpFs)
            enable(ReportingFrequencyPage)
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
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }

    s"submit(isAgent = $isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "redirect to OptInCompletedController" in {
          enable(ReportingFrequencyPage, SignUpFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockOptInService.makeOptInCall()(any(), any(), any()))
            .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess()))

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent).url)
        }

        s"redirect to optInError page" when {
          "the optInCall fails" in {
            enable(ReportingFrequencyPage, SignUpFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            when(mockOptInService.makeOptInCall()(any(), any(), any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

            val result = action(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
          }
        }

        "render the reporting obligations page" when {
          "the Sign Up feature switch is disabled" in {
            disable(SignUpFs)
            enable(ReportingFrequencyPage)
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