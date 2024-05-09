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

import config.ItvcErrorHandler
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.ConfirmSingleYearOptOut

import scala.concurrent.Future

class SingleYearOptOutConfirmationControllerSpec extends TestSupport
  with MockAuthenticationPredicate {

  object TestSingleYearOptOutConfirmationController extends SingleYearOptOutConfirmationController(
    auth = testAuthenticator,
    view = app.injector.instanceOf[ConfirmSingleYearOptOut])(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = mockItvcErrorHandler,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  def tests(isAgent: Boolean): Unit = {
    val request = if (isAgent) fakeRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
    val previousPage = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url
    "show method is invoked" should {
      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        val result: Future[Result] = TestSingleYearOptOutConfirmationController.show(isAgent = isAgent)(request)
        status(result) shouldBe Status.OK
      }
    }

    "submit method is invoked" when {
      "Yes response is submitted" should {
        s"return result with $SEE_OTHER status with redirect to ${"checkpoint page"}" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          val result: Future[Result] = TestSingleYearOptOutConfirmationController.submit(isAgent = isAgent)(
            request.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "true",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe controllers.optOut.routes.OptOutCheckpointController.show.url
        }
      }
      "No response is submitted" should {
        s"return result with $SEE_OTHER status with redirect to ${"Next update page"}" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          val result: Future[Result] = TestSingleYearOptOutConfirmationController.submit(isAgent = isAgent)(
            request.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "false",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe previousPage
        }
      }
      "invalid response is submitted" should {
        s"return result with $BAD_REQUEST status" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          val result: Future[Result] = TestSingleYearOptOutConfirmationController.submit(isAgent = isAgent)(
            request.withFormUrlEncodedBody(
              ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> "",
              ConfirmOptOutSingleTaxYearForm.csrfToken -> ""
            ))
          status(result) shouldBe Status.BAD_REQUEST
        }
      }
    }
  }


  "SingleYearOptOutConfirmationController - Individual" when {
    tests(isAgent = false)
  }
  "SingleYearOptOutConfirmationController - Agent" when {
    tests(isAgent = true)
  }
}
