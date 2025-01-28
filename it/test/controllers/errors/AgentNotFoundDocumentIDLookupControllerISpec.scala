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

package controllers.errors

import controllers.ControllerISpecHelper
import helpers.servicemocks.MTDAgentAuthStub
import play.api.http.Status._

class AgentNotFoundDocumentIDLookupControllerISpec extends ControllerISpecHelper {

  val agentErrorUri: String = "/agents/custom-not-found"

  "Calling the AgentNotFoundDocumentIDLookupController.show()" when {

    "user is authorised" should {
      "respond with the correct page" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentRequiredSuccess()

        val res = buildGETMTDClient(agentErrorUri).futureValue

        res should have(
          httpStatus(OK),
          pageTitleAgent("error.custom.heading")
        )
      }
    }
  }
}
