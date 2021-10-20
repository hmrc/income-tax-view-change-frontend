/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent

import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.views.agent.MockConfirmClient
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired

class RemoveClientDetailsSessionsControllerSpec extends TestSupport
  with MockConfirmClient
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with MockItvcErrorHandler {

  object TestRemoveClientDetailsSessionsController extends RemoveClientDetailsSessionsController(
    mockAuthService
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    mockItvcErrorHandler,
    ec
  )

  ".show" when {
    "the user is not authenticated" should {
      "redirect the user to authenticate" in {
        setupMockAgentAuthorisationException()

        val result = TestRemoveClientDetailsSessionsController.show()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result = TestRemoveClientDetailsSessionsController.show()(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

		"remove client details session keys and redirect to the enter client UTR page" in {
			setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

			val result = TestRemoveClientDetailsSessionsController.show()(fakeRequestConfirmedClient())

			val removedSessionKeys: List[String] =
				List(
					"SessionKeys.clientLastName",
					"SessionKeys.clientFirstName",
					"SessionKeys.clientNino",
					"SessionKeys.clientUTR",
					"SessionKeys.confirmedClient"
				)

			removedSessionKeys.foreach(key => result.futureValue.header.headers.get(key) shouldBe None)

			status(result) shouldBe SEE_OTHER
			redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/client-utr")

		}
  }
}
