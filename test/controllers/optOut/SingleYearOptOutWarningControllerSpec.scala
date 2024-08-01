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

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import models.incomeSourceDetails.TaxYear
import models.optout.OptOutOneYearViewModel
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.SingleYearOptOutWarning

import scala.concurrent.Future

class SingleYearOptOutWarningControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService {

  object TestSingleYearOptOutWarningController extends SingleYearOptOutWarningController(
    auth = testAuthenticator,
    view = app.injector.instanceOf[SingleYearOptOutWarning],
    optOutService = mockOptOutService)(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  def tests(isAgent: Boolean): Unit = {
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
    val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
    val confirmOptOutPage = Some(controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url)
    val nextUpdatesPage = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url
    val taxYear = TaxYear.forYearEnd(2024)
    val eligibleTaxYearResponse = Future.successful(Some(OptOutOneYearViewModel(taxYear, None)))
    val noEligibleTaxYearResponse = Future.successful(None)
    val failedResponse = Future.failed(new Exception("some error"))

    "show method is invoked" should {
      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

        val result: Future[Result] = TestSingleYearOptOutWarningController.show(isAgent = isAgent)(requestGET)
        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status" when {
        "there is no tax year eligible for opt out" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(noEligibleTaxYearResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.show(isAgent = isAgent)(requestGET)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "opt out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(failedResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.show(isAgent = isAgent)(requestGET)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "submit method is invoked" should {
      s"return result with $SEE_OTHER status with redirect to $confirmOptOutPage" when {
        "Yes response is submitted" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.submit(isAgent = isAgent)(
            requestPOST.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "true",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.SEE_OTHER

          redirectLocation(result) shouldBe confirmOptOutPage
        }
      }
      s"return result with $SEE_OTHER status with redirect to $nextUpdatesPage" when {
        "No response is submitted" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.submit(isAgent = isAgent)(
            requestPOST.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "false",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(nextUpdatesPage)
        }
      }
      s"return result with $BAD_REQUEST status" when {
        "invalid response is submitted" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(eligibleTaxYearResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.submit(isAgent = isAgent)(
            requestPOST.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.BAD_REQUEST
        }
      }
      s"return result with $INTERNAL_SERVER_ERROR status" when {
        "there is no tax year eligible for opt out" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(noEligibleTaxYearResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.show(isAgent = isAgent)(requestPOST)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "opt out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockRecallNextUpdatesPageOneYearOptOutViewModel(failedResponse)

          val result: Future[Result] = TestSingleYearOptOutWarningController.show(isAgent = isAgent)(requestPOST)
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }


  "SingleYearOptOutWarningController - Individual" when {
    tests(isAgent = false)
  }
  "SingleYearOptOutWarningController - Agent" when {
    tests(isAgent = true)
  }
}
