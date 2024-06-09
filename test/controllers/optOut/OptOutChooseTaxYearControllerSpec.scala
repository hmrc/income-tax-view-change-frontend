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
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutMultiYearViewModel
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.NextUpdatesService.SubmissionsCountForTaxYear
import services.optout.CurrentOptOutTaxYear
import services.optout.OptOutService.SubmissionsCountForTaxYearModel
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.OptOutChooseTaxYear

import scala.concurrent.Future

class OptOutChooseTaxYearControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockFrontendAuthorisedFunctions with MockOptOutService {

  val optOutChooseTaxYear: OptOutChooseTaxYear = app.injector.instanceOf[OptOutChooseTaxYear]
  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]

  val controller = new OptOutChooseTaxYearController(optOutChooseTaxYear, mockOptOutService)(appConfig,
    ec, testAuthenticator, mockAuthService, itvcErrorHandler, itvcErrorHandlerAgent, mcc)

  val yearEnd = 2014
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(yearEnd)
  val nextTaxYear: TaxYear = currentTaxYear.nextYear
  val previousTaxYear: TaxYear = currentTaxYear.previousYear

  val optOutTaxYear: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
  val eligibleTaxYearResponse: Future[Some[OptOutMultiYearViewModel]] = Future.successful(Some(OptOutMultiYearViewModel()))
  val noEligibleTaxYearResponse: Future[None.type] = Future.successful(None)
  val optOutYearsOffered: Seq[TaxYear] = Seq(previousTaxYear, currentTaxYear, nextTaxYear)
  val optOutYearsOfferedFuture: Future[Seq[TaxYear]] = Future.successful(optOutYearsOffered)

  "OptOutChooseTaxYearController - Individual" when {
    testHappyCase(isAgent = false)
  }

  "OptOutChooseTaxYearController - Agent" when {
    testHappyCase(isAgent = true)
  }

  def testHappyCase(isAgent: Boolean): Unit = {

    "show method is invoked" should {
      s"return result with ${Status.OK} status" in {

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockNextUpdatesPageMultiYearOptOutViewModel(eligibleTaxYearResponse)
        mockGetTaxYearsAvailableForOptOut(optOutYearsOfferedFuture)
        mockGetSubmissionCountForTaxYear(optOutYearsOffered, counts)

        val result: Future[Result] = controller.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }
    }

  }

  val counts: Future[SubmissionsCountForTaxYearModel] = Future.successful(SubmissionsCountForTaxYearModel(Seq(
    SubmissionsCountForTaxYear(TaxYear.forYearEnd(2023), 1),
    SubmissionsCountForTaxYear(TaxYear.forYearEnd(2024), 1),
    SubmissionsCountForTaxYear(TaxYear.forYearEnd(2025), 0)
  )))

}