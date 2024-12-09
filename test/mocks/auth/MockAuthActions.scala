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
import auth.authV2.AuthActions
import authV2.AuthActionsTestData._
import config.featureswitch.FeatureSwitching
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import mocks.MockItvcErrorHandler
import mocks.services.{MockClientDetailsService, MockIncomeSourceDetailsService, MockSessionDataService}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api
import play.api.Play
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.agent.ClientDetailsService
import services.{IncomeSourceDetailsService, SessionDataService}
import testConstants.BaseTestConstants.{agentAuthRetrievalSuccess, testAuthSuccessResponse}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, BearerTokenExpired, InsufficientEnrolments, InvalidBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthActions extends
  TestSupport with
  MockIncomeSourceDetailsService with
  MockAgentAuthorisedFunctions with
  MockUserAuthorisedFunctions with
  MockAuditingService with
  MockItvcErrorHandler with
  MockSessionDataService with
  MockClientDetailsService with
  FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
//    Play.stop(fakeApplication())
    reset(mockAuthService)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Play.stop(fakeApplication())
  }

  override def afterEach() = {
    super.afterEach()
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  lazy val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  lazy val mockAuthService: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])

  def applicationBuilderWithAuthBindings(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(mockAuthService),
        api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
        api.inject.bind[SessionDataService].toInstance(mockSessionDataService),
        api.inject.bind[ClientDetailsService].toInstance(mockClientDetailsService)
      )
  }

  val mockAuthActions: AuthActions = app.injector.instanceOf[AuthActions]

  def setupMockSuccess[X, Y](mtdUserRole: MTDUserRole): Unit = mtdUserRole match {
    case MTDIndividual => setupMockUserAuth
    case MTDPrimaryAgent => setupMockAgentWithClientAuth(false)
    case _ => setupMockAgentWithClientAuth(true)
  }

  def setupMockUserAuth[X, Y]: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(true, true)
    val retrievalValue = allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel
    setupMockUserAuthSuccess(retrievalValue)
  }

  def setupMockAgentWithClientAuth[X, Y](isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment = true)
    val retrievalValue = allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(retrievalValue, mtdId, isSupportingAgent)
  }

  def setupMockAgentWithClientAuthAndIncomeSources[X, Y](isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment = true)
    val retrievalValue = allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(retrievalValue, mtdId, isSupportingAgent)
    mockSingleBusinessIncomeSource()
  }

  def setupMockUserAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    setupMockUserAuthException(exception)
  }

  def setupMockAgentWithClientAuthorisationException(exception: AuthorisationException = new InvalidBearerToken, isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    setupMockAgentWithClientAuthException(exception, mtdId, isSupportingAgent)
  }

  def testMTDAuthFailuresForRole(action: Action[AnyContent], userRole: MTDUserRole)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    userRole match {
      case MTDIndividual => testMTDAuthFailuresForIndividual(action, userRole)(fakeRequest)
      case _ => testMTDAuthFailuresForAgent(action, userRole)(fakeRequest)
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
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }

    s"the $userRole has a session that has timed out" should {
      "redirect to timeout controller" in {
        setupMockUserAuthorisationException(new BearerTokenExpired)

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    s"the $userRole is not enrolled into HMRC-MTD-IT" should {
      "redirect to NotEnrolledController controller" in {
        setupMockUserAuthorisationException(InsufficientEnrolments("missing HMRC-MTD-IT enrolment"))

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.errors.routes.NotEnrolledController.show.url)
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
    testMTDAuthFailuresForAgent(action, mdtUserRole)(fakeRequest)
  }

  def testMTDAuthFailuresForAgent(action: Action[AnyContent], mtdUserRole: MTDUserRole)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val userType = if(isSupportingAgent) "supporting agent" else "primary agent"
    s"the $userType is not authenticated" should {
      "redirect to signin" in {
        setupMockGetSessionDataSuccess()
        setupMockGetClientDetailsSuccess()
        setupMockAgentWithClientAuthException( new InvalidBearerToken, mtdId, isSupportingAgent)

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }

    s"the $userType has a session that has timed out" should {
      "redirect to timeout controller" in {
        setupMockGetSessionDataSuccess()
        setupMockGetClientDetailsSuccess()
        setupMockAgentWithClientAuthException( new BearerTokenExpired, mtdId, isSupportingAgent)

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    s"the $userType does not have an arn enrolment" should {
      "redirect to AgentError controller" in {
        setupMockGetSessionDataSuccess()
        setupMockGetClientDetailsSuccess()
        setupMockAgentWithClientAuthException(InsufficientEnrolments("missing HMRC-AS-AGENT enrolment"), mtdId, isSupportingAgent)

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.errors.routes.AgentErrorController.show.url)
      }
    }

    s"the $userType does not have a valid delegated MTD enrolment" should {
      "redirect to ClientRelationshipFailureController controller" in {
        setupMockGetSessionDataSuccess()
        setupMockGetClientDetailsSuccess()
        setupMockAgentWithClientAuthorisationException(exception = InsufficientEnrolments("HMRC-MTD-IT is missing"), isSupportingAgent)
        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.agent.routes.ClientRelationshipFailureController.show.url)
      }
    }

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

  def testSupportingAgentDeniedAccess(action: Action[AnyContent])(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    "render the supporting agent unauthorised page" in {
      setupMockSuccess(MTDSupportingAgent)

      val result = action(fakeRequest)

      status(result) shouldBe Status.UNAUTHORIZED
      val unauthorisedPage = Jsoup.parse(contentAsString(result))
      unauthorisedPage.title shouldEqual "You are not authorised to access this page - Manage your client’s Income Tax updates - GOV.UK"
    }
  }
}
