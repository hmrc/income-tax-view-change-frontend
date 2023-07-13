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
import forms.utils.SessionKeys.{addUkPropertyAccountingMethod, addUkPropertyStartDate}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourcesResponse
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse


class CheckUKPropertyDetailsControllerISpec extends ComponentSpecBase {
  object CheckUKPropertyDetails {
    val showUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url
    val submitUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submitAgent().url
    val backUrl: String = controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url
    val successUrl: String = controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent().url + "?id=1234567890"
    val completedJourneyCookies: Map[String, String] = Map(addUkPropertyStartDate -> "2022-10-10",
      addUkPropertyAccountingMethod -> "CASH") ++ clientDetailsWithConfirmation
    val changeText: String = messagesAPI("incomeSources.add.checkUKPropertyDetails.change") + " " +
      messagesAPI("incomeSources.add.checkUKPropertyDetails.change") // duplicated due to visually hidden text
    val confirmText: String = messagesAPI("incomeSources.add.checkUKPropertyDetails.confirm")
  }


  s"calling GET ${CheckUKPropertyDetails.showUrl}" should {
    "200 - render the Check UK Property Details page" when {
      "User is authorised and has completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", CheckUKPropertyDetails.completedJourneyCookies)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.checkUKPropertyDetails.heading"),
          elementTextByID("change-start-date-link")(CheckUKPropertyDetails.changeText),
          elementTextByID("change-accounting-method-link")(CheckUKPropertyDetails.changeText),
          elementTextByID("continue-button")(CheckUKPropertyDetails.confirmText)
        )
      }
      "500 ISE" when {
        "User has not completed the Add UK Property journey" in {
          Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)

          result should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
      "303 SEE_OTHER - redirect to home page" when {
        "Income Sources FS disabled" in {
          Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
          stubAuthorisedAgentUser(authorised = true)
          disable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", CheckUKPropertyDetails.completedJourneyCookies)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
          )
        }
      }
    }
  }

  s"calling POST ${CheckUKPropertyDetails.submitUrl}" should {
    "303 SEE_OTHER and redirect to UK Property Reporting Method page" when {
      "User is authorised and has completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        Given("I wiremock stub a successful Create Income Sources (UK Property) response")
        val createResponseJson = List(CreateIncomeSourcesResponse("1234567890"))
        val testBody = Map(
          "ukPropertyDetails.tradingStartDate" -> Seq("2011-01-01"),
          "ukPropertyDetails.cashOrAccrualsFlag" -> Seq("CASH"),
          "ukPropertyDetails.startDate" -> Seq("2011-01-01")
        )

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", CheckUKPropertyDetails.completedJourneyCookies)(testBody)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(CheckUKPropertyDetails.successUrl)
        )
      }
    }
    "500 ISE" when {
      "User has not completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Create Income Sources (UK Property) response")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        val createResponseJson = List(CreateIncomeSourcesResponse("1234567890"))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)

        val testBody = Map(
          "ukPropertyDetails.tradingStartDate" -> Seq("2011-01-01"),
          "ukPropertyDetails.cashOrAccrualsFlag" -> Seq("CASH"),
          "ukPropertyDetails.startDate" -> Seq("2011-01-01")
        )
        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", CheckUKPropertyDetails.completedJourneyCookies)(testBody)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "303 SEE_OTHER and redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val testBody = Map(
          "ukPropertyDetails.tradingStartDate" -> Seq("2011-01-01"),
          "ukPropertyDetails.cashOrAccrualsFlag" -> Seq("CASH"),
          "ukPropertyDetails.startDate" -> Seq("2011-01-01")
        )

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", CheckUKPropertyDetails.completedJourneyCookies)(testBody)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
        )
      }
    }
  }

}
