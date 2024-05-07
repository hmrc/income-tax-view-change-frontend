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

import config.ItvcErrorHandler
import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testUtils.TestSupport

import scala.concurrent.Future

class SingleYearOptOutConfirmationControllerSpec extends TestSupport
  with MockAuthenticationPredicate {

  object TestSingleYearOptOutConfirmationController extends SingleYearOptOutConfirmationController()(
    appConfig = appConfig,
    ec = ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = mockItvcErrorHandler,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    authorisedFunctions = mockAuthService) {
  }

  "SingleYearOptOutConfirmationController" when {
    "show method is invoked" should {
      s"return result with $OK status" in {
        val result: Future[Result] = TestSingleYearOptOutConfirmationController.show(isAgent = false)(fakeRequestWithTestSession)
        status(result) shouldBe Status.OK
      }
    }
  }
}
