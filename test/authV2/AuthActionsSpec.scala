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

package authV2

import auth.authV2.AuthExceptions.{MissingAgentReferenceNumber, MissingMtdId}
import auth.authV2.actions._
import auth.authV2.{AgentUser, AuthActions}
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAuthConnector}
import controllers.agent.sessionUtils.SessionKeys
import mocks.auth.MockOldAuthActions
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import services.IncomeSourceDetailsService
import sttp.model.HeaderNames.Location
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}

import java.net.URLEncoder
import scala.concurrent.Future

class AuthActionsSpec extends TestSupport with ScalaFutures with MockOldAuthActions{

  val nino              = "AA111111A"
  val saUtr             = "123456789"
  val mtdId             = "abcde"
  val arn               = "12345"

  val agentEnrolment            = Enrolment("HMRC-AS-AGENT",  Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), "Activated", None)
  val ninoEnrolment             = Enrolment("HMRC-NI",        Seq(EnrolmentIdentifier("NINO", nino)),                "Activated", None)
  val saEnrolment               = Enrolment("IR-SA",          Seq(EnrolmentIdentifier("UTR", saUtr)),                "Activated", None)
  val credentials               = Credentials("foo", "bar")
  val defaultIncomeSourcesData  = IncomeSourceDetailsModel(nino, saUtr, Some("2012"), Nil, Nil)

  def mtdIdAgentPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId).withDelegatedAuthRule("mtd-it-auth")
  def mtdIdIndividualPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId)

  val individualRetrievalData = RetrievalData(
    enrolments = Enrolments(Set(ninoEnrolment, mtdIdIndividualPredicate(mtdId), saEnrolment)),
    name = None,
    credentials = Some(credentials),
    affinityGroup = Some(AffinityGroup.Individual),
    confidenceLevel = ConfidenceLevel.L250
  )

  val agentRetrievalData = RetrievalData(
    enrolments = Enrolments(Set(agentEnrolment, mtdIdAgentPredicate(mtdId), saEnrolment)),
    name = None,
    credentials = Some(credentials),
    affinityGroup = Some(AffinityGroup.Agent),
    confidenceLevel = ConfidenceLevel.L250
  )

  "individualOrAgentWithClient" when {

    "user is an individual" should {

      "execute block when timestamp and auth token, origin, HMRC-MTD-ID enrolment and successful income sources response" in new ResultFixture(
        retrievals = individualRetrievalData,
        request = fakeRequestWithActiveSession.withSession(("origin", "PTA")),
        block = {
          (user: MtdItUser[_]) =>  {
            user.nino shouldBe nino
            user.arn shouldBe None
            user.mtditid shouldBe mtdId
            user.saUtr shouldBe Some(saUtr)
            user.credId shouldBe Some(credentials.providerId)
            Future.successful(Ok("!"))
          }
        },
        isAgent = false
      ) {
        result.header.status shouldBe OK
      }

      "redirect to not enrolled page if no HMRC-MTD-ID in enrolments" in new ResultFixture(
        retrievals = individualRetrievalData.copy(enrolments = Enrolments(Set.empty)),
        request = fakeRequestWithActiveSession.withSession(("origin", "PTA"), (SessionKeys.clientMTDID, mtdId)),
        isAgent = false) {

        val notEnrolledUrl: String = controllers.errors.routes.NotEnrolledController.show.url
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe notEnrolledUrl
      }

      s"show internal error when $INTERNAL_SERVER_ERROR returned from income sources" in new ResultFixture(
        retrievals = individualRetrievalData,
        incomeSources = IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "Internal server error"),
        isAgent = false
      ) {
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      s"show internal error when $NOT_FOUND returned from income sources" in new ResultFixture(
        retrievals = individualRetrievalData,
        incomeSources = IncomeSourceDetailsError(NOT_FOUND, "Record not found"),
        isAgent = false
      ) {
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      s"redirect to /uplift with confidence level < ${appConfig.requiredConfidenceLevel}, " in new ResultFixture(
        retrievals = individualRetrievalData.copy(confidenceLevel = ConfidenceLevel.L50 ),
        isAgent = false) {

        val completionUrl = URLEncoder.encode("http://localhost:9081/report-quarterly/income-and-expenses/view/uplift-success?origin=PTA", "UTF-8")
        val failureUrl = URLEncoder.encode("http://localhost:9081/report-quarterly/income-and-expenses/view/cannot-view-page", "UTF-8")
        val upliftUrl = s"http://localhost:9948/iv-stub/uplift?origin=ITVC&confidenceLevel=${appConfig.requiredConfidenceLevel}&completionURL=$completionUrl&failureURL=$failureUrl"

        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe upliftUrl
      }

      s"redirect to /cannot-access-service when required enrolments not found" in new AuthThrowsExceptionFixture(
        retrievals = individualRetrievalData,
        authorisationException = new InsufficientEnrolments("Enrolment not found"),
        isAgent = false
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/cannot-access-service"
      }

      s"redirect to /session-timeout when BearerToken has expired" in new AuthThrowsExceptionFixture(
        retrievals = individualRetrievalData,
        authorisationException = new BearerTokenExpired("Bearer token expired"),
        isAgent = false
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/session-timeout"
      }

      "redirect to /session-timeout with a timestamp and no auth token" in new ResultFixture(
        retrievals = individualRetrievalData,
        request = fakeRequestWithTimeoutSession.withSession(("origin", "PTA")),
        isAgent = false
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location).contains("/report-quarterly/income-and-expenses/view/session-timeout") shouldBe true
      }

      s"redirect to /sign-in when any other error is returned" in new AuthThrowsExceptionFixture(
        retrievals = individualRetrievalData,
        authorisationException = new UnsupportedAffinityGroup("Affinity group not supported"),
        isAgent = false
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/sign-in"
      }
    }

    "user is an agent" should {

      val sessionData = Map(
        (SessionKeys.confirmedClient, "true"),
        (SessionKeys.clientMTDID, mtdId),
        (SessionKeys.clientFirstName, "Brian"),
        (SessionKeys.clientLastName, "Brianson"),
        (SessionKeys.clientUTR, saUtr),
        ("origin", "PTA")
      )

      val validAgentRequest = fakeRequestWithActiveSession.withSession({sessionData.toList}:_*)

      val timeoutAgentRequest = fakeRequestWithTimeoutSession.withSession({sessionData.toList}:_*)

      val agentRequestMissingClientMtdId = fakeRequestWithActiveSession
        .withSession({sessionData.filterNot(_._1 == SessionKeys.clientMTDID).toList}:_*)

      val agentRequestMissingConfirmedClient = fakeRequestWithActiveSession
        .withSession({sessionData.filterNot(_._1 == SessionKeys.confirmedClient).toList}: _*)

      "execute block when has timestamp and auth token, origin, agent enrolment, client data and successful income sources response" in new ResultFixture(
        retrievals = agentRetrievalData,
        request = validAgentRequest,
        block = {
          (user: MtdItUser[_]) => {
            user.nino shouldBe nino
            user.arn shouldBe Some(arn)
            user.mtditid shouldBe mtdId
            user.saUtr shouldBe Some(saUtr)
            Future.successful(Ok("!"))
        }},
        isAgent = true
      ) {
        result.header.status shouldBe OK
      }

      s"show internal error when $INTERNAL_SERVER_ERROR returned from income sources" in new ResultFixture(
        retrievals = agentRetrievalData,
        incomeSources = IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "Internal server error"),
        request = validAgentRequest,
        isAgent = true
      ) {
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      s"show internal error when $NOT_FOUND returned from income sources" in new ResultFixture(
        retrievals = agentRetrievalData,
        incomeSources = IncomeSourceDetailsError(NOT_FOUND, "Record not found"),
        request = validAgentRequest,
        isAgent = true
      ) {
        result.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      "throw exception when agent reference number is missing" in new ExceptionFixture(
        retrievals = agentRetrievalData.copy(enrolments = Enrolments(Set.empty)),
        request = validAgentRequest,
        expectedError = new MissingAgentReferenceNumber,
        isAgent = true
      ) {  }

      "redirect to /agents/client-utr when confirmed client is missing" in new ResultFixture(
        retrievals = agentRetrievalData,
        request = agentRequestMissingConfirmedClient,
        isAgent = true,
        sessionDataFailure = true
        ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }

      "redirect to /agents/client-utr when client id is missing from session" in new ResultFixture(
        retrievals = agentRetrievalData,
        request = agentRequestMissingClientMtdId,
        isAgent = true,
        sessionDataFailure = true) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }

      s"redirect to /cannot-access-service when required enrolments not found" in new AuthThrowsExceptionFixture(
        retrievals = agentRetrievalData,
        request = validAgentRequest,
        authorisationException = new InsufficientEnrolments("Enrolment not found"),
        isAgent = true
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/cannot-access-service"
      }

      "redirect to /session-timeout with a timestamp and no auth token" in new ResultFixture(
        retrievals = agentRetrievalData,
        request = timeoutAgentRequest,
        isAgent = true
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location).contains("/report-quarterly/income-and-expenses/view/session-timeout") shouldBe true
      }

      s"redirect to /session-timeout when BearerToken has expired" in new AuthThrowsExceptionFixture(
        retrievals = agentRetrievalData,
        request = validAgentRequest,
        authorisationException = new BearerTokenExpired("Bearer token expired"),
        isAgent = true
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/session-timeout"
      }

      s"redirect to /sign-in when any other error is returned" in new AuthThrowsExceptionFixture(
        retrievals = agentRetrievalData,
        request = validAgentRequest,
        authorisationException = new UnsupportedAffinityGroup("Affinity group not supported"),
        isAgent = true
      ) {
        result.header.status shouldBe SEE_OTHER
        result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/sign-in"
      }
    }
  }

  "isAgent" should {

    "execute block when timestamp and auth token, origin, HMRC-MTD-ID enrolment and successful income sources response" in new AgentResultFixture(
      retrievals = agentRetrievalData,
      request = fakeRequestWithActiveSession,
      block = {
        (user: AgentUser[_]) =>  {
          user.enrolments shouldBe Enrolments(Set(agentEnrolment, mtdIdAgentPredicate(mtdId), saEnrolment))
          user.affinityGroup shouldBe Some(AffinityGroup.Agent)
          user.confidenceLevel shouldBe ConfidenceLevel.L250
          user.credId shouldBe Some(credentials.providerId)
          Future.successful(Ok("!"))
        }
      }
    ) {
      result.header.status shouldBe OK
    }

    s"redirect to /session-timeout when BearerToken has expired" in new AgentAuthThrowsExceptionFixture(
      retrievals = agentRetrievalData,
      authorisationException = BearerTokenExpired("Bearer token expired")
    ) {
      result.header.status shouldBe SEE_OTHER
      result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/session-timeout"
    }

    "redirect to /session-timeout with a timestamp and no auth token" in new AgentResultFixture(
      retrievals = agentRetrievalData,
      request = fakeRequestWithTimeoutSession.withSession()
    ) {
      result.header.status shouldBe SEE_OTHER
      result.header.headers(Location).contains("/report-quarterly/income-and-expenses/view/session-timeout") shouldBe true
    }

    s"redirect to /agents/agent-error when required enrolments not found" in new AgentAuthThrowsExceptionFixture(
      retrievals = agentRetrievalData,
      authorisationException = InsufficientEnrolments("Enrolment not found")
    ) {
      result.header.status shouldBe SEE_OTHER
      result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/agents/agent-error"
    }

    s"redirect to /sign-in when any other error is returned" in new AgentAuthThrowsExceptionFixture(
      retrievals = agentRetrievalData,
      authorisationException = MissingBearerToken("Bearer token not found")
    ){
      result.header.status shouldBe SEE_OTHER
      result.header.headers(Location) shouldBe "/report-quarterly/income-and-expenses/view/sign-in"
    }

  }

  "isAgentWithConfrimedClient" should {

  }

  lazy val mockAuthConnector = mock[FrontendAuthConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockIncomeSourceDetailsService)
  }

  override def fakeApplication(): Application = {

    val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService),
