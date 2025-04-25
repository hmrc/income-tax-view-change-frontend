/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.ws.WSResponse

class ClientDetailsFailureControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val path = "/agents/not-authorised-to-view-client"

  s"GET ${controllers.agent.routes.ClientRelationshipFailureController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        MTDAgentAuthStub.stubUnauthorised()

        val result: WSResponse = buildGETMTDClient(path, Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
    s"redirect to agent error page" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentError()

        val result: WSResponse = buildGETMTDClient(path, Map.empty).futureValue

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
        )
      }
    }
    s"return $OK with the enter client utr page" in {
      MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

      val result: WSResponse = buildGETMTDClient(path, Map.empty).futureValue

      Then("The client relationship failure page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("agent.client_relationship_failure.heading", isErrorPage = true)
      )
    }
  }
}
