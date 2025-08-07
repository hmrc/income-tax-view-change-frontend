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

package controllers.optOut

import enums.{MTDIndividual, MTDSupportingAgent}
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import mocks.auth.MockAuthActions
import mocks.services.MockOptOutService
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.optout.OptOutOneYearViewModel
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optout.OptOutService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class
SingleYearOptOutWarningControllerSpec extends MockAuthActions with MockOptOutService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService)
    ).build()

  lazy val testSingleYearOptOutWarningController = app.injector.instanceOf[SingleYearOptOutWarningController]

  mtdAllRoles.foreach{ case mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual

    val requestGET = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    val requestPOST = fakePostRequestBasedOnMTDUserType(mtdUserRole)
    val confirmOptOutPage = Some(controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url)

    val optOutCancelledUrl =
      if (isAgent) {
        controllers.optOut.routes.OptOutCancelledController.showAgent().url
      } else {
        controllers.optOut.routes.OptOutCancelledController.show().url
      }

    val taxYear = TaxYear.forYearEnd(2024)
    val eligibleTaxYearResponse = Future.successful(Some(OptOutOneYearViewModel(taxYear, None)))
    val noEligibleTaxYearResponse = Future.successful(None)
    val failedResponse = Future.failed(new Exception("some error"))

    s"SingleYearOptOutWarningController - $mtdUserRole" when {

      "show method is invoked" should {

        val showAction = testSingleYearOptOutWarningController.show(isAgent = isAgent)

        s"return result with $OK status" in {
          enable(OptOutFs)
          setupMockSuccess(mtdUserRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

          val result: Future[Result] = showAction(requestGET)
          status(result) shouldBe Status.OK
        }

        s"return result with $INTERNAL_SERVER_ERROR status" when {
          "there is no tax year eligible for opt out" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(noEligibleTaxYearResponse)

            val result: Future[Result] = showAction(requestGET)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "opt out service fails" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(failedResponse)

            val result: Future[Result] = showAction(requestGET)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "render the home page" when {
          "the feature switch is disabled" in {
            disable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result: Future[Result] = showAction(requestGET)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        if (isAgent)
          testMTDAgentAuthFailures(showAction, mtdUserRole == MTDSupportingAgent)
        else
          testMTDIndividualAuthFailures(showAction)
      }

      "submit method is invoked" should {

        val submitAction = testSingleYearOptOutWarningController.submit(isAgent = isAgent)

        s"return result with $SEE_OTHER status with redirect to $confirmOptOutPage" when {
          "Yes response is submitted" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

            val result: Future[Result] = submitAction(
              requestPOST.withFormUrlEncodedBody(
                ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "true",
                ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
              ))
            status(result) shouldBe Status.SEE_OTHER

            redirectLocation(result) shouldBe confirmOptOutPage
          }
        }
        s"return result with 303 status with redirect to Opt Out Cancelled Page" when {

          "No response is submitted" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

            val result: Future[Result] = submitAction(
              requestPOST.withFormUrlEncodedBody(
                ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "false",
                ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
              ))
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(optOutCancelledUrl)
        }
          }

        s"return result with $BAD_REQUEST status" when {
          "invalid response is submitted" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

            val result: Future[Result] = submitAction(
              requestPOST.withFormUrlEncodedBody(
                ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "",
                ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
              ))
            status(result) shouldBe Status.BAD_REQUEST
          }
        }
        s"return result with $INTERNAL_SERVER_ERROR status" when {
          "there is no tax year eligible for opt out" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(noEligibleTaxYearResponse)

            val result: Future[Result] = submitAction(requestPOST)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "opt out service fails" in {
            enable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRecallNextUpdatesPageOneYearOptOutViewModel(failedResponse)

            val result: Future[Result] = submitAction(requestPOST)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        "redirect to the home page" when {
          "the feature switch is disabled" in {
            disable(OptOutFs)
            setupMockSuccess(mtdUserRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result: Future[Result] = submitAction(requestPOST)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }

        if (isAgent)
          testMTDAgentAuthFailures(submitAction, mtdUserRole == MTDSupportingAgent)
        else
          testMTDIndividualAuthFailures(submitAction)
      }

    }

  }
}
