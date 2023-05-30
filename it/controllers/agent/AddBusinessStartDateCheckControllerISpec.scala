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

package controllers.agent

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class AddBusinessStartDateCheckControllerISpec extends ComponentSpecBase {
  val addBusinessStartDateCheckShowUrl: String = controllers.routes.AddBusinessStartDateCheckController.submitAgent().url
  val addBusinessTradeShowUrl: String = controllers.routes.AddBusinessTradeController.showAgent().url
  val addBusinessStartDateShowUrl: String = controllers.routes.AddBusinessStartDateController.showAgent().url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.routes.AddBusinessStartDateCheckController.submitAgent().url
  val continueButtonText: String = messagesAPI("base.continue")
  val prefix: String = "add-business-start-date-check"
  val csrfToken: String = "csrfToken"

  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(testDate)(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$prefix.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckSubmitUrl" should {
    s"redirect to $addBusinessTradeShowUrl" when {
      "form is filled correctly" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("Yes"), testDate)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessTradeShowUrl)
        )
      }
    }
    s"redirect to $addBusinessStartDateShowUrl" when {
      "form response is No" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("No"), testDate)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateShowUrl)
        )
      }
    }
    "return a BAD_REQUEST" when {
      "form is filled incorrectly" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val testDate = "2022-01-01"

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(None, testDate)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$prefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$prefix.radio.error"))
        )
      }
    }
  }
}
