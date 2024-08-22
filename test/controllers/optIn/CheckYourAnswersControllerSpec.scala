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
import mocks.services.{MockDateService, MockOptInService, MockOptOutService}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{OK, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.CheckYourAnswersView
import play.api.test.Helpers._

class CheckYourAnswersControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockOptInService with MockDateService {

  val controller = new CheckYourAnswersController(
    view = app.injector.instanceOf[CheckYourAnswersView],
    mockOptInService,
    authorisedFunctions = mockAuthService,
    auth = testAuthenticator,
  )(
    dateService = mockDateService,
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents]
  )

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  def tests(isAgent: Boolean): Unit = {
    "show page" should {
      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockFetchSavedChosenTaxYear(taxYear2023)

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = controller.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.OK
      }
    }
  }

  "CheckYourAnswersController - Individual" when {
    tests(isAgent = false)
  }

  "CheckYourAnswersController - Agent" when {
    tests(isAgent = true)
  }
}