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
import mocks.services.{MockOptOutService, MockSessionService}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.{MultiYearOptOutCheckpointViewModel, OneYearOptOutCheckpointViewModel, OptOutSessionData}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.optout.OneYearOptOutFollowedByAnnual
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import utils.OptOutJourney
import views.html.optOut.{CheckOptOutAnswers, ConfirmOptOut}

import scala.concurrent.Future

class ConfirmOptOutControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockSessionService {

  object TestConfirmOptOutController extends ConfirmOptOutController(
    auth = testAuthenticator,
    view = app.injector.instanceOf[ConfirmOptOut],
    checkOptOutAnswers = app.injector.instanceOf[CheckOptOutAnswers],
    optOutService = mockOptOutService,
    sessionService = mockSessionService)(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  def tests(isAgent: Boolean): Unit = {
    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    val expectedOptOutSessionData = OptOutSessionData(intent = Some(TaxYear.forYearEnd(2024).toString))
    val expectedSessionData = UIJourneySessionData(sessionId = testSessionId,
      journeyType = OptOutJourney.Name,
      optOutSessionData = Some(expectedOptOutSessionData))

    val taxYear = TaxYear.forYearEnd(2024)
    val eligibleTaxYearResponse = Future.successful(Some(OneYearOptOutCheckpointViewModel(taxYear, Some(OneYearOptOutFollowedByAnnual))))
    val eligibleMultiTaxYearResponse = Future.successful(Some(MultiYearOptOutCheckpointViewModel(taxYear, None)))
    val noEligibleTaxYearResponse = Future.successful(None)
    val failedResponse = Future.failed(new Exception("some error"))


    "show method is invoked" should {
      s"return result with $OK status when viewing Single Year Opt Out" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(expectedSessionData)))
        mockOptOutCheckPointPageViewModel(eligibleTaxYearResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }

      s"return result with $OK status when viewing Multi Year Opt Out" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(expectedSessionData)))
        mockOptOutCheckPointPageViewModel(eligibleMultiTaxYearResponse)

        val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status" when {
        "there is no tax year eligible for opt out" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockCreateSession(true)
          setupMockGetMongo(Right(Some(expectedSessionData)))
          mockOptOutCheckPointPageViewModel(noEligibleTaxYearResponse)


          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "opt out service fails" in {
          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
          setupMockCreateSession(true)
          setupMockGetMongo(Right(Some(expectedSessionData)))
          mockOptOutCheckPointPageViewModel(failedResponse)

          val result: Future[Result] = TestConfirmOptOutController.show(isAgent)(requestGET)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
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