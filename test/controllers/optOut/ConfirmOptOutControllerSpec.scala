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
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import models.incomeSourceDetails.TaxYear
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearCheckpointViewModel}
import org.mockito.Mockito.mock
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.SessionService
import services.optout.OptOutService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.{CheckOptOutAnswers, ConfirmOptOut}

import scala.concurrent.Future

class ConfirmOptOutControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService {

  object TestConfirmOptOutController extends ConfirmOptOutController(
    auth = testAuthenticator,
    view = app.injector.instanceOf[ConfirmOptOut],
    checkOptOutAnswers = app.injector.instanceOf[CheckOptOutAnswers],
    optOutService = mockOptOutService,
    sessionService =  mock(classOf[SessionService]))(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  def tests(isAgent: Boolean): Unit = {
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    val taxYear = TaxYear.forYearEnd(2024)
    val eligibleOneTaxYearResponse = Future.successful(OptOutOneYearCheckpointViewModel(taxYear, showFutureChangeInfo = true))
    val eligibleMultiTaxYearResponse = Future.successful(OptOutMultiYearViewModel(taxYear))
//    val noEligibleTaxYearResponse = Future.successful(OptOutOneYearCheckpointViewModel(
//      taxYear = ?,
//      showFutureChangeInfo = false
//    ))
   // val noEligibleTaxYearResponse = Future.successful(None)
    val failedResponse = Future.failed(new Exception("some error"))

    "show method is invoked" should {
      s"return result with $OK status when viewing Single Year Opt Out" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockOptOutOneYearCheckPointPageViewModel(eligibleOneTaxYearResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }

      s"return result with $OK status when viewing Multi Year Opt Out" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockOptOutMultiYearCheckPointPageViewModel(eligibleMultiTaxYearResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status" when {
//        "there is no tax year eligible for opt out" in {
//          setupMockAuthorisationSuccess(isAgent)
//          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
//          mockOptOutOneYearCheckPointPageViewModel(noEligibleTaxYearResponse)
//
//          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)
//
//          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
//        }

        "opt out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockOptOutOneYearCheckPointPageViewModel(failedResponse)

          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }


  "ConfirmOptOutController - Individual" when {
    tests(isAgent = false)
  }
  "ConfirmOptOutController - Agent" when {
    tests(isAgent = true)
  }
}
