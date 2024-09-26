/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.OptOutError

import scala.concurrent.Future

class OptOutErrorControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockFrontendAuthorisedFunctions {

  val view: OptOutError = app.injector.instanceOf[OptOutError]
  val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val controller = new OptOutErrorController(view, testAuthenticator, mockAuthService)(appConfig, ec, itvcErrorHandler, itvcErrorHandlerAgent, mcc)

  "OptOutErrorController - Individual" when {
    view(isAgent = false)
  }

  "OptOutErrorController - Agent" when {
    view(isAgent = true)
  }

  def view(isAgent: Boolean): Unit = {

    "OptOutErrorController show" should {
      s"return error page" in {

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result: Future[Result] = controller.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }
    }
  }
}