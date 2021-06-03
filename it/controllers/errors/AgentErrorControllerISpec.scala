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

import helpers.ComponentSpecBase
import play.api.http.Status._

class AgentErrorControllerISpec extends ComponentSpecBase {

  val agentErrorUri: String = "/agent-error"

  "Calling the AgentErrorController.show()" when {

    unauthorisedTest(agentErrorUri)

    "user is authorised" should {
      "respond with the correct page" in {
        When(s"I call GET $agentErrorUri")
        val res = IncomeTaxViewChangeFrontend.get(agentErrorUri)

        Then("I can see the correct page")
        res should have(
          httpStatus(OK),
          pageTitle("You can't use this service yet - Business Tax account - GOV.UK")
        )
      }
    }

  }
}
