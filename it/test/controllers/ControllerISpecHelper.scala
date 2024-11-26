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

import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import helpers.ComponentSpecBase
import helpers.servicemocks._
import play.api.http.Status.SEE_OTHER
import testConstants.BaseIntegrationTestConstants.getAgentClientDetailsForCookie

trait ControllerISpecHelper extends ComponentSpecBase {

  override val haveDefaultAuthMocks: Boolean = false

  val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  def homeUrl(mtdUserRole: MTDUserRole): String = mtdUserRole match {
    case MTDIndividual => controllers.routes.HomeController.show().url
    case _ => controllers.routes.HomeController.showAgent.url
  }

  def stubAuthorised(mtdRole: MTDUserRole): Unit = {
    getMTDAuthStub(mtdRole).stubAuthorised()
  }

  def getMTDAuthStub(mtdUserRole: MTDUserRole): MTDAuthStub = mtdUserRole match {
    case MTDIndividual => MTDIndividualAuthStub
    case MTDPrimaryAgent => MTDPrimaryAgentAuthStub
    case _ => MTDSupportingAgentAuthStub
  }

  def getAdditionalCookies(mtdUserRole: MTDUserRole, requiresConfirmedClient: Boolean = true) = mtdUserRole match {
    case MTDIndividual => Map.empty[String, String]
    case MTDPrimaryAgent => getAgentClientDetailsForCookie(false, requiresConfirmedClient)
    case _ => getAgentClientDetailsForCookie(true, requiresConfirmedClient)
  }


  def testNoClientDataFailure(requestPath: String, optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    "the user does not have client session data" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show.url}" in {
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
        )
      }
    }
  }

  def testAuthFailures(requestPath: String,
                                  mtdUserRole: MTDUserRole,
                                  optBody: Option[Map[String, Seq[String]]] = None,
                                  requiresConfirmedClient: Boolean = true): Unit = {
    val mtdAuthStub = getMTDAuthStub(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole, requiresConfirmedClient)

    "does not have a valid session" should {
      s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" in {
        mtdAuthStub.stubUnauthorised()
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }

    "has an expired bearerToken" should {
      s"redirect ($SEE_OTHER) to ${controllers.timeout.routes.SessionTimeoutController.timeout.url}" in {
        mtdAuthStub.stubBearerTokenExpired()
        val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.timeout.routes.SessionTimeoutController.timeout.url)
        )
      }
    }

    mtdAuthStub match {
      case authStub: MTDAgentAuthStub =>
        "does not have arn enrolment" should {
          s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show.url}" in {
            authStub.stubNoAgentEnrolmentError()
            val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.errors.routes.AgentErrorController.show.url)
            )
          }
        }

        "does not have a valid delegated MTD enrolment" should {
          s"redirect ($SEE_OTHER) to ${controllers.agent.routes.ClientRelationshipFailureController.show.url}" in {
            authStub.stubMissingDelegatedEnrolment()
            val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.ClientRelationshipFailureController.show.url)
            )
          }
        }

        "is not an agent" should {
          "redirect to the home controller" in {
            authStub.stubNotAnAgent()

            val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.routes.HomeController.show().url)
            )
          }
        }

      case _ =>
        "does not have HMRC-MTD-IT enrolment" should {
          s"redirect ($SEE_OTHER) to ${controllers.errors.routes.NotEnrolledController.show.url}" in {
            MTDIndividualAuthStub.stubInsufficientEnrolments()
            val result = buildMTDClient(requestPath, optBody = optBody).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.errors.routes.NotEnrolledController.show.url)
            )
          }
        }

        "does not have the required confidence level" should {
          s"redirect ($SEE_OTHER) to IV uplift" in {
            MTDIndividualAuthStub.stubAuthorised(Some(50))
            val result = buildMTDClient(requestPath, optBody = optBody).futureValue

            result.status shouldBe SEE_OTHER
            result.header("Location").get should include("/iv-stub")
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
}
