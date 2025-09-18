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
import mocks.services.{MockDateService, MockOptInService}
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.TaxYear
import models.optin.ConfirmTaxYearViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.DateService
import services.optIn.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class ConfirmTaxYearControllerSpec extends MockAuthActions
  with MockOptInService with MockDateService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService),
      api.inject.bind[DateService].toInstance(mockDateService)
    ).build()

  lazy val testController = app.injector.instanceOf[ConfirmTaxYearController]

  val endCurrentTaxYear = 2025
  val taxYear2024_25: TaxYear = TaxYear.forYearEnd(endCurrentTaxYear)

  val endTaxYear = 2026
  val taxYear2025_26: TaxYear = TaxYear.forYearEnd(endCurrentTaxYear)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "render the confirm tax year page for current tax year" in {
          enable(ReportingFrequencyPage)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockOptInService.getConfirmTaxYearViewModel(any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ConfirmTaxYearViewModel(
              taxYear2024_25, routes.ReportingFrequencyPageController.show(isAgent).url, isNextTaxYear = false,
              isAgent)
            )))

          val result = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        "render the confirm tax year page for next tax year" in {
          enable(ReportingFrequencyPage)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          when(mockOptInService.getConfirmTaxYearViewModel(any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ConfirmTaxYearViewModel(
              taxYear2025_26, routes.ReportingFrequencyPageController.show(isAgent).url, isNextTaxYear = true,
              isAgent)
            )))

          val result = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        s"return result with $INTERNAL_SERVER_ERROR status" when {
          "getConfirmTaxYearViewModel fails" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            when(mockOptInService.getConfirmTaxYearViewModel(any())(any(), any(), any()))
              .thenReturn(Future.successful(None))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
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
          enable(ReportingFrequencyPage)
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
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            when(mockOptInService.makeOptInCall()(any(), any(), any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

            val result = action(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
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