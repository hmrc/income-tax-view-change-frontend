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
import forms.utils.SessionKeys.addUkPropertyStartDate
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class CheckUKPropertyStartDateControllerISpec extends ComponentSpecBase {
  val checkUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent().url
  val checkUKPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submitAgent().url
  val addUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddUKPropertyStartDateController.showAgent().url
  val ukPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.UKPropertyAccountingMethod.showAgent().url

  val dateCookie: Map[String, String] = Map(addUkPropertyStartDate -> "2022-10-10")
  val dateText: String = "10 October 2022"
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $checkUKPropertyStartDateShowUrl" should {
    "render the Check UK Property Business Start Date page" when {
      "Agent is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkUKPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-start-date-check",
          dateCookie ++ clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("radioForm.checkDate.heading"),
          elementTextBySelector("#check-uk-property-start-date > p")(dateText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $checkUKPropertyStartDateSubmitUrl" should {
    s"redirect to $ukPropertyAccountingMethodShowUrl" when {
      "agent selects 'yes' the date entered is correct" in {
        val formData: Map[String, Seq[String]] = Map("check-uk-property-start-date" -> Seq("yes"))

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-start-date-check",
          dateCookie ++ clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(ukPropertyAccountingMethodShowUrl)
        )
      }
      s"redirect to $addUKPropertyStartDateShowUrl" when {
        "agent selects 'no' the date entered is not correct" in {
          val formData: Map[String, Seq[String]] = Map("check-uk-property-start-date" -> Seq("no"))

          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-start-date-check",
            dateCookie ++ clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(addUKPropertyStartDateShowUrl)
          )
        }
      }
      s"return BAD_REQUEST $checkUKPropertyStartDateShowUrl" when {
        "agent does not select anything" in {
          val formData: Map[String, Seq[String]] = Map("check-uk-property-start-date" -> Seq(""))

          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-start-date-check",
            dateCookie ++ clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("check-uk-property-start-date-error")(messagesAPI("base.error-prefix") + " " +
              messagesAPI("incomeSources.add.checkUKPropertyStartDate.error.required"))
          )
        }
      }
    }
  }
}