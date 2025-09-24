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

package controllers.optOut.oldJourney

import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import controllers.optOut.routes
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockOptOutService, MockSessionService}
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.{MultiYearOptOutCheckpointViewModel, OneYearOptOutCheckpointViewModel, OptOutCheckpointViewModel}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import services.optout.{CurrentOptOutTaxYear, OneYearOptOutFollowedByAnnual, OptOutService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class ConfirmOptOutControllerSpec extends MockAuthActions
  with MockOptOutService with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService),
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[ConfirmOptOutController]

  val yearEnd = 2024
  val taxYear: TaxYear = TaxYear.forYearEnd(yearEnd)
  val optOutTaxYear: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, taxYear)
  val oneYearViewModelResponse: Future[Some[OptOutCheckpointViewModel]] =
    Future.successful(Some(OneYearOptOutCheckpointViewModel(optOutTaxYear.taxYear, Some(OneYearOptOutFollowedByAnnual))))
  val multiYearViewModelResponse: Future[Some[OptOutCheckpointViewModel]] =
    Future.successful(Some(MultiYearOptOutCheckpointViewModel(optOutTaxYear.taxYear)))
  val noEligibleTaxYearResponse: Future[None.type] = Future.successful(None)
  val failedResponse: Future[Nothing] = Future.failed(new Exception("some error"))

  val optOutUpdateResponseSuccess: Future[ITSAStatusUpdateResponse] = Future.successful(ITSAStatusUpdateResponseSuccess())
  val optOutUpdateResponseFailure: Future[ITSAStatusUpdateResponse] = Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        s"render the confirm opt out page" that {
          "is for one year" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutCheckPointPageViewModel(oneYearViewModelResponse)

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
          }

          "is for multiple years" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutCheckPointPageViewModel(multiYearViewModelResponse)

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
          }
        }
        s"return result with $INTERNAL_SERVER_ERROR status" when {

          "there is no tax year eligible for opt out" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutCheckPointPageViewModel(noEligibleTaxYearResponse)

            val result = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "opt out service fails" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutCheckPointPageViewModel(failedResponse)

            val result = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "render the home page" when {
          "the opt out feature switch is disabled" in {
            disable(OptOutFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents"
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

    s"post(isAgent = $isAgent)" when {
      val action = testController.submit(isAgent)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        "redirect to confirmed opt out controller" in {
          enable(OptOutFs)
          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockMakeOptOutUpdateRequest(optOutUpdateResponseSuccess)

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.ConfirmedOptOutController.show(isAgent).url)
        }

        s"redirect to optOutError controller" when {

          "there is no tax year eligible for opt out" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockMakeOptOutUpdateRequest(optOutUpdateResponseFailure)

            val result = action(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent).url)
          }

          "opt-out service fails" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockMakeOptOutUpdateRequest(optOutUpdateResponseFailure)

            val result = action(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent).url)
          }
        }
        "redirect to the home page" when {
          "the opt out feature switch is disabled" in {
            disable(OptOutFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }

}
