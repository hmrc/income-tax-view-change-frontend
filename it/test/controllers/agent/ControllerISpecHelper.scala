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

import helpers.agent.ComponentSpecBase
import helpers.servicemocks.MTDAgentAuthStub
import play.api.http.Status.SEE_OTHER
import testConstants.BaseIntegrationTestConstants._

trait ControllerISpecHelper extends ComponentSpecBase {

  def testNoClientDataFailure(requestPath: String, optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    "the user does not have client session data" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show.url}" in {
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
        )
      }
    }
  }

  def testAuthFailuresForMTDAgent(requestPath: String,
                                  isSupportingAgent: Boolean,
                                  requiresConfirmedClient: Boolean = true,
                                  optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, requiresConfirmedClient)

    "does not have a valid session" should {
      s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" in {
        MTDAgentAuthStub.stubUnauthorised(testMtditid, isSupportingAgent = isSupportingAgent)
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }

    "has an expired bearerToken" should {
      s"redirect ($SEE_OTHER) to ${controllers.timeout.routes.SessionTimeoutController.timeout.url}" in {
        MTDAgentAuthStub.stubBearerTokenExpired(testMtditid, isSupportingAgent = isSupportingAgent)
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        )
      }
    }

    "does not have arn enrolment" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show.url}" in {
        MTDAgentAuthStub.stubNoAgentEnrolment(testMtditid, isSupportingAgent = isSupportingAgent)
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show.url)
        )
      }
    }

    "does not have a valid delegated MTD enrolment" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.routes.ClientRelationshipFailureController.show.url}" in {
        MTDAgentAuthStub.stubMissingDelegatedEnrolment(testMtditid, isSupportingAgent = isSupportingAgent)
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ClientRelationshipFailureController.show.url)
        )
      }
    }

    "is not an agent" should {
      "redirect to the home controller" in {
        MTDAgentAuthStub.stubNotAnAgent(testMtditid, isSupportingAgent)

        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
  }

}
