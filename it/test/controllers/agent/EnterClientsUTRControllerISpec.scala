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
import controllers.ControllerISpecHelper
import enums.MTDPrimaryAgent
import helpers.servicemocks._
import play.api.http.Status._
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.testMtdItId
import testConstants.IncomeSourceIntegrationTestConstants._

class EnterClientsUTRControllerISpec extends ControllerISpecHelper {
  
  val path = "/agents/client-utr"

  s"GET $path" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        MTDAgentAuthStub.stubUnauthorised()

        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show().url}" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentError()
        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
        )
      }
    }
    s"return $OK with the enter client utr page" in {
      MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
      val result = buildGETMTDClient(path).futureValue

      result should have(
        httpStatus(OK),
        pageTitle(MTDPrimaryAgent, "agent.enter_clients_utr.heading")
      )
    }
  }

  s"POST ${controllers.agent.routes.EnterClientsUTRController.submit.url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        MTDAgentAuthStub.stubUnauthorised()

        val result = buildPOSTMTDPostClient(path, body = Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"redirect to agent error page" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        MTDAgentAuthStub.stubNoAgentEnrolmentError()

        val result = buildPOSTMTDPostClient(path, body = Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
        )
      }
    }

    s"return $BAD_REQUEST" when {
      "no utr is submitted" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildPOSTMTDPostClient(path, body = Map.empty).futureValue

        Then("The enter clients utr page is returned with an error")
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "an empty utr string is submitted" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(""))).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr containing non-digits is submitted" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildPOSTMTDPostClient(path, body = Map("utr" -> Seq("abc"))).futureValue
        
        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr which has less than 10 digits is submitted" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildPOSTMTDPostClient(path, body = Map("utr" -> Seq("123456789"))).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
      "a utr which has more than 10 digits is submitted" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()

        val result = buildPOSTMTDPostClient(path, body = Map("utr" -> Seq("12345678901"))).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("agent.enter_clients_utr.heading", isInvalidInput = true)
        )
      }
    }

    s"redirect ($SEE_OTHER) to the next page" when {
      "the utr is submitted by a primary agent is valid" in {
        val validUTR: String = "1234567890"
        MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(false)
        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(validUTR)))) { result  =>
          AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some(testArn), saUtr = validUTR, credId = Some(credId), Some(false)))

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.ConfirmClientUTRController.show().url)
          )
        }
      }

      "the utr submitted by a secondary agent is valid" in {
        val validUTR: String = "1234567890"
        MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(true)

        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(validUTR)))) { result =>

          AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some(testArn), saUtr = validUTR, credId = Some(credId), Some(true)))

          Then("The enter clients utr page is returned with an error")
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.ConfirmClientUTRController.show().url)
          )
        }
      }

      "the utr submitted by a primary agent contains spaces and is valid" in {
        val validUTR: String = "1234567890"
        val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

        MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(false)

        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(utrWithSpaces)))) { result =>
          AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some(testArn), saUtr = validUTR, credId = Some(credId), Some(false)))

          Then("The enter clients utr page is returned with an error")
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.ConfirmClientUTRController.show().url)
          )
        }
      }

      "the utr submitted by a secondary agent contains spaces and is valid" in {
        val validUTR: String = "1234567890"
        val utrWithSpaces: String = " 1 2 3 4 5 6 7 8 9 0 "

        MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(true)

        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(utrWithSpaces)))) { result =>
          AuditStub.verifyAuditEvent(EnterClientUTRAuditModel(isSuccessful = true, nino = testNino, mtditid = testMtdItId, arn = Some(testArn), saUtr = validUTR, credId = Some(credId), Some(true)))

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.ConfirmClientUTRController.show().url)
          )
        }
      }
    }

    s"redirect $SEE_OTHER to the UTR Error page" when {
      "the client details could not be found" in {
        val validUTR: String = "1234567890"

        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
          status = NOT_FOUND,
          response = Json.obj()
        )

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(validUTR)))) { result =>
          Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.UTRErrorController.show().url)
          )
        }
      }

      "the business details could not be found" in {
        val validUTR: String = "1234567890"

        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(validUTR)))) { result =>
          Then(s"Technical difficulties are shown with status $INTERNAL_SERVER_ERROR")
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.UTRErrorController.show().url)
          )
        }
      }

      "the primary or secondary agent enrolment is not present" in {
        val validUTR: String = "1234567890"

        MTDAgentAuthStub.stubAuthorisedButNotMTDEnrolled()
        CitizenDetailsStub.stubGetCitizenDetails(validUTR,
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

        whenReady(buildPOSTMTDPostClient(path, body = Map("utr" -> Seq(validUTR)))) { result =>
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.UTRErrorController.show().url)
          )
        }
      }
    }
  }
}
