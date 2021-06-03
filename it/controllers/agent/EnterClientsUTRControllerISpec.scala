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
package controllers.agent

import assets.BaseIntegrationTestConstants._
import assets.BusinessDetailsIntegrationTestConstants.testMtdItId
import assets.IncomeSourceIntegrationTestConstants._
import config.featureswitch.{AgentViewer, FeatureSwitching}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, CitizenDetailsStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

class EnterClientsUTRControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  s"GET ${controllers.agent.routes.EnterClientsUTRController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"redirect ($SEE_OTHER) to ${controllers.errors.routes.AgentErrorController.show().url}" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then(s"The user is redirected to ${controllers.errors.routes.AgentErrorController.show().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.errors.routes.AgentErrorController.show().url)
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the agent viewer feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $OK with the enter client utr page" when {
      "the agent viewer feature switch is enabled" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then("The enter client's utr page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitle("What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
    }
  }

  s"POST ${controllers.agent.routes.EnterClientsUTRController.submit().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the agent viewer feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $BAD_REQUEST" when {
      "no utr is submitted" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
      "an empty utr string is submitted" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(""))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
      "a utr containing non-digits is submitted" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("abc"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
      "a utr which has less than 10 digits is submitted" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("123456789"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
      "a utr which has more than 10 digits is submitted" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("12345678901"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK")
        )
      }
    }
    s"redirect ($SEE_OTHER) to the next page" when {
      "the utr submitted is valid" in {
        val validUTR: String = "1234567890"
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtdItId)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = OK,
          response = CitizenDetailsStub.validCitizenDetailsResponse(
            firstName = "testFirstName",
            lastName = "testLastName",
            nino = testNino
          )
        )
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = OK,
          response = Json.toJson(singleBusinessResponse)
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ConfirmClientUTRController.show().url)
        )
      }
    }

    s"redirect $SEE_OTHER to the UTR Error page" when {
      "the client details could not be found" in {
        val validUTR: String = "1234567890"

        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = NOT_FOUND,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.UTRErrorController.show().url)
        )
      }

      "the business details could not be found" in {
        val validUTR: String = "1234567890"

        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = OK,
          response = CitizenDetailsStub.validCitizenDetailsResponse(
            firstName = "testFirstName",
            lastName = "testLastName",
            nino = testNino
          )
        )
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = NOT_FOUND,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.UTRErrorController.show().url)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR with technical difficulties" when {
      "there was an unexpected response retrieving the client details" in {
        val validUTR: String = "1234567890"

        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = INTERNAL_SERVER_ERROR,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK")
        )
      }
      "there was an unexpected response retrieving the business details" in {
        val validUTR: String = "1234567890"

        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = OK,
          response = CitizenDetailsStub.validCitizenDetailsResponse(
            firstName = "testFirstName",
            lastName = "testLastName",
            nino = testNino
          )
        )
        IncomeTaxViewChangeStub.stubGetBusinessDetails(testNino)(
          status = INTERNAL_SERVER_ERROR,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK")
        )
      }
    }
  }

}
