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
import audit.models.IvUpliftRequiredAuditModel
import auth.FrontendAuthorisedFunctions
import auth.authV2.actions._
import auth.authV2.models.{AuthorisedAndEnrolledRequest, AuthorisedUserRequest}
import authV2.AuthActionsTestData._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testNino, testRetrievedUserName, testSaUtr}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, ItmpAddress, ItmpName, LoginTimes, MdtpInformation}
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments, MissingBearerToken, User}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class AuthoriseAndRetrieveAgentForNrsSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(frontendAuthFunctions),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: AuthorisedUserRequest[_] => Assertion
                      ): AuthorisedUserRequest[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: AuthorisedUserRequest[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val authAction = app.injector.instanceOf[AuthoriseAndRetrieveAgentForNrs]

  "refine" should {
    List(Agent).foreach { affinityGroup =>
      "return the expected MtdItUserOptionNino response" when {
        s"the user is an ${affinityGroup.toString} enrolled into HMRC-MTD-IT with the required confidence level" that {
          "has a name, nino and sa enrolment" in {

            when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[NrsAgentAuthRetrievals](
                getAllEnrolmentsAgent(hasNino = true, hasSA = true) ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~
                  Some(affinityGroup) ~ acceptedConfidenceLevel ~ Some("internalId") ~ Some("externalId") ~ Some("AgentCode") ~
                  Some("nino") ~ Some(LocalDate.of(2026, 5, 5)) ~ Some("email") ~
                  AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")) ~
                  Some("groupIdentifier") ~ Some(User) ~ Some(MdtpInformation("deviceId", "sessionId")) ~
                  Some(ItmpName(Some("givenName"), Some("middleName"), Some("familyName"))) ~ Some(LocalDate.of(2026, 5, 5)) ~
                  Some(ItmpAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("line5"), Some("postcode"), Some("countryName"), Some("countryCode"))) ~
                  Some("credentialStrength") ~ LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500)))
              )
            )

            val result = authAction.authorise(false).invokeBlock(
              fakeRequestWithClientDetails,
              defaultAsyncBody{res =>
                res.authUserDetails.affinityGroup shouldBe Some(affinityGroup)
                res.authUserDetails.optNino shouldBe Some(testNino)
                res.authUserDetails.saUtr shouldBe Some(testSaUtr)
                res.authUserDetails.name shouldBe Some(testRetrievedUserName)
                res.authUserDetails.internalId shouldBe Some("internalId")
                res.authUserDetails.externalId shouldBe Some("externalId")
                res.authUserDetails.agentCode shouldBe Some("AgentCode")
                res.authUserDetails.nino shouldBe Some("nino")
                res.authUserDetails.dateOfBirth shouldBe Some(LocalDate.of(2026, 5, 5))
                res.authUserDetails.email shouldBe Some("email")
                res.authUserDetails.agentInformation shouldBe Some(AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")))
                res.authUserDetails.groupIdentifier shouldBe Some("groupIdentifier")
                res.authUserDetails.credentialRole shouldBe Some(User)
                res.authUserDetails.mdtpInformation shouldBe Some(MdtpInformation("deviceId", "sessionId"))
                res.authUserDetails.itmpName shouldBe Some(ItmpName(Some("givenName"), Some("middleName"), Some("familyName")))
                res.authUserDetails.itmpDateOfBirth shouldBe Some(LocalDate.of(2026, 5, 5))
                res.authUserDetails.itmpAddress shouldBe Some(ItmpAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), Some("line5"), Some("postcode"), Some("countryName"), Some("countryCode")))
                res.authUserDetails.credentialStrength shouldBe Some("credentialStrength")
                res.authUserDetails.loginTimes shouldBe Some(LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500))))
              }
            )

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

          "also has no optional data" in {

            when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[NrsAgentAuthRetrievals](
                getAllEnrolmentsAgent(false, false) ~ None ~ Some(testCredentials) ~ Some(affinityGroup) ~ acceptedConfidenceLevel ~
                  None ~ None ~ None ~ None ~ None ~ None ~ AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")) ~
                  None ~ None ~ None ~ None ~ None ~ None ~ None ~ LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500)))
              )
            )

            val result = authAction.authorise(false).invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody{res =>
                res.authUserDetails.affinityGroup shouldBe Some(affinityGroup)
                res.authUserDetails.optNino shouldBe None
                res.authUserDetails.saUtr shouldBe None
                res.authUserDetails.name shouldBe None
                res.authUserDetails.internalId shouldBe None
                res.authUserDetails.externalId shouldBe None
                res.authUserDetails.agentCode shouldBe None
                res.authUserDetails.nino shouldBe None
                res.authUserDetails.dateOfBirth shouldBe None
                res.authUserDetails.email shouldBe None
                res.authUserDetails.agentInformation shouldBe Some(AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")))
                res.authUserDetails.groupIdentifier shouldBe None
                res.authUserDetails.credentialRole shouldBe None
                res.authUserDetails.mdtpInformation shouldBe None
                res.authUserDetails.itmpName shouldBe None
                res.authUserDetails.itmpDateOfBirth shouldBe None
                res.authUserDetails.itmpAddress shouldBe None
                res.authUserDetails.credentialStrength shouldBe None
                res.authUserDetails.loginTimes shouldBe Some(LoginTimes(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(500))))
              })

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "redirect to Agent Error page" when {
          s"the user is an ${affinityGroup.toString} that is not enrolled as an agent" in {

            when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.failed[NrsAgentAuthRetrievals](InsufficientEnrolments())
            )

            val result = authAction.authorise(false).invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/agent-error")
          }
        }
      }
    }

    "redirect to Home page" when {
      "the user is not an Agent" in {
        when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.successful[NrsAgentAuthRetrievals](
            getAllEnrolmentsIndividual(true, true) ~ Some(testRetrievedUserName) ~ Some(testCredentials) ~ Some(Individual) ~ notAcceptedConfidenceLevel ~
              None ~ None ~ None ~ None ~ None ~ None ~ AgentInformation(Some("agentId"), Some("agentCode"), Some("agentName")) ~ None ~ None ~ None ~ None ~ None ~ None ~ None ~
              LoginTimes(Instant.now(), Some(Instant.now()))
          )
        )

        val result = authAction.authorise(false).invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
      }
    }

    "redirect to Session timed out page" when {
      s"the user is has an expired bearer token" in {

        when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[NrsAgentAuthRetrievals](BearerTokenExpired())
        )

        val result = authAction.authorise(false).invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/session-timeout")
      }
    }

    "redirect to Signin" when {
      s"the user is not signed in" in {

        when(mockAuthConnector.authorise[NrsAgentAuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[NrsAgentAuthRetrievals](MissingBearerToken())
        )

        val result = authAction.authorise(false).invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }
}
