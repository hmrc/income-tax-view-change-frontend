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

package controllers.optIn

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockOptInService, MockOptOutService}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.OptInCompletedView

class OptInCompletedControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockOptInService {

  val controller = new OptInCompletedController(
    view = app.injector.instanceOf[OptInCompletedView],
    mockOptInService,
    authorisedFunctions = mockAuthService,
    auth = testAuthenticator,
  )(
    appConfig = appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  def testShowSuccessCase(isAgent: Boolean): Unit = {

    "show page for current year" should {
      s"return result with $OK status" in {

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val currentTaxYear = CurrentOptInTaxYear(ITSAStatus.Annual, taxYear2023)
        val nextTaxYear = NextOptInTaxYear(ITSAStatus.Annual, taxYear2023.nextYear, currentTaxYear)
        val proposition = OptInProposition(currentTaxYear, nextTaxYear)
        mockFetchOptInProposition(Some(proposition))
        mockFetchSavedChosenTaxYear(Some(taxYear2023))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.OK
      }
    }

    "show page for next year" should {
      s"return result with $OK status" in {

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val currentTaxYear = CurrentOptInTaxYear(ITSAStatus.Annual, taxYear2023)
        val nextTaxYear = NextOptInTaxYear(ITSAStatus.Annual, taxYear2023.nextYear, currentTaxYear)
        val proposition = OptInProposition(currentTaxYear, nextTaxYear)
        mockFetchOptInProposition(Some(proposition))
        mockFetchSavedChosenTaxYear(Some(taxYear2023.nextYear))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.OK
      }
    }
  }

  def testShowFailCase(isAgent: Boolean): Unit = {

    "show page in error for current year" should {
      s"return result with $INTERNAL_SERVER_ERROR status" in {

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        mockFetchOptInProposition(None)
        mockFetchSavedChosenTaxYear(Some(taxYear2023))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "show page in error for next year" should {
      s"return result with $INTERNAL_SERVER_ERROR status" in {

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val currentTaxYear = CurrentOptInTaxYear(ITSAStatus.Annual, taxYear2023)
        val nextTaxYear = NextOptInTaxYear(ITSAStatus.Annual, taxYear2023.nextYear, currentTaxYear)
        val proposition = OptInProposition(currentTaxYear, nextTaxYear)
        mockFetchOptInProposition(Some(proposition))
        mockFetchSavedChosenTaxYear(None)

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "OptInCompletedController - Individual" when {
    testShowSuccessCase(isAgent = false)
    testShowFailCase(isAgent = false)
  }

  "OptInCompletedController - Agent" when {
    testShowSuccessCase(isAgent = true)
    testShowFailCase(isAgent = true)
  }

}
