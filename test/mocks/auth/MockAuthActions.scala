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

package mocks.auth

import audit.AuditingService
import audit.mocks.MockAuditingService
import auth.FrontendAuthorisedFunctions
import authV2.AuthActionsTestData._
import config.featureswitch.FeatureSwitching
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import mocks.services.{MockClientDetailsService, MockIncomeSourceDetailsService, MockSessionDataService}
import org.jsoup.Jsoup
import org.mockito.Mockito.{mock, reset}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.agent.ClientDetailsService
import services.{IncomeSourceDetailsService, SessionDataService}
import testConstants.BaseTestConstants.{testMtditid, testRetrievedUserName}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core._

trait MockAuthActions
  extends TestSupport
    with MockIncomeSourceDetailsService
    with MockAgentAuthorisedFunctions
    with MockUserAuthorisedFunctions
    with MockAuditingService
    with MockSessionDataService
    with MockClientDetailsService
    with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
    reset(mockAuthService)
  }

  override def afterEach() = {
    super.afterEach()
  }

  lazy val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  val mockAuthService: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])

  lazy val applicationBuilderWithAuthBindings: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(mockAuthService),
        api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
        api.inject.bind[SessionDataService].toInstance(mockSessionDataService),
        api.inject.bind[ClientDetailsService].toInstance(mockClientDetailsService)
      )
  }

  def setupMockSuccess(mtdUserRole: MTDUserRole): Unit = mtdUserRole match {
    case MTDIndividual => setupMockUserAuth
    case MTDPrimaryAgent => setupMockAgentWithClientAuth(false)
    case _ => setupMockAgentWithClientAuth(true)
  }

  def setupMockUserAuth: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(hasNino = true, hasSA = true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel
    setupMockUserAuthSuccess(retrievalValue)
  }

  def setupMockUserAuthNoSAUtr: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(hasNino = true, hasSA = false)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel
    setupMockUserAuthSuccess(retrievalValue)
  }

  def setupMockAgentWithClientAuth(isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(retrievalValue, testMtditid, isSupportingAgent)
  }

  def setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(retrievalValue, testMtditid, isSupportingAgent)
    mockSingleBusinessIncomeSource()
  }

  final def setupMockUserAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    setupMockUserAuthException(exception)
  }

  def setupMockAgentWithoutMTDEnrolmentForClient(): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithMissingDelegatedMTDEnrolment(retrievalValue, testMtditid)
  }

  def setupMockAgentSuccess(): Unit = {
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentAuthSuccess(retrievalValue)
  }

  def setupMockAgentWithClientAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    setupMockAgentAuthException(exception)
  }

  def testMTDAuthFailuresForRole(action: Action[AnyContent],
                                 userRole: MTDUserRole,
                                 supportingAgentAccessAllowed: Boolean = true)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    userRole match {
      case MTDIndividual => testMTDAuthFailuresForIndividual(action, userRole)(fakeRequest)
      case _ => testMTDAuthFailuresForAgent(action, userRole, supportingAgentAccessAllowed)(fakeRequest)
    }
  }

  def testMTDIndividualAuthFailures(action: Action[AnyContent]): Unit = {
    testMTDAuthFailuresForIndividual(action, MTDIndividual)(fakeRequestWithActiveSession)
  }

  def testMTDAuthFailuresForIndividual(action: Action[AnyContent], userRole: MTDUserRole)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    s"the $userRole is not authenticated" should {
      "redirect to signin" in {
        setupMockUserAuthorisationException()

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    s"the $userRole has a session that has timed out" should {
      "redirect to timeout controller" in {
        setupMockUserAuthorisationException(new BearerTokenExpired)

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    s"the $userRole is not enrolled into HMRC-MTD-IT" should {
      "redirect to NotEnrolledController controller" in {
        setupMockUserAuthorisationException(InsufficientEnrolments("missing HMRC-MTD-IT enrolment"))

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotEnrolledController.show().url)
      }
    }

    s"the $userRole is not authenticated and enrolled into HMRC-MTD-IT but doesn't have income source" should {
      "render the internal error page" in {
        setupMockUserAuth
        mockErrorIncomeSource()

        val result = action(fakeRequest)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        val errorPage = Jsoup.parse(contentAsString(result))
        errorPage.title shouldEqual "Sorry, there is a problem with the service - GOV.UK"
      }
    }
  }

  def testMTDAgentAuthFailures(action: Action[AnyContent], isSupportingAgent: Boolean): Unit = {
    val fakeRequest = fakeRequestConfirmedClient(isSupportingAgent = isSupportingAgent)
    val mdtUserRole = if (isSupportingAgent) MTDSupportingAgent else MTDPrimaryAgent
    testMTDAuthFailuresForAgent(action, mdtUserRole, true)(fakeRequest)
  }

  def testMTDAuthFailuresForAgent(action: Action[AnyContent],
                                  mtdUserRole: MTDUserRole,
                                  supportingAgentAccessAllowed: Boolean)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val userType = if(isSupportingAgent) "supporting agent" else "primary agent"
    if(mtdUserRole == MTDPrimaryAgent) {
      s"the agent is not authenticated" should {
        "redirect to signin" in {
          setupMockGetSessionDataSuccess()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(new InvalidBearerToken)

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
        }
      }

      s"the agent has a session that has timed out" should {
        "redirect to timeout controller" in {
          setupMockGetSessionDataSuccess()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(new BearerTokenExpired)

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
        }
      }

      s"the agent does not have an arn enrolment" should {
        "redirect to AgentError controller" in {
          setupMockGetSessionDataSuccess()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(InsufficientEnrolments("missing HMRC-AS-AGENT enrolment"))

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show().url)
        }
      }
    }

    else {
      s"the agent does not have a valid delegated MTD enrolment" should {
        "redirect to ClientRelationshipFailureController controller" in {
          setupMockGetSessionDataSuccess()
          setupMockGetClientDetailsSuccess()
          setupMockAgentWithoutMTDEnrolmentForClient()
          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show().url)
        }
      }
    }

    val incomeSourceRequired = mtdUserRole match {
      case MTDSupportingAgent => supportingAgentAccessAllowed
      case _ => true
    }

    if(incomeSourceRequired) {
      s"the $userType is not authenticated and has delegated enrolment but doesn't have income source" should {
        "render the internal error page" in {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockErrorIncomeSource()

          val result = action(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          val errorPage = Jsoup.parse(contentAsString(result))
          errorPage.title shouldEqual "Sorry, there is a problem with the service - GOV.UK"
        }
      }
    }
  }

  def testSupportingAgentDeniedAccess(action: Action[AnyContent])(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    "render the supporting agent unauthorised page" in {
      setupMockSuccess(MTDSupportingAgent)

      val result = action(fakeRequest)

      status(result) shouldBe Status.UNAUTHORIZED
      val unauthorisedPage = Jsoup.parse(contentAsString(result))
      unauthorisedPage.title shouldEqual "You are not authorised to access this page - GOV.UK"
    }
  }
}
