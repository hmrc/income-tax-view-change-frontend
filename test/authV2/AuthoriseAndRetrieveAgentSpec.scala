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

import audit.AuditingService
import auth.FrontendAuthorisedFunctions
import auth.authV2.AgentUser
import auth.authV2.actions._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments, MissingBearerToken}
import authV2.AuthActionsTestData._

import scala.concurrent.Future

class AuthoriseAndRetrieveAgentSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(frontendAuthFunctions),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: AgentUser[_] => Assertion
                      ): AgentUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: AgentUser[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val authAction = fakeApplication().injector.instanceOf[AuthoriseAndRetrieveAgent]

  "refine" should {
      "return the expected MtdItUserOptionNino response" when {
        s"the user is an Agent enrolled as a HMRC-AS-AGENT" that {
          "has nino and sa enrolment" in {
            val allEnrolments = getAllEnrolmentsAgent(true, true)
            val expectedResponse = getAgentData(allEnrolments)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AgentAuthRetrievals](
                allEnrolments ~ Some(credentials) ~ Some(Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

          "has no additional enrolments" in {
            val allEnrolments = getAllEnrolmentsAgent(false, false)
            val expectedResponse = getAgentData(allEnrolments)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AgentAuthRetrievals](
                allEnrolments ~ Some(credentials) ~ Some(Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "redirect to AgentError page" when {
          s"the user is an Agent that is not enrolled into HMRC-AS-AGENT" in {

            when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.failed[AgentAuthRetrievals](InsufficientEnrolments())
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/agent-error")
          }
        }
      }

    "redirect to Home page" when {
      List(Individual, Organisation).foreach { affinityGroup =>
        s"the user is an ${affinityGroup.toString}" in {
          when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
            Future.successful[AgentAuthRetrievals](
              getAllEnrolmentsAgent(false, false) ~ Some(credentials) ~ Some(affinityGroup) ~ notAcceptedConfidenceLevel
            )
          )

          val result = authAction.invokeBlock(
            fakeRequestWithActiveSession,
            defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
        }
      }
    }

    "redirect to Session timed out page" when {
      s"the user is has an expired bearer token" in {

        when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AgentAuthRetrievals](BearerTokenExpired())
        )

        val result = authAction.invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/session-timeout")
      }
    }

    "redirect to Signin" when {
      s"the user is not signed in" in {

        when(mockAuthConnector.authorise[AgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AgentAuthRetrievals](MissingBearerToken())
        )

        val result = authAction.invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }
}