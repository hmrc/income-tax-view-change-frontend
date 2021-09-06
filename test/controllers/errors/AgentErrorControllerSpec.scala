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

package controllers.errors

import assets.MessagesLookUp.{AgentErrorMessages => pageMessages}
import config.FrontendAppConfig
import controllers.predicates.SessionTimeoutPredicate
import mocks.controllers.predicates.MockAuthenticationPredicate
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.errorPages.AgentError

class AgentErrorControllerSpec extends TestSupport with MockAuthenticationPredicate {

  val TestAgentErrorController = new AgentErrorController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[AgentError]
  )(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  "Calling the show action of the NotAnAgentController" should {

    lazy val result = TestAgentErrorController.show(fakeRequestWithActiveSession)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK (200)" in {
      status(result) shouldBe OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    s"have the title '${pageMessages.title}'" in {
      document.title() shouldBe pageMessages.title
    }
  }

}
