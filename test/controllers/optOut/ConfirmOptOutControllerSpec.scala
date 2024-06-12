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
import connectors.optout.OptOutUpdateRequestModel.{OptOutUpdateResponse, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutCheckpointViewModel
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import repositories.UIJourneySessionDataRepository
import services.optout.{CurrentOptOutTaxYear, OneYearOptOutFollowedByAnnual}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.{ConfirmOptOut, ConfirmOptOutMultiYear}

import scala.concurrent.Future

class ConfirmOptOutControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService {

  object TestConfirmOptOutController extends ConfirmOptOutController(
    auth = testAuthenticator,
    view = app.injector.instanceOf[ConfirmOptOut],
    multiyearCheckpointView = app.injector.instanceOf[ConfirmOptOutMultiYear],
    optOutService = mockOptOutService)(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  "ConfirmOptOutController - Individual" when {
    oneYearShowTest(isAgent = false)
    multiYearShowTest(isAgent = false)

    oneYearSubmitTest(isAgent = false)
    multiYearSubmitTest(isAgent = false)
  }

  "ConfirmOptOutController - Agent - view" when {
    oneYearShowTest(isAgent = true)
    multiYearShowTest(isAgent = true)

    oneYearSubmitTest(isAgent = true)
    multiYearSubmitTest(isAgent = true)
  }

  val yearEnd = 2024
  val taxYear: TaxYear = TaxYear.forYearEnd(yearEnd)
  val optOutTaxYear: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, taxYear)
  val oneYearViewModelResponse: Future[Some[OptOutCheckpointViewModel]] =
    Future.successful(Some(OptOutCheckpointViewModel(optOutTaxYear.taxYear, Some(OneYearOptOutFollowedByAnnual))))
  val multiYearViewModelResponse: Future[Some[OptOutCheckpointViewModel]] =
    Future.successful(Some(OptOutCheckpointViewModel(optOutTaxYear.taxYear, Some(OneYearOptOutFollowedByAnnual),
      isOneYear = false)))
  val noEligibleTaxYearResponse: Future[None.type] = Future.successful(None)
  val failedResponse: Future[Nothing] = Future.failed(new Exception("some error"))

  val optOutUpdateResponseSuccess: Future[OptOutUpdateResponse] = Future.successful(OptOutUpdateResponseSuccess("123"))
  val optOutUpdateResponseFailure: Future[OptOutUpdateResponse] = Future.successful(OptOutUpdateResponseFailure.defaultFailure("123"))


  def oneYearShowTest(isAgent: Boolean): Unit = {
    val testName = "OneYear Opt-Out"
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    s"show method is invoked for $testName" should {

      s"return result with $OK status for $testName" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockOptOutCheckPointPageViewModel(oneYearViewModelResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status" when {

        "there is no tax year eligible for opt out" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockOptOutCheckPointPageViewModel(noEligibleTaxYearResponse)

          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "opt out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockOptOutCheckPointPageViewModel(failedResponse)

          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  def multiYearShowTest(isAgent: Boolean): Unit = {
    val testName = "MultiYear Opt-Out"
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    s"show method is invoked $testName" should {

      s"return result with $OK status for $testName" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockOptOutCheckPointPageViewModel(multiYearViewModelResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }
    }
  }

  def oneYearSubmitTest(isAgent: Boolean): Unit = {
    val testName = "OneYear Opt-Out"
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    s"submit method is invoked for $testName" should {

      s"return result with $OK status for $testName" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMakeOptOutUpdateRequest(optOutUpdateResponseSuccess)

        val result: Future[Result] = TestConfirmOptOutController.submit(isAgent)(requestGET)

        status(result) shouldBe Status.SEE_OTHER
      }

      s"return result with $INTERNAL_SERVER_ERROR status" when {

        "there is no tax year eligible for opt out" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockMakeOptOutUpdateRequest(optOutUpdateResponseFailure)

          val result: Future[Result] = TestConfirmOptOutController.submit(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "opt-out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          mockMakeOptOutUpdateRequest(optOutUpdateResponseFailure)

          val result: Future[Result] = TestConfirmOptOutController.submit(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  def multiYearSubmitTest(isAgent: Boolean): Unit = {
    val testName = "MultiYear Opt-Out"
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    s"submit method is invoked $testName" should {

      s"return result with $OK status for $testName" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockMakeOptOutUpdateRequest(optOutUpdateResponseSuccess)

        val result: Future[Result] = TestConfirmOptOutController.submit(isAgent)(requestGET)

        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}
