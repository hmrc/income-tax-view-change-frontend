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
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.{titleInternalServer, titleClientRelationshipFailure}
import play.api.http.Status._
import play.api.libs.ws.WSResponse

class ClientDetailsFailureControllerISpec extends ComponentSpecBase with FeatureSwitching {

  s"GET ${controllers.agent.routes.ClientRelationshipFailureController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getClientRelationshipFailure

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getClientRelationshipFailure

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }
    s"return $OK with the enter client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getClientRelationshipFailure

      Then("The client relationship failure page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent(titleClientRelationshipFailure, isErrorPage = true)
      )
    }
  }
}
