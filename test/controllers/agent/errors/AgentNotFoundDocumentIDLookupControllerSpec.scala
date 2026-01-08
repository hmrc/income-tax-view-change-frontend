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

class AgentNotFoundDocumentIDLookupControllerSpec extends MockAuthActions {

  override lazy val app = applicationBuilderWithAuthBindings.build()

  lazy val testAgentErrorController = app.injector.instanceOf[AgentNotFoundDocumentIDLookupController]

  "Calling the show action of the NotAnAgentController" should {

    "render the Agent not found page" in {
      setupMockAgentWithoutARNAuthSuccess(mockFAF)(agentAuthRetrievalSuccess)
      val result = testAgentErrorController.show(fakeRequestWithActiveSession)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe messages("htmlTitle.agent", messages("error.custom.heading"))
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }
}