//        api.inject.bind[AuthorisedFunctions].toInstance(frontendAuthFunctions),
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(frontendAuthFunctions),
//        api.inject.bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup]  ~ ConfidenceLevel

  private type AuthAgentRetrievals =
    Enrolments ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel
//
//  private type AuthAgentWithClientRetrievals =
//    Enrolments ~ Option[Credentials] ~ Option[AffinityGroup] ~ Option[Name]

  case class RetrievalData(enrolments: Enrolments,
                           name: Option[Name],
                           credentials: Option[Credentials],
                           affinityGroup: Option[AffinityGroup],
                           confidenceLevel: ConfidenceLevel)

  val mockRetrieveClientData: RetrieveClientData = new RetrieveClientData(
    mockSessionDataService,
    errorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
    appConfig = appConfig,
    clientDetailsService = mockClientDetailsService
  )

  val authActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutAction],
    mockAuthActions.authoriseAndRetrieve,
    app.injector.instanceOf[AuthoriseAndRetrieveIndividual],
    app.injector.instanceOf[AuthoriseAndRetrieveAgent],
    app.injector.instanceOf[AuthoriseAndRetrieveMtdAgent],
    mockAuthActions.agentHasClientDetails,
    app.injector.instanceOf[AgentHasConfirmedClientAction],
    app.injector.instanceOf[AgentIsPrimaryAction],
    mockAuthActions.asMtdUser,
    app.injector.instanceOf[NavBarRetrievalAction],
    app.injector.instanceOf[IncomeSourceRetrievalAction],
    mockRetrieveClientData,
    app.injector.instanceOf[FeatureSwitchRetrievalAction]
  )

  abstract class Fixture(retrievals: RetrievalData,
                         request: FakeRequest[AnyContent] = FakeRequest(),
                         incomeSources: IncomeSourceDetailsResponse = defaultIncomeSourcesData,
                         block: MtdItUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!")),
                         isAgent: Boolean,
                         sessionDataFailure: Boolean) {
    val testConstant = if (isAgent) BaseTestConstants.testAgentAuthRetrievalSuccess else BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse()
    setupMockAuthRetrievalSuccess(testConstant)

    if (sessionDataFailure) setupMockGetSessionDataNotFound() else setupMockGetSessionDataSuccess()

    when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
      .thenReturn(Future.successful(incomeSources))

    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
      Future.successful[AuthRetrievals](
        retrievals.enrolments ~ retrievals.name ~ retrievals.credentials ~ retrievals.affinityGroup ~ retrievals.confidenceLevel
      )
    )
  }



  class AuthThrowsExceptionFixture(retrievals: RetrievalData,
                      request: FakeRequest[AnyContent] = FakeRequest(),
                      incomeSources: IncomeSourceDetailsResponse = defaultIncomeSourcesData,
                      block: MtdItUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!")),
                      authorisationException: AuthorisationException,
                                   isAgent: Boolean,
                                   sessionDataFailure: Boolean = false)
    extends Fixture(retrievals, request, incomeSources, block, isAgent, sessionDataFailure) {

    when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(Future.failed( authorisationException ))

    val result = authActions.asIndividualOrAgent(false).async(block)(request).futureValue
  }

  class ResultFixture(retrievals: RetrievalData,
                      request: FakeRequest[AnyContent] = FakeRequest(),
                      incomeSources: IncomeSourceDetailsResponse = defaultIncomeSourcesData,
                      block: MtdItUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!")),
                      isAgent: Boolean,
                      sessionDataFailure: Boolean = false)
    extends Fixture(retrievals, request, incomeSources, block, isAgent, sessionDataFailure) {

    val result = authActions.asIndividualOrAgent(isAgent).async(block)(request).futureValue
  }

  class ExceptionFixture(retrievals: RetrievalData,
                         request: FakeRequest[AnyContent] = FakeRequest(),
                         incomeSources: IncomeSourceDetailsResponse = defaultIncomeSourcesData,
                         block: MtdItUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!")),
                         expectedError: Throwable,
                         isAgent: Boolean,
                         sessionDataFailure: Boolean = false)
    extends Fixture(retrievals, request, incomeSources, block, isAgent, sessionDataFailure) {

    val failedException: TestFailedException = intercept[TestFailedException] {
      authActions.asIndividualOrAgent(isAgent).async(block)(request).futureValue
    }

    failedException.getCause.getClass shouldBe expectedError.getClass
  }



  class AgentAuthThrowsExceptionFixture(retrievals: RetrievalData,
                                        request: FakeRequest[AnyContent] = FakeRequest(),
                                        block: AgentUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!")),
                                        authorisationException: AuthorisationException) {

    when(mockAuthConnector.authorise[AuthAgentRetrievals](any(), any())(any(), any())).thenReturn(Future.failed( authorisationException ))

    val result = authActions.asAgent().async(block)(request).futureValue
  }

  class AgentResultFixture(retrievals: RetrievalData,
                           request: FakeRequest[AnyContent] = FakeRequest(),
                           block: AgentUser[_] => Future[Result] = (_) => Future.successful(Ok("OK!"))) {

    when(mockAuthConnector.authorise[AuthAgentRetrievals](any(), any())(any(), any())).thenReturn(
      Future.successful[AuthAgentRetrievals](
        retrievals.enrolments ~ retrievals.credentials ~ retrievals.affinityGroup ~ retrievals.confidenceLevel
      )
    )

    val result = authActions.asAgent().async(block)(request).futureValue
  }

}
