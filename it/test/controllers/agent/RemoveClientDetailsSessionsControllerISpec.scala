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

import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class RemoveClientDetailsSessionsControllerISpec extends ControllerISpecHelper {

  val path = "/agents/remove-client-sessions"

  s"GET ${controllers.agent.routes.EnterClientsUTRController.show.url}" when {
    val isSupportingAgent = false
    val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
    "Removing the client details session keys" should {
      "redirect to client UTR page" in {
        MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = buildMTDClient(path, additionalCookies).futureValue

        val removedSessionKeys: List[String] =
          List(
            "SessionKeys.clientLastName",
            "SessionKeys.clientFirstName",
            "SessionKeys.clientNino",
            "SessionKeys.clientUTR",
            "SessionKeys.confirmedClient"
          )

        removedSessionKeys.foreach(key => result.cookie(key) shouldBe None)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
        )

      }
    }
    testAuthFailuresForMTDAgent(path, isSupportingAgent, false)
  }

}
