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
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import mocks.repositories.MockOptOutSessionDataRepository
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutMultiYearViewModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutService.QuarterlyUpdatesCountForTaxYearModel
import services.optout.{CurrentOptOutTaxYear, OptOutProposition, OptOutTestSupport}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.OptOutChooseTaxYear

import scala.concurrent.Future

class OptOutChooseTaxYearControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockFrontendAuthorisedFunctions with MockOptOutService with MockOptOutSessionDataRepository {

  val optOutProposition: OptOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

  val optOutChooseTaxYear: OptOutChooseTaxYear = app.injector.instanceOf[OptOutChooseTaxYear]
  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]

  val controller = new OptOutChooseTaxYearController(optOutChooseTaxYear, mockOptOutService, mockOptOutSessionDataRepository)(appConfig,
    ec, testAuthenticator, mockAuthService, itvcErrorHandler, itvcErrorHandlerAgent, mcc)

  val yearEnd = optOutProposition.availableTaxYearsForOptOut(1).endYear
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(yearEnd)
  val nextTaxYear: TaxYear = currentTaxYear.nextYear
  val previousTaxYear: TaxYear = currentTaxYear.previousYear

  val optOutTaxYear: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
  val eligibleTaxYearResponse: Future[Some[OptOutMultiYearViewModel]] = Future.successful(Some(OptOutMultiYearViewModel()))
  val noEligibleTaxYearResponse: Future[None.type] = Future.successful(None)
  val optOutYearsOffered: Seq[TaxYear] = Seq(previousTaxYear, currentTaxYear, nextTaxYear)
  val optOutYearsOfferedFuture: Future[Seq[TaxYear]] = Future.successful(optOutYearsOffered)

  val counts: Future[QuarterlyUpdatesCountForTaxYearModel] = Future.successful(QuarterlyUpdatesCountForTaxYearModel(Seq(
    QuarterlyUpdatesCountForTaxYear(optOutProposition.availableTaxYearsForOptOut.head, 1),
    QuarterlyUpdatesCountForTaxYear(optOutProposition.availableTaxYearsForOptOut(1), 1),
    QuarterlyUpdatesCountForTaxYear(optOutProposition.availableTaxYearsForOptOut.last, 0)
  )))

  val taxYears: Seq[TaxYear] =
    Seq(TaxYear.forYearEnd(2023), TaxYear.forYearEnd(2024), TaxYear.forYearEnd(2025))
  val futureTaxYears: Future[Seq[TaxYear]] = Future.successful(taxYears)

  val submissionsCountForTaxYearModel: Future[QuarterlyUpdatesCountForTaxYearModel] = counts

  "OptOutChooseTaxYearController - Individual" when {
    controllerShowTest(isAgent = false)
    controllerSubmitTest(isAgent = false)
    testSaveIntent(isAgent = false)
  }

  "OptOutChooseTaxYearController - Agent" when {
    controllerShowTest(isAgent = true)
    controllerSubmitTest(isAgent = true)
    testSaveIntent(isAgent = true)
  }

  def controllerShowTest(isAgent: Boolean): Unit = {

    "show method is invoked" should {
      s"return result with ${Status.OK} status for show" in {

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockFetchIntent(Future.successful(None))
        mockGetSubmissionCountForTaxYear(counts)
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }
    }

    "show method is invoked with intent pre-selected" should {
      s"return result with ${Status.OK} status for show" in {

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockFetchIntent(Future.successful(Some(optOutTaxYear.taxYear)))
        mockGetSubmissionCountForTaxYear(counts)
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.show(isAgent)(requestGET)

        Jsoup.parse(contentAsString(result)).select("#choice-year-1[checked]").toArray should have length 1
        status(result) shouldBe Status.OK
      }
    }

  }

  def controllerSubmitTest(isAgent: Boolean): Unit = {

    "submit method is invoked and choice made in form" should {
      s"return result with successful ${Status.SEE_OTHER} status for submit" in {

        val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
        val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> currentTaxYear.toString,
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
        )

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockGetSubmissionCountForTaxYear(counts)
        mockSaveIntent(currentTaxYear, Future.successful(true))
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.submit(isAgent)(requestPOSTWithChoice)

        status(result) shouldBe Status.SEE_OTHER
        val agentPath = if(isAgent) "/agents" else ""
        redirectLocation(result) shouldBe Some(s"/report-quarterly/income-and-expenses/view${agentPath}/optout/review-confirm-taxyear")
      }

      s"return result with ${Status.INTERNAL_SERVER_ERROR} status for submit when intent cant be saved" in {

        val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
        val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> currentTaxYear.toString,
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
        )

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockGetSubmissionCountForTaxYear(counts)
        mockSaveIntent(currentTaxYear, Future.successful(false))
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.submit(isAgent)(requestPOSTWithChoice)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "submit method is invoked and choice is missing in form" should {
      s"return result with with error ${Status.BAD_REQUEST} status for submit" in {

        val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
        val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> "", //missing
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
        )

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockGetSubmissionCountForTaxYear(counts)
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.submit(isAgent)(requestPOSTWithChoice)

        status(result) shouldBe Status.BAD_REQUEST
        redirectLocation(result) shouldBe None
      }
    }
  }

  def testSaveIntent(isAgent: Boolean): Unit = {

    "submit method is invoked and choice made in form but save intent fails" should {
      s"return result with ${Status.INTERNAL_SERVER_ERROR} status for submit" in {

        val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
        val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> currentTaxYear.toString,
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
        )

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockGetSubmissionCountForTaxYear(counts)
        mockSaveIntent(currentTaxYear, Future.successful(false))
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.submit(isAgent)(requestPOSTWithChoice)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        redirectLocation(result) shouldBe None
      }
    }

    "submit method is invoked and choice made not in form but save intent fails" should {
      s"return result with ${Status.BAD_REQUEST} status for submit" in {

        val requestPOST = if (isAgent) fakePostRequestConfirmedClient() else fakePostRequestWithNinoAndOrigin("PTA")
        val requestPOSTWithChoice = requestPOST.withFormUrlEncodedBody(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> "",
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
        )

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockGetSubmissionCountForTaxYear(counts)
        mockSaveIntent(currentTaxYear, Future.successful(true))
        mockRecallOptOutProposition(Future.successful(optOutProposition))

        val result: Future[Result] = controller.submit(isAgent)(requestPOSTWithChoice)

        status(result) shouldBe Status.BAD_REQUEST
        redirectLocation(result) shouldBe None
      }
    }

  }
}