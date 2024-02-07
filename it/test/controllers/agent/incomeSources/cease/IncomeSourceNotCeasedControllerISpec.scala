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

package controllers.agent.incomeSources.cease


import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class IncomeSourceNotCeasedControllerISpec extends ComponentSpecBase {

  "The IncomeSourceNotCeasedController.show - Agent" should {
    "200 OK" when {
      "agent is authorised with a business income source type in the request" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.get(uri = "/income-sources/cease/error-business-not-ceased", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("standardError.heading", isErrorPage = true)
        )
      }
      "agent is authorised with a UK property income source type in the request" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.get(uri = "/income-sources/cease/error-uk-property-not-ceased", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("standardError.heading", isErrorPage = true)
        )
      }
      "agent is authorised with a foreign property income source type in the request" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.get(uri = "/income-sources/cease/error-foreign-property-not-ceased", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("standardError.heading", isErrorPage = true)
        )
      }
    }
  }
}
