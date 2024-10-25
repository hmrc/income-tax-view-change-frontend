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
import audit.models.IvUpliftRequiredAuditModel
import auth.authV2.AuthActionsTestData._
import auth.authV2.actions._
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.Assertion
import play.api
import play.api.http.HeaderNames
import play.api.{Application, Play}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.{Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisationException, BearerTokenExpired, ConfidenceLevel, InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class AuthoriseAndRetrieveIndividualSpec extends AuthActionsSpecHelper {

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

  lazy val authAction = fakeApplication().injector.instanceOf[AuthoriseAndRetrieveIndividual]

  "refine" should {
    List(Individual, Organisation).foreach { affinityGroup =>
      "return the expected MtdItUserOptionNino response" when {
        s"the user is an ${affinityGroup.toString} enrolled into HMRC-MTD-IT with the required confidence level" that {
          "has a name, nino and sa enrolment" in {
            val expectedResponse = getMtdItUserOptionNinoForAuthorise(
              affinityGroup = Some(affinityGroup),
              hasNino = true,
              hasSA = true,
              hasUserName = true)(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                getAllEnrolmentsIndividual(true, true) ~ Some(userName) ~ Some(credentials) ~ Some(affinityGroup) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

          "also has no name or additional enrolments" in {
            val expectedResponse = getMtdItUserOptionNinoForAuthorise(
              affinityGroup = Some(affinityGroup))(fakeRequestWithActiveSession)

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                getAllEnrolmentsIndividual(false, false) ~ None ~ Some(credentials) ~ Some(affinityGroup) ~ acceptedConfidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody(_ shouldBe expectedResponse))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "redirect to IV" when {
          s"the user is an ${affinityGroup.toString} enrolled into HMRC-MTD-IT without the required confidence level" that {
            "has a name, nino and sa enrolment" in {

              when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
                Future.successful[AuthRetrievals](
                  getAllEnrolmentsIndividual(true, true) ~ Some(userName) ~ Some(credentials) ~ Some(affinityGroup) ~ notAcceptedConfidenceLevel
                )
              )

              val result = authAction.invokeBlock(
                fakeRequestWithActiveSession,
                defaultAsync)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result).get should include("/iv-stub/uplift")
              verify(mockAuditingService, times(1)).audit(any[IvUpliftRequiredAuditModel](), any())(any(), any(), any())
            }

            "also has no name or additional enrolments" in {

              when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
                Future.successful[AuthRetrievals](
                  getAllEnrolmentsIndividual(false, false) ~ None ~ Some(credentials) ~ Some(affinityGroup) ~ notAcceptedConfidenceLevel
                )
              )

              val result = authAction.invokeBlock(
                fakeRequestWithActiveSession,
                defaultAsync)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result).get should include("/iv-stub/uplift")
              verify(mockAuditingService, times(1)).audit(any[IvUpliftRequiredAuditModel](), any())(any(), any(), any())
            }
          }
        }

        "redirect to NotEnrolled page" when {
          s"the user is an ${affinityGroup.toString} that is not enrolled into HMRC-MTD-IT" in {

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.failed[AuthRetrievals](InsufficientEnrolments())
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/cannot-access-service")
          }
        }
      }
    }

    "redirect to EnterClientsUtr page" when {
      "the user is an Agent" in {
        val expectedResponse = getMtdItUserOptionNinoForAuthorise(
          affinityGroup = Some(Agent),
          hasNino = true,
          hasSA = true,
          hasUserName = true)(fakeRequestWithActiveSession)

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.successful[AuthRetrievals](
            getAllEnrolmentsIndividual(true, true) ~ Some(userName) ~ Some(credentials) ~ Some(Agent) ~ notAcceptedConfidenceLevel
          )
        )

        val result = authAction.invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsyncBody(_ shouldBe expectedResponse))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/client-utr")
      }
    }

    "redirect to Session timed out page" when {
      s"the user is has an expired bearer token" in {

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AuthRetrievals](BearerTokenExpired())
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

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AuthRetrievals](MissingBearerToken())
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