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

import enums.MTDIndividual
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import mocks.auth.MockAuthActions
import mocks.repositories.MockOptOutSessionDataRepository
import mocks.services.MockOptOutService
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutMultiYearViewModel
import org.jsoup.Jsoup
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.{CurrentOptOutTaxYear, OptOutProposition, OptOutService, OptOutTestSupport}
import services.reportingfreq.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class OptOutChooseTaxYearControllerSpec extends MockAuthActions
  with MockOptOutService with MockOptOutSessionDataRepository {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService),
      api.inject.bind[OptOutSessionDataRepository].toInstance(mockOptOutSessionDataRepository)
    ).build()

  lazy val testController = app.injector.instanceOf[OptOutChooseTaxYearController]


  val optOutProposition: OptOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

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

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        s"render the choose tax year page" that {
          "has intent not pre-selected" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockGetSubmissionCountForTaxYear(counts)
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, None))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
            Jsoup.parse(contentAsString(result)).select("#choice-year-1[checked]").toArray should have length 0

          }

          "has intent pre-selected" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockGetSubmissionCountForTaxYear(counts)
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(optOutTaxYear.taxYear)))

            val result = action(fakeRequest)

            Jsoup.parse(contentAsString(result)).select("#choice-year-1[checked]").toArray should have length 1
            status(result) shouldBe Status.OK
          }
        }

        "render the home page" when {
          "the OptOut feature switch is disabled" in {
            disable(OptOutFs)

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
        "redirect to review and confirm page" when {
          "current tax is chosen" in {
            enable(OptOutFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockGetSubmissionCountForTaxYear(counts)
            mockSaveIntent(currentTaxYear, Future.successful(true))
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(currentTaxYear)))

            val formData = Map(
              ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> currentTaxYear.toString,
              ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> ""
            )

            val result = action(fakeRequest.withFormUrlEncodedBody(
              formData.toSeq: _*
            ))

            status(result) shouldBe Status.SEE_OTHER
            val agentPath = if (isAgent) "/agents" else ""
            redirectLocation(result) shouldBe Some(s"/report-quarterly/income-and-expenses/view${agentPath}/optout/review-confirm-taxyear")
          }

          "previous tax is chosen" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockGetSubmissionCountForTaxYear(counts)
            mockSaveIntent(previousTaxYear, Future.successful(true))
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(previousTaxYear)))

            val formData = Map(ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> previousTaxYear.toString,
              ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> "")

            val result = action(fakeRequest.withFormUrlEncodedBody(
              formData.toSeq: _*
            ))

            status(result) shouldBe Status.SEE_OTHER
            val agentPath = if (isAgent) "/agents" else ""
            redirectLocation(result) shouldBe Some(s"/report-quarterly/income-and-expenses/view${agentPath}/optout/review-confirm-taxyear")
          }

          "next tax is chosen" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockGetSubmissionCountForTaxYear(counts)
            mockSaveIntent(nextTaxYear, Future.successful(true))
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(nextTaxYear)))

            val formData = Map(ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> nextTaxYear.toString,
              ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> "")

            val result = action(fakeRequest.withFormUrlEncodedBody(
              formData.toSeq: _*
            ))

            status(result) shouldBe Status.SEE_OTHER
            val agentPath = if (isAgent) "/agents" else ""
            redirectLocation(result) shouldBe Some(s"/report-quarterly/income-and-expenses/view${agentPath}/optout/review-confirm-taxyear")
          }
        }

        "return a BadRequest" when {
          "choice is missing in form" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockGetSubmissionCountForTaxYear(counts)
            mockSaveIntent(nextTaxYear, Future.successful(true))
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(nextTaxYear)))

            val formData = Map(ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> "", //missing
              ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> "")

            val result = action(fakeRequest.withFormUrlEncodedBody(
              formData.toSeq: _*
            ))

            status(result) shouldBe Status.BAD_REQUEST
          }
        }

        "return an Internal Server Error" when {
          "intent cant be saved" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockGetSubmissionCountForTaxYear(counts)
            mockSaveIntent(currentTaxYear, Future.successful(false))
            mockRecallOptOutPropositionWithIntent(Future.successful(optOutProposition, Some(currentTaxYear)))

            val formData = Map(ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> currentTaxYear.toString,
              ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> "")

            val result = action(fakeRequest.withFormUrlEncodedBody(
              formData.toSeq: _*
            ))

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        "redirect to the home page" when {
          "the opt out feature switch is disabled" in {
            disable(OptOutFs)
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
  }
}