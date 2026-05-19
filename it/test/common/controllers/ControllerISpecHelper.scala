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

package common.controllers

import common.config.FrontendAppConfig
import common.controllers.agent.errors.routes as agentErrorRoutes
import common.controllers.agent.routes as agentRoutes
import common.controllers.errors.routes as errorRoutes
import common.helpers.ComponentSpecBase
import common.helpers.servicemocks.BusinessDetailsStub.stubGetBusinessDetails
import common.helpers.servicemocks.CitizenDetailsStub.stubGetCitizenDetails
import common.helpers.servicemocks.FeatureSwitchStub.stubGetFeatureSwitches
import common.helpers.servicemocks.{AuditStub, MTDAgentAuthStub, MTDIndividualAuthStub, SessionDataStub}
import common.models.audit.AccessDeniedForSupportingAgentAuditModel
import common.viewUtils.InternalUrlHelper
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import models.admin.FeatureSwitchName
import models.extensions.FinancialDetailsModelExtension
import play.api.http.Status.{SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.getAgentClientDetailsForCookie

trait ControllerISpecHelper extends ComponentSpecBase with FinancialDetailsModelExtension {

  val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  override val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def homeUrl(mtdUserRole: MTDUserRole): String = mtdUserRole match {
    case MTDIndividual => hub.controllers.routes.HomeController.show().url
    case _ => hub.controllers.routes.HomeController.showAgent().url
  }

  def stubAuthorised(mtdRole: MTDUserRole, featureSwitches: List[FeatureSwitchName] = List()): Unit = {
    if(mtdRole != MTDIndividual) {
      SessionDataStub.stubGetSessionDataResponseSuccess()
      stubGetCitizenDetails()
      stubGetBusinessDetails()()
    }
    stubAuthCalls(mtdRole)
    stubGetFeatureSwitches(featureSwitches)
  }

  def stubAuthCalls(mtdUserRole: MTDUserRole): Unit = mtdUserRole match {
    case MTDIndividual => MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
    case MTDPrimaryAgent => MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(false)
    case _ => MTDAgentAuthStub.stubAuthorisedAndMTDEnrolled(true)
  }

  def getAdditionalCookies(mtdUserRole: MTDUserRole, requiresConfirmedClient: Boolean = true) = mtdUserRole match {
    case MTDIndividual => Map.empty[String, String]
    case MTDPrimaryAgent => getAgentClientDetailsForCookie(false, requiresConfirmedClient)
    case _ => getAgentClientDetailsForCookie(true, requiresConfirmedClient)
  }


  def testNoClientDataFailure(requestPath: String, optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    "the user does not have client session data" should {
      s"redirect ($SEE_OTHER) to ${hub.controllers.agent.routes.EnterClientsUTRController.show().url}" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        SessionDataStub.stubGetSessionDataResponseNotFound()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(hub.controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }

    "the user has client session data but citizen details not found" should {
      s"redirect ($SEE_OTHER) to ${hub.controllers.agent.routes.EnterClientsUTRController.show().url}" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        SessionDataStub.stubGetSessionDataResponseSuccess()
        stubGetCitizenDetails(status = 404)
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(hub.controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }
  }

  def testAuthFailures(requestPath: String,
                                  mtdUserRole: MTDUserRole,
                                  optBody: Option[Map[String, Seq[String]]] = None,
                                  requiresConfirmedClient: Boolean = true): Unit = {
    val isAgent = mtdUserRole != MTDIndividual
    val mtdAuthStub = if(!isAgent) MTDIndividualAuthStub else MTDAgentAuthStub
    val additionalCookies = getAdditionalCookies(mtdUserRole, requiresConfirmedClient)

    if(mtdUserRole != MTDSupportingAgent) {
      "does not have a valid session" should {
        s"redirect ($SEE_OTHER) to ${InternalUrlHelper.signinUrl}" in {
          if (mtdUserRole != MTDIndividual) {
            SessionDataStub.stubGetSessionDataResponseSuccess()
            stubGetCitizenDetails()
            stubGetBusinessDetails()()
          }
          mtdAuthStub.stubUnauthorised()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(InternalUrlHelper.signinUrl)
          )
        }
      }

      "has an expired bearerToken" should {
        s"redirect ($SEE_OTHER) to ${InternalUrlHelper.timeoutUrl}" in {
          if (mtdUserRole != MTDIndividual) {
            SessionDataStub.stubGetSessionDataResponseSuccess()
            stubGetCitizenDetails()
            stubGetBusinessDetails()()
          }
          mtdAuthStub.stubBearerTokenExpired()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(InternalUrlHelper.timeoutUrl)
          )
        }
      }
    }

    if(isAgent) {
      testAgentAuthFailures(requestPath, additionalCookies, optBody, requiresConfirmedClient, mtdUserRole)
    } else {
      testIndividualAuthFailures(requestPath, optBody)
    }
  }

  def testIndividualAuthFailures(requestPath: String,
                                 optBody: Option[Map[String, Seq[String]]]): Unit = {
    "does not have HMRC-MTD-IT enrolment" should {
      s"redirect ($SEE_OTHER) to ${errorRoutes.NotEnrolledController.show().url}" in {
        MTDIndividualAuthStub.stubInsufficientEnrolments()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorRoutes.NotEnrolledController.show().url)
        )
      }
    }

    "does not have the required confidence level" should {
      s"redirect ($SEE_OTHER) to IV uplift" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled(Some(50))
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
          redirectURI(hub.controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }
  }

  def testAgentAuthFailures(requestPath: String,
                            additionalCookies: Map[String, String],
                            optBody: Option[Map[String, Seq[String]]],
                            requiresConfirmedClient: Boolean,
                            mtdUserRole: MTDUserRole): Unit = {
    if (mtdUserRole == MTDPrimaryAgent) {
      "does not have arn enrolment" should {
        s"redirect ($SEE_OTHER) to ${agentErrorRoutes.AgentErrorController.show().url}" in {
          SessionDataStub.stubGetSessionDataResponseSuccess()
          stubGetCitizenDetails()
          stubGetBusinessDetails()()

          MTDAgentAuthStub.stubNoAgentEnrolmentError()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(agentErrorRoutes.AgentErrorController.show().url)
          )
        }
      }

      "is not an agent" should {
        "redirect to the home controller" in {
          SessionDataStub.stubGetSessionDataResponseSuccess()
          stubGetCitizenDetails()
          stubGetBusinessDetails()()
          MTDAgentAuthStub.stubNotAnAgent()

          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(hub.controllers.routes.HomeController.show().url)
          )
        }
      }
      if (requiresConfirmedClient) {
        testNoClientDataFailure(requestPath, optBody)
      }
    } else {
      "does not have a valid delegated MTD enrolment" should {
        s"redirect ($SEE_OTHER) to ${agentRoutes.ClientRelationshipFailureController.show().url}" in {
          SessionDataStub.stubGetSessionDataResponseSuccess()
          stubGetCitizenDetails()
          stubGetBusinessDetails()()
          MTDAgentAuthStub.stubAuthorisedButNotMTDEnrolled()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(agentRoutes.ClientRelationshipFailureController.show().url)
          )
        }
      }
    }
  }

  def testSupportingAgentAccessDenied(requestPath: String,
                                      additionalCookies: Map[String, String],
                                      optBody: Option[Map[String, Seq[String]]] = None): Unit = {
    "render the supporting agent unauthorised page" in {
      stubAuthorised(MTDSupportingAgent)

      whenReady(buildMTDClient(requestPath, additionalCookies, optBody)) { result =>
        result should have(
          httpStatus(UNAUTHORIZED),
          pageTitle(MTDSupportingAgent, "agent-unauthorised.heading", isErrorPage = true)
        )
        AuditStub.verifyAuditEvent(AccessDeniedForSupportingAgentAuditModel(getAuthorisedAndEnrolledUser(MTDSupportingAgent)))
      }
    }
  }
}
