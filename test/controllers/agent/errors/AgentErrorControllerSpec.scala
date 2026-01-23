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

package controllers.agent.errors

import mocks.auth.MockAuthActions
import org.jsoup.Jsoup
import play.api.test.Helpers._
import testConstants.BaseTestConstants.agentAuthRetrievalSuccess

class AgentErrorControllerSpec extends MockAuthActions {

  override lazy val app = applicationBuilderWithAuthBindings.build()

  lazy val testAgentErrorController = app.injector.instanceOf[AgentErrorController]

  "Calling the show action of the AgentErrorController" should {

    lazy val result = testAgentErrorController.show(fakeRequestWithActiveSession)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK (200)" in {
      setupMockAgentWithoutARNAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
      status(result) shouldBe OK
    }

    "return HTML" in {
      setupMockAgentWithoutARNAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    s"have the title ${messages("htmlTitle.agent", messages("agent-error.heading"))}" in {
      setupMockAgentWithoutARNAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
      document.title() shouldBe messages("htmlTitle.errorPage", messages("agent-error.heading"))
    }
  }

}
