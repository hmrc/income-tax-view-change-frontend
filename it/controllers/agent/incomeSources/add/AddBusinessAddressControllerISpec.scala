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

package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ComponentSpecBase {

  val changeBusinessAddressShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = true).url
  val businessAddressShowAgentUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url

  s"calling GET $businessAddressShowAgentUrl" should {
    "render the add business address page" when {
      "Agent is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("address lookup service returns an ACCEPTED (202) HTTP status and has a location in its header")
        AddressLookupStub.stubPostInitialiseAddressLookup()

        When(s"I call GET $businessAddressShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessAddress

        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"calling GET $changeBusinessAddressShowAgentUrl" should {
    "render the change business address page" when {
      "Agent is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("address lookup service returns an ACCEPTED (202) HTTP status and has a location in its header")
        AddressLookupStub.stubPostInitialiseAddressLookup()

        When(s"I call GET $changeBusinessAddressShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getAddChangeBusinessAddress

        result should have(
          httpStatus(444),
        )
      }
    }
  }
}
