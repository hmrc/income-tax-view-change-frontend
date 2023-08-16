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
import enums.IncomeSourceJourney.ForeignProperty
import forms.utils.SessionKeys.foreignPropertyStartDate
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class ForeignPropertyStartDateCheckControllerISpec extends ComponentSpecBase {
  val foreignPropertyStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent().url
  val foreignPropertyStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.submitAgent().url
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty.key).url
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url

  val dateCookie: Map[String, String] = Map(foreignPropertyStartDate -> "2022-10-10")
  val dateText: String = "10 October 2022"
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $foreignPropertyStartDateCheckShowUrl" should {
    "render the foreign property start date check page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateCheckShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date-check", clientDetailsWithConfirmation ++ dateCookie)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.foreignProperty.startDate.check.heading"),
          elementTextByID("foreign-property-start-date-check-hint")(dateText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateCheckSubmitUrl" should {
    s"redirect to $foreignPropertyAccountingMethodShowUrl" when {
      "form is filled correctly with input Yes" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-start-date-check" -> Seq("Yes"))}
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date-check", clientDetailsWithConfirmation ++ dateCookie)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyAccountingMethodShowUrl)
        )
      }
      "form is filled correctly with input No" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-start-date-check" -> Seq("No"))
        }
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date-check", clientDetailsWithConfirmation ++ dateCookie)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateShowUrl)
        )
      }
      "form is filled incorrectly" in {

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date-check", clientDetailsWithConfirmation ++ dateCookie)(Map.empty)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("foreign-property-start-date-check-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.add.foreignProperty.startDate.check.error"))
        )
      }
    }
  }
}