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

import audit.models.EnterClientUTRAuditModel
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.SessionDataStub.{stubPostSessionDataResponseConflictResponse, stubPostSessionDataResponseFailure, stubPostSessionDataResponseOkResponse}
import helpers.servicemocks.{AuditStub, CitizenDetailsStub, IncomeTaxViewChangeStub}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.testMtdItId
import testConstants.IncomeSourceIntegrationTestConstants._

class EnterClientsUTRControllerISpec extends ComponentSpecBase with FeatureSwitching {

  s"GET ${controllers.agent.routes.EnterClientsUTRController.show.url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
    s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show.url}" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

        Then(s"The user is redirected to ${controllers.agent.errors.routes.AgentErrorController.show.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show.url)
        )
      }
    }
    s"return $OK with the enter client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getEnterClientsUTR

      Then("The enter client's utr page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgentLogin("agent.enter_clients_utr.heading")
      )
    }
  }

  s"POST ${controllers.agent.routes.EnterClientsUTRController.submit.url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
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
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }

    s"return $OK with empty black banner on the enter client utr page" in {
      stubAuthorisedAgentUser(authorised = true)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getConfirmClientUTR(clientDetailsWithoutConfirmation)

      val document: Document = Jsoup.parse(result.toString)
      document.select(".govuk-header__content")
        .select(".hmrc-header__service-name hmrc-header__service-name--linked")
        .text() shouldBe ""

      result should have(
        httpStatus(OK),
        pageTitleAgentLogin("agent.confirmClient.heading")
      )
    }

    s"return $BAD_REQUEST" when {
      "no utr is submitted" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(None)

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "an empty utr string is submitted" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(""))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr containing non-digits is submitted" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("abc"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr which has less than 10 digits is submitted" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("123456789"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr which has more than 10 digits is submitted" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some("12345678901"))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
    }

    s"redirect ($SEE_OTHER) to the next page" when {
      "the utr submitted is valid and session data service post returns an OK response" in {
        val validUTR: String = "1234567890"
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
        Then(s"I stub the session-data service call to return status $OK")
        stubPostSessionDataResponseOkResponse()

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some("1"), saUtr = validUTR, credId = None))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ConfirmClientUTRController.show.url)
        )
      }

      "the utr submitted contains spaces and is valid and session data service post returns an OK response" in {
        val validUTR: String = "1234567890"
        val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

        stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtdItId)

        Then(s"I stub the session-data service call to return status $OK")
        stubPostSessionDataResponseOkResponse()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(utrWithSpaces))

        AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some("1"), saUtr = validUTR, credId = None))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ConfirmClientUTRController.show.url)
        )
      }

      "the utr submitted contains spaces and is valid and session data service post returns a CONFLICT response" in {
        val validUTR: String = "1234567890"
        val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

        stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtdItId)

        Then(s"I stub the session-data service call to return status $CONFLICT")
        stubPostSessionDataResponseConflictResponse()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(utrWithSpaces))

        AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some("1"), saUtr = validUTR, credId = None))

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ConfirmClientUTRController.show.url)
        )
      }
    }

    s"redirect $SEE_OTHER to the UTR Error page" when {
      "the client details could not be found" in {
        val validUTR: String = "1234567890"

        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = NOT_FOUND,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.UTRErrorController.show.url)
        )
      }

      "the business details could not be found" in {
        val validUTR: String = "1234567890"

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
          redirectURI(controllers.agent.routes.UTRErrorController.show.url)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR ISE page" when {
      "there was an unexpected response retrieving the client details" in {
        val validUTR: String = "1234567890"

        stubAuthorisedAgentUser(authorised = true)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR)(
          status = INTERNAL_SERVER_ERROR,
          response = Json.obj()
        )

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"ISE is shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleInternalServer, isErrorPage = true)
        )
      }
      "there was an unexpected response retrieving the business details" in {
        val validUTR: String = "1234567890"

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

        Then(s"ISE is shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleInternalServer, isErrorPage = true)
        )
      }
      "the income tax session data post call returned an error" in {
        val validUTR: String = "1234567890"
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

        Then(s"I stub the session-data service call to return status $INTERNAL_SERVER_ERROR")
        stubPostSessionDataResponseFailure()

        val result: WSResponse = IncomeTaxViewChangeFrontend.postEnterClientsUTR(Some(validUTR))

        Then(s"ISE is shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleInternalServer, isErrorPage = true)
        )
      }
      "there was a failed future when posting data to the income tax session data service" in {
        val validUTR: String = "1234567890"
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

        Then(s"ISE is shown with status $INTERNAL_SERVER_ERROR")
        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleInternalServer, isErrorPage = true)
        )
      }
    }
  }
}
