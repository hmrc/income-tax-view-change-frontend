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

import audit.models.AccessDeniedForSupportingAgentAuditModel
import config.FrontendAppConfig
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import helpers.ComponentSpecBase
import helpers.servicemocks.BusinessDetailsStub.stubGetBusinessDetails
import helpers.servicemocks.CitizenDetailsStub.stubGetCitizenDetails
import helpers.servicemocks._
import models.admin.FeatureSwitchName
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.extensions.FinancialDetailsModelExtension
import play.api.http.Status.{SEE_OTHER, UNAUTHORIZED}
import testConstants.BaseIntegrationTestConstants.getAgentClientDetailsForCookie
import testOnly.repository.FeatureSwitchRepository

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait ControllerISpecHelper extends ComponentSpecBase with FinancialDetailsModelExtension {

  val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  val featureSwitchRepository = app.injector.instanceOf[FeatureSwitchRepository]

  override val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def homeUrl(mtdUserRole: MTDUserRole): String = mtdUserRole match {
    case MTDIndividual => controllers.routes.HomeController.show().url
    case _ => controllers.routes.HomeController.showAgent().url
  }

  def stubAuthorised(mtdRole: MTDUserRole): Unit = {
    if(mtdRole != MTDIndividual) {
      SessionDataStub.stubGetSessionDataResponseSuccess()
      stubGetCitizenDetails()
      stubGetBusinessDetails()()
    }
    stubAuthCalls(mtdRole)
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
      s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        SessionDataStub.stubGetSessionDataResponseNotFound()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
        )
      }
    }

    "the user has client session data but citizen details not found" should {
      s"redirect ($SEE_OTHER) to ${controllers.agent.routes.EnterClientsUTRController.show().url}" in {
        MTDAgentAuthStub.stubAuthorisedWithAgentEnrolment()
        SessionDataStub.stubGetSessionDataResponseSuccess()
        stubGetCitizenDetails(status = 404)
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
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
        s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" in {
          if (mtdUserRole != MTDIndividual) {
            SessionDataStub.stubGetSessionDataResponseSuccess()
            stubGetCitizenDetails()
            stubGetBusinessDetails()()
          }
          mtdAuthStub.stubUnauthorised()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.SignInController.signIn().url)
          )
        }
      }

      "has an expired bearerToken" should {
        s"redirect ($SEE_OTHER) to ${controllers.timeout.routes.SessionTimeoutController.timeout().url}" in {
          if (mtdUserRole != MTDIndividual) {
            SessionDataStub.stubGetSessionDataResponseSuccess()
            stubGetCitizenDetails()
            stubGetBusinessDetails()()
          }
          mtdAuthStub.stubBearerTokenExpired()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.timeout.routes.SessionTimeoutController.timeout().url)
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
      s"redirect ($SEE_OTHER) to ${controllers.errors.routes.NotEnrolledController.show().url}" in {
        MTDIndividualAuthStub.stubInsufficientEnrolments()
        val result = buildMTDClient(requestPath, optBody = optBody).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.errors.routes.NotEnrolledController.show().url)
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
          redirectURI(controllers.agent.routes.EnterClientsUTRController.show().url)
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
        s"redirect ($SEE_OTHER) to ${controllers.agent.errors.routes.AgentErrorController.show().url}" in {
          SessionDataStub.stubGetSessionDataResponseSuccess()
          stubGetCitizenDetails()
          stubGetBusinessDetails()()

          MTDAgentAuthStub.stubNoAgentEnrolmentError()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.errors.routes.AgentErrorController.show().url)
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
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
      if (requiresConfirmedClient) {
        testNoClientDataFailure(requestPath, optBody)
      }
    } else {
      "does not have a valid delegated MTD enrolment" should {
        s"redirect ($SEE_OTHER) to ${controllers.agent.routes.ClientRelationshipFailureController.show().url}" in {
          SessionDataStub.stubGetSessionDataResponseSuccess()
          stubGetCitizenDetails()
          stubGetBusinessDetails()()
          MTDAgentAuthStub.stubAuthorisedButNotMTDEnrolled()
          val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.ClientRelationshipFailureController.show().url)
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
      val result = buildMTDClient(requestPath, additionalCookies, optBody).futureValue
      result should have(
        httpStatus(UNAUTHORIZED),
        pageTitle(MTDSupportingAgent, "agent-unauthorised.heading", isErrorPage = true)
      )
      AuditStub.verifyAuditEvent(AccessDeniedForSupportingAgentAuditModel(getAuthorisedAndEnrolledUser(MTDSupportingAgent)))
    }
  }

  def disableAllSwitches(): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitches(allFeatureSwitches.map(_ -> false).toMap), 5.seconds)
    else
      allFeatureSwitches.foreach(switch => disable(switch))

  override def enable(featureSwitch: FeatureSwitchName): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitch(featureSwitch, true), 5.seconds)
    else
      sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  override def disable(featureSwitch: FeatureSwitchName): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitch(featureSwitch, false), 5.seconds)
    else
      sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF

}
