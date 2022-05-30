/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.errors

import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import views.html.errorPages.AgentError

class AgentErrorControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler {

  val TestAgentErrorController = new AgentErrorController(
    mockAuthService,
    app.injector.instanceOf[AgentError]
  )(
    app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    mockItvcErrorHandler,
    ec
  )

  "Calling the show action of the NotAnAgentController" should {

    lazy val result = TestAgentErrorController.show(fakeRequestWithActiveSession)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK (200)" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      status(result) shouldBe OK
    }

    "return HTML" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    s"have the title ${messages("agent.titlePattern.serviceName.govUk", messages("agent-error.heading"))}" in {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
      document.title() shouldBe messages("agent.titlePattern.serviceName.govUk", messages("agent-error.heading"))
    }
  }

}
