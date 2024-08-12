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

package controllers.optin

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optin.ChooseTaxYearView

class OptInChooseYearControllerSpec extends TestSupport with MockAuthenticationPredicate with MockOptOutService {

  val controller = new ChooseYearController(
    view = app.injector.instanceOf[ChooseTaxYearView], authorisedFunctions = mockAuthService, auth = testAuthenticator,
  )(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents]
  )

  def tests(isAgent: Boolean): Unit = {

    val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

    "show page" should {
      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result = controller.show(isAgent).apply(requestGET)

        status(result) shouldBe Status.OK
      }
    }
  }

  "OptInChooseYearController - Individual" when {
    tests(isAgent = false)
  }

  "OptInChooseYearController - Agent" when {
    tests(isAgent = true)
  }

}