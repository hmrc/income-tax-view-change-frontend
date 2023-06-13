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

class AddBusinessStartDateControllerISpec extends ComponentSpecBase {
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddBusinessStartDateController.showAgent().url
  val addBusinessStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessStartDateController.submitAgent().url
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.showAgent().url
  val prefix: String = "add-business-start-date"
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $addBusinessStartDateShowUrl" should {
    "render the Add Business Start Date Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$prefix.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateSubmitUrl" should {
    s"redirect to $addBusinessStartDateCheckShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            s"$prefix.day" -> Seq("1"),
            s"$prefix.month" -> Seq("1"),
            s"$prefix.year" -> Seq("2022")
          )
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .post("/income-sources/add/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckShowUrl)
        )
      }
    }
    s"return a BAD_REQUEST" when {
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            s"$prefix.day" -> Seq("$"),
            s"$prefix.month" -> Seq("%"),
            s"$prefix.year" -> Seq("&")
          )
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .post("/income-sources/add/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$prefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$prefix.error.invalid"))
        )
      }
    }
  }
}
