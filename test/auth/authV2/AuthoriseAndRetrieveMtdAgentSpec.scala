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

package auth.authV2

import audit.AuditingService
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import auth.authV2.AuthActionsTestData._
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
import uk.gov.hmrc.auth.core.{AffinityGroup, BearerTokenExpired, InsufficientEnrolments, MissingBearerToken}

import scala.concurrent.Future

class AuthoriseAndRetrieveMtdAgentSpec extends AuthActionsSpecHelper {

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
                        requestTestCase: MtdItUserOptionNino[_] => Assertion
                      ): MtdItUserOptionNino[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUserOptionNino[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val authAction = fakeApplication().injector.instanceOf[AuthoriseAndRetrieveMtdAgent]

  //TODO move this to AuthActionsTestData
  lazy val fakeClientDetailsRequest = ClientDataRequest(
    clientMTDID = "XAIT00000000015",
    clientFirstName = Some("Test"),
    clientLastName = Some("User"),
    clientNino = "AA111111A",
    clientUTR = "1234567890",
    isSupportingAgent = false,
    confirmed = true
  )(fakeRequestWithClientDetails)

  "refine" should {
      "return the expected MtdItUserOptionNino response" when {
        s"the user is an Agent enrolled as a HMRC-AS-AGENT with a primary delegated enrolment" that {
          "has nino and sa enrolment" in {
            val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment =  true)
            val expectedResponse = getMtdItUserOptionNinoForAuthoriseMtdAgent(
              Some(AffinityGroup.Agent), fakeClientDetailsRequest
            )(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeClientDetailsRequest,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

          "has no additional enrolments" in {
            val allEnrolments = getAllEnrolmentsAgent(false, false)
            val expectedResponse = getMtdItUserOptionNinoForAuthoriseMtdAgent(Some(AffinityGroup.Agent), fakeClientDetailsRequest)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeClientDetailsRequest,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        s"the user is an Agent enrolled as a HMRC-AS-AGENT with a secondary delegated enrolment" that {
          "has nino and sa enrolment" in {
            val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment =  true)
            val expectedResponse = getMtdItUserOptionNinoForAuthorise(Some(AffinityGroup.Agent), hasUserName = true)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeClientDetailsRequest,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

          "has no additional enrolments" in {
            val allEnrolments = getAllEnrolmentsAgent(false, false)
            val expectedResponse = getAgentData(allEnrolments)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeClientDetailsRequest,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "redirect to AgentError page" when {
          s"the user is an Agent that is not enrolled into HMRC-AS-AGENT" in {

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.failed[AuthRetrievals](InsufficientEnrolments())
            )

            val result = authAction.invokeBlock(
              fakeClientDetailsRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/agent-error")
          }
        }
      }

    "redirect to Home page" when {
      List(Individual, Organisation).foreach { affinityGroup =>
        s"the user is an ${affinityGroup.toString}" in {
          when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
            Future.successful[AuthRetrievals](
              getAllEnrolmentsAgent(false, false) ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
            )
          )

          val result = authAction.invokeBlock(
            fakeClientDetailsRequest,
            defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
        }
      }
    }

    "redirect to Session timed out page" when {
      s"the user is has an expired bearer token" in {

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AuthRetrievals](BearerTokenExpired())
        )

        val result = authAction.invokeBlock(
          fakeClientDetailsRequest,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/session-timeout")
      }
    }

    "redirect to Signin" when {
      s"the user is not signed in" in {

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AuthRetrievals](MissingBearerToken())
        )

        val result = authAction.invokeBlock(
          fakeClientDetailsRequest,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }
}
