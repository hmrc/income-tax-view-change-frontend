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
import auth.authV2.actions._
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, BearerTokenExpired, InsufficientEnrolments, MissingBearerToken}
import authV2.AuthActionsTestData._

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
        api.inject.bind[AuditingService].toInstance(mockAuditingService)
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

  def redirectAsync(location: String): MtdItUserOptionNino[_] => Future[Result] = (_) => Future.successful(Results.SeeOther(location))

  lazy val authAction = fakeApplication().injector.instanceOf[AuthoriseAndRetrieveMtdAgent]

  //TODO move this to AuthActionsTestData
  lazy val fakeClientDetailsRequest = ClientDataRequest(
    clientMTDID = mtdId,
    clientFirstName = Some("Test"),
    clientLastName = Some("Client"),
    clientNino = nino,
    clientUTR = saUtr,
    isSupportingAgent = false,
    confirmed = true
  )(fakeRequestWithClientDetails)

  "refine" should {
    "return the expected MtdItUserOptionNino response" when {
      s"the user is an Agent enrolled as a HMRC-AS-AGENT with a primary delegated enrolment (HMRC-MTD-IT)" in {
        val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment = true)

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.successful[AuthRetrievals](
            allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
          )
        )

        val result = authAction.invokeBlock(
          fakeClientDetailsRequest,
          defaultAsyncBody { res =>
            res.mtditid shouldBe mtdId
            res.nino shouldBe Some(nino)
            res.userName shouldBe Some(userName)
            res.btaNavPartial shouldBe None
            res.saUtr shouldBe Some(saUtr)
            res.credId shouldBe Some(credentials.providerId)
            res.userType shouldBe Some(AffinityGroup.Agent)
            res.arn shouldBe Some(arn)
            res.optClientName shouldBe Some(clientName)
            res.isSupportingAgent shouldBe false
          }
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      s"the user is an Agent enrolled as a HMRC-AS-AGENT with a secondary delegated enrolment (HMRC-MTD-IT-SUPP)" in {
        val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment = true, mtdIdSupportingAgentPredicate(mtdId))

        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.successful[AuthRetrievals](
            allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
          )
        )

        val result = authAction.invokeBlock(
          fakeClientDetailsRequest,
          defaultAsyncBody { res =>
            res.mtditid shouldBe mtdId
            res.nino shouldBe Some(nino)
            res.userName shouldBe Some(userName)
            res.btaNavPartial shouldBe None
            res.saUtr shouldBe Some(saUtr)
            res.credId shouldBe Some(credentials.providerId)
            res.userType shouldBe Some(AffinityGroup.Agent)
            res.arn shouldBe Some(arn)
            res.optClientName shouldBe Some(clientName)
            res.isSupportingAgent shouldBe false
          }
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    "redirect to Home page" when {
      List(Individual, Organisation).foreach { affinityGroup =>
        s"the user is an ${affinityGroup.toString}" in {
          when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
            Future.successful[AuthRetrievals](
              getAllEnrolmentsAgent(false, false) ~ Some(userName) ~ Some(credentials) ~ Some(affinityGroup) ~ acceptedConfidenceLevel
            )
          )

          val result = authAction.invokeBlock(
            fakeClientDetailsRequest,
            redirectAsync("/report-quarterly/income-and-expenses/view/")
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
        }
      }
    }

    "redirect to the Agent Error page" when {
      "the user is an Agent that is not enrolled into HMRC-AS-AGENT" in {
        when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
          Future.failed[AuthRetrievals](InsufficientEnrolments())
        )

        val result = authAction.invokeBlock(
          fakeClientDetailsRequest,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/agent-error")
      }

      "redirect to the ClientRelationshipFailureController" when {

        "the user is an Agent, but has no delegated enrolments" in {
          when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
            Future.failed[AuthRetrievals](InsufficientEnrolments("HMRC-MTD-IT"))
          )

          val result = authAction.invokeBlock(
            fakeClientDetailsRequest,
            defaultAsync
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/not-authorised-to-view-client")
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
          redirectAsync("/report-quarterly/income-and-expenses/view/session-timeout")
        )

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
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
  }
}
