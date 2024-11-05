/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import audit.models.IvUpliftRequiredAuditModel
import helpers.ComponentSpecBase
import helpers.servicemocks.MTDIndividualAuthStub.requiredConfidenceLevel
import helpers.servicemocks.{AuditStub, MTDIndividualAuthStub}
import play.api.http.Status.SEE_OTHER

trait ControllerISpecHelper extends ComponentSpecBase {

  def testAuthFailuresForMTDIndividual(requestPath: String,
                                  optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    "does not have a valid session" should {
      s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" in {
        MTDIndividualAuthStub.stubUnauthorised()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }

    "has an expired bearerToken" should {
      s"redirect ($SEE_OTHER) to ${controllers.timeout.routes.SessionTimeoutController.timeout.url}" in {
        MTDIndividualAuthStub.stubBearerTokenExpired()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        )
      }
    }

    "does not have HMRC-MTD-IT enrolment" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show.url}" in {
        MTDIndividualAuthStub.stubInsufficientEnrolments()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentErrorController.show.url)
        )
      }
    }

    "does not have the required confidence level" should {
      s"redirect ($SEE_OTHER) to IV uplift" in {
        MTDIndividualAuthStub.stubAuthorised(Some(50))
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result.status shouldBe SEE_OTHER
        result.header("Location").get should include("http://stubIV.com")
        AuditStub.verifyAuditEvent(IvUpliftRequiredAuditModel("Individual", 50, requiredConfidenceLevel))
      }
    }

    "is an agent" should {
      "redirect to the Enter clients UTR controller" in {
        MTDIndividualAuthStub.stubAuthorisedButAgent()

        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
        )
      }
    }
  }

}
