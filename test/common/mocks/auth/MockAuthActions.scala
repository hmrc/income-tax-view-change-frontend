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

package common.mocks.auth

import common.auth.actions.AuthActionsTestData.*
import common.auth.FrontendAuthorisedFunctions
import common.connectors.{ITSAStatusConnector, IncomeSourceConnector}
import common.controllers.agent.routes as agentRoutes
import common.controllers.agent.errors.routes as agentErrorRoutes
import common.controllers.errors.routes as errorRoutes
import common.enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import common.mocks.connectors.{MockIncomeSourceConnector, MockIncomeTaxCalculationConnector}
import common.mocks.services.{MockAuditingService, MockClientDetailsService, MockITSAStatusService, MockSessionDataService}
import common.mocks.services.admin.MockFeatureSwitchService
import common.models.admin.FeatureSwitchName
import common.models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse, TaxYear}
import common.models.itsaStatus.ITSAStatus.*
import common.services.AuditingService
import common.services.admin.FeatureSwitchService
import common.viewUtils.InternalUrlHelper
import common.models.itsaStatus.StatusReason.MtdItsaOptOut
import common.models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import common.models.liabilitycalculation.{Inputs, LiabilityCalculationResponse, LiabilityCalculationResponseModel, Metadata, PersonalInformation}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.stubbing.OngoingStubbing
import play.api
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import org.scalatestplus.mockito.MockitoSugar.mock => sMock
import common.services.{DateServiceInterface, SessionDataService}
import common.services.agent.ClientDetailsService
import scala.concurrent.Future
import common.testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testRetrievedUserName}
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import common.testUtils.TestSupport
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, LoginTimes}
import java.time.Instant

