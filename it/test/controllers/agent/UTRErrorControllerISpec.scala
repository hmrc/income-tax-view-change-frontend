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

package controllers.agent

import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.servicemocks.MTDAgentAuthStub
import play.api.http.Status._

class UTRErrorControllerISpec extends ComponentSpecBase with FeatureSwitching {
  val path = "/agents/cannot-view-client"

  s"GET $path" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        MTDAgentAuthStub.stubUnauthorised()
        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"return $SEE_OTHER with Agent error page" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentError()
        val result = buildGETMTDClient(path).futureValue

        Then(s"Agent error page is shown with status SEE_OTHER")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
        )
      }
    }

    s"return $OK with the UTR Error page" when {
      "without checking whether the UTR is in session" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildGETMTDClient(path).futureValue

        Then("The UTR Error page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitleAgent("agent.utr_error.heading", isErrorPage = true)
        )
      }
    }
  }

  s"POST $path" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        MTDAgentAuthStub.stubUnauthorised()
        val result = buildPOSTMTDPostClient(path, body = Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"return $SEE_OTHER to agent error page" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentError()
        val result = buildPOSTMTDPostClient(path, body = Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
        )
      }
    }
  }

}