trait MockAuthActions
  extends TestSupport
    with MockAuthServiceSupport
    with MockAgentAuthorisedFunctions
    with MockUserAuthorisedFunctions
    with MockAuditingService
    with MockSessionDataService
    with MockClientDetailsService
    with MockFeatureSwitchService
    with MockITSAStatusService
    with MockIncomeTaxCalculationConnector
    with MockIncomeSourceConnector {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    reset(mockFAF)
  }

  override def afterEach() = {
    super.afterEach()
  }

  lazy val mtdAllRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)
  lazy val mockFAF: FrontendAuthorisedFunctions = mock(classFAF)
  
  lazy val mockItsaStatusConnector = sMock[ITSAStatusConnector]
  lazy val mockDateServiceInterface = sMock[DateServiceInterface]

  lazy val applicationBuilderWithAuthBindings: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(mockFAF),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
        api.inject.bind[IncomeSourceConnector].toInstance(mockIncomeSourceConnector),
        api.inject.bind[SessionDataService].toInstance(mockSessionDataService),
        api.inject.bind[ClientDetailsService].toInstance(mockClientDetailsService),
        api.inject.bind[FeatureSwitchService].toInstance(mockFeatureSwitchService)
      )
      .configure(Map("feature-switches.read-from-mongo" -> true))
  }

  def setupMockSuccess(mtdUserRole: MTDUserRole, withNrs: Boolean = false, enabledFeatures: List[FeatureSwitchName] = List()): Unit = {
    if (withNrs) {
      mtdUserRole match {
        case MTDIndividual => setupMockUserAuthWithNrs
        case MTDPrimaryAgent => setupMockAgentWithClientAuthWithNrs(false)
        case _ => setupMockAgentWithClientAuthWithNrs(true)
      }
    } else {
      mtdUserRole match {
        case MTDIndividual => setupMockUserAuth
        case MTDPrimaryAgent => setupMockAgentWithClientAuth(false)
        case _ => setupMockAgentWithClientAuth(true)
      }
    }
    setupMockFeatureSwitches(enabledFeatures*)
  }

  def mockItsaStatusRetrievalAction(
                                     incomeSourceDetailsModel: IncomeSourceDetailsResponse = singleBusinessIncome,
                                     taxYear: TaxYear = TaxYear(2025, 2026)
                                   ): OngoingStubbing[TaxYear] = {

    val itsaStatusResponses = List(
      ITSAStatusResponseModel(
        taxYear = taxYear.previousYear.shortenTaxYearEnd,
        itsaStatusDetails = Some(List(
          StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
        ))
      ),
      ITSAStatusResponseModel(
        taxYear = taxYear.shortenTaxYearEnd,
        itsaStatusDetails = Some(List(
          StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
        ))
      )
    )

    when(mockIncomeSourceConnector.getIncomeSources()(any(), any()))
      .thenReturn(Future.successful(incomeSourceDetailsModel))

    when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Right(itsaStatusResponses)))

    when(mockDateServiceInterface.getCurrentTaxYear)
      .thenReturn(taxYear)
  }

  def mockTriggeredMigrationRetrievalAction(): OngoingStubbing[Future[LiabilityCalculationResponseModel]] = {
    when(mockITSAStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(List(
        ITSAStatusResponseModel(
          taxYear = "2023-24",
          itsaStatusDetails = Some(List(
            StatusDetail("", Voluntary, MtdItsaOptOut, None)
          ))
        )
      )))

    when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
      .thenReturn(Future(LiabilityCalculationResponse(
        metadata = Metadata(None, "IY"),
        inputs = Inputs(PersonalInformation("")),
        calculation = None,
        messages = None,
        submissionChannel = None
      )))
  }

  def setupMockUserAuth: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(hasNino = true, hasSA = true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel
    setupMockUserAuthSuccess(mockFAF)(retrievalValue)
  }

  def setupMockUserAuthWithNrs: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel ~
      None ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~
      LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500)))
    setupMockUserAuthSuccess(mockFAF)(retrievalValue)
  }

  def setupMockUserAuthNoSAUtr: Unit = {
    val allEnrolments = getAllEnrolmentsIndividual(hasNino = true, hasSA = false)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Individual) ~ acceptedConfidenceLevel
    setupMockUserAuthSuccess(mockFAF)(retrievalValue)
  }

  def setupMockAgentWithClientAuth(isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(mockFAF)(retrievalValue, testMtditid, isSupportingAgent)
  }

  def setupMockAgentWithClientAuthWithNrs(isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel ~
      None ~ None ~ None ~ None ~ None ~ None ~ AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")) ~
      None ~ None ~ None ~ None ~ None ~ None ~ None ~ LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500)))
    setupMockAgentWithClientAuthSuccess(mockFAF)(retrievalValue, testMtditid, isSupportingAgent)
  }

  def setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(mockFAF)(retrievalValue, testMtditid, isSupportingAgent)
    mockSingleBusinessIncomeSource()
  }

  final def setupMockUserAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    setupMockUserAuthException(mockFAF)(exception)
  }

  def setupMockAgentWithoutMTDEnrolmentForClient(): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithMissingDelegatedMTDEnrolment(mockFAF)(retrievalValue, testMtditid)
  }

  def setupMockAgentWithoutMTDEnrolmentForClientWithNrs(): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel ~
      None ~ None ~ None ~ None ~ None ~ None ~ AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")) ~
      None ~ None ~ None ~ None ~ None ~ None ~ None ~ LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500)))
    setupMockAgentWithMissingDelegatedMTDEnrolmentWithNrs(mockFAF)(retrievalValue, testMtditid)
  }

  def setupMockAgentSuccess(): Unit = {
    val allEnrolments = getAllEnrolmentsAgent(true, true)
    val retrievalValue = allEnrolments ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentAuthSuccess(mockFAF)(retrievalValue)
  }

  def setupMockAgentWithClientAuthorisationException(exception: AuthorisationException = new InvalidBearerToken): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    setupMockAgentAuthException(mockFAF)(exception)
  }

  def testMTDAuthFailuresForRole(
                                  action: Action[AnyContent],
                                  userRole: MTDUserRole,
                                  supportingAgentAccessAllowed: Boolean = true,
                                  withNrsRetrievals: Boolean = false
                                )(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {
    userRole match {
      case MTDIndividual =>
        testMTDAuthFailuresForIndividual(action, userRole, withNrsRetrievals)(fakeRequest)
      case _ =>
        testMTDAuthFailuresForAgent(action, userRole, supportingAgentAccessAllowed, withNrsRetrievals)(fakeRequest)
    }
  }

  def testMTDIndividualAuthFailures(action: Action[AnyContent]): Unit = {
    testMTDAuthFailuresForIndividual(action, MTDIndividual)(fakeRequestWithActiveSession)
  }

  def testMTDAuthFailuresForIndividual(action: Action[AnyContent], userRole: MTDUserRole, useNrsRetrievals: Boolean = false)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {

    s"the $userRole is not authenticated" should {

      "redirect to signin" in {

        setupMockUserAuthorisationException()
        mockItsaStatusRetrievalAction()

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(InternalUrlHelper.signinUrl)
      }
    }

    s"the $userRole has a session that has timed out" should {

      "redirect to timeout controller" in {

        setupMockUserAuthorisationException(new BearerTokenExpired)
        mockItsaStatusRetrievalAction()

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(InternalUrlHelper.timeoutUrl)
      }
    }

    s"the $userRole is not enrolled into HMRC-MTD-IT" should {

      "redirect to NotEnrolledController controller" in {

        setupMockUserAuthorisationException(InsufficientEnrolments("missing HMRC-MTD-IT enrolment"))
        mockItsaStatusRetrievalAction()

        val result = action(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(errorRoutes.NotEnrolledController.show().url)
      }
    }

    s"the $userRole is not authenticated and enrolled into HMRC-MTD-IT but doesn't have income source" should {

      "render the internal error page" in {

        if(useNrsRetrievals) {
          setupMockUserAuthWithNrs
        } else {
          setupMockUserAuth
        }
        mockItsaStatusRetrievalAction(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))
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

  def testMTDAuthFailuresForAgent(
                                   action: Action[AnyContent],
                                   mtdUserRole: MTDUserRole,
                                   supportingAgentAccessAllowed: Boolean,
                                   useNrsRetrievals: Boolean = false)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]
                                 ): Unit = {

    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val userType = if (isSupportingAgent) "supporting agent" else "primary agent"
    if (mtdUserRole == MTDPrimaryAgent) {

      s"the agent is not authenticated" should {
        "redirect to signin" in {
          setupMockGetSessionDataSuccess()
          mockItsaStatusRetrievalAction()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(mockFAF)(new InvalidBearerToken)

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(InternalUrlHelper.signinUrl)
        }
      }

      s"the agent has a session that has timed out" should {
        "redirect to timeout controller" in {
          setupMockGetSessionDataSuccess()
          mockItsaStatusRetrievalAction()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(mockFAF)(new BearerTokenExpired)

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(InternalUrlHelper.timeoutUrl)
        }
      }

      s"the agent does not have an arn enrolment" should {
        "redirect to AgentError controller" in {
          setupMockGetSessionDataSuccess()
          mockItsaStatusRetrievalAction()
          setupMockGetClientDetailsSuccess()
          setupMockAgentAuthException(mockFAF)(InsufficientEnrolments("missing HMRC-AS-AGENT enrolment"))

          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(agentErrorRoutes.AgentErrorController.show().url)
        }
      }
    } else {
      s"the agent does not have a valid delegated MTD enrolment" should {
        "redirect to ClientRelationshipFailureController controller" in {
          setupMockGetSessionDataSuccess()
          mockItsaStatusRetrievalAction()
          setupMockGetClientDetailsSuccess()
          if (useNrsRetrievals) {
            setupMockAgentWithoutMTDEnrolmentForClientWithNrs()
          } else {
            setupMockAgentWithoutMTDEnrolmentForClient()
          }
          val result = action(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(agentRoutes.ClientRelationshipFailureController.show().url)
        }
      }
    }

    val incomeSourceRequired = mtdUserRole match {
      case MTDSupportingAgent => supportingAgentAccessAllowed
      case _ => true
    }

    if (incomeSourceRequired) {
      s"the $userType is not authenticated and has delegated enrolment but doesn't have income source" should {
        "render the internal error page" in {

          if (useNrsRetrievals) {
            setupMockAgentWithClientAuthWithNrs(isSupportingAgent)
          } else {
            setupMockAgentWithClientAuth(isSupportingAgent)
          }
          mockItsaStatusRetrievalAction(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))
          mockErrorIncomeSource()

          val result = action(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          val errorPage = Jsoup.parse(contentAsString(result))
          errorPage.title shouldEqual "Sorry, there is a problem with the service - GOV.UK"
        }
      }
    }
  }

  def testSupportingAgentDeniedAccess(action: Action[AnyContent], withNrsRetrievals: Boolean = false)(fakeRequest: FakeRequest[AnyContentAsEmpty.type]): Unit = {

    "render the supporting agent unauthorised page" in {

      setupMockSuccess(MTDSupportingAgent, withNrsRetrievals)
      mockItsaStatusRetrievalAction()
      val result = action(fakeRequest)
      status(result) shouldBe Status.UNAUTHORIZED
      val unauthorisedPage = Jsoup.parse(contentAsString(result))
      unauthorisedPage.title shouldEqual "You are not authorised to access this page - GOV.UK"
    }
  }
}
