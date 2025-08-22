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
import auth.authV2.AuthExceptions.NoAssignment
import auth.authV2.actions._
import auth.authV2.models.AuthorisedAndEnrolledRequest
import authV2.AuthActionsTestData._
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import testConstants.BaseTestConstants.testMtditid
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InsufficientEnrolments, MissingBearerToken}

import scala.concurrent.Future

class AuthoriseAndRetrieveMtdAgentSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(frontendAuthFunctions),
        api.inject.bind[AuditingService].toInstance(mockAuditingService)
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: AuthorisedAndEnrolledRequest[_] => Assertion
                      ): AuthorisedAndEnrolledRequest[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: AuthorisedAndEnrolledRequest[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  def redirectAsync(location: String): AuthorisedAndEnrolledRequest[_] => Future[Result] = (_) => Future.successful(Results.SeeOther(location))

  lazy val authAction = app.injector.instanceOf[AuthoriseAndRetrieveMtdAgent]

  "refine" should {
    "return the expected MtdItUserOptionNino response" when {
      s"the user is an Agent with a primary delegated enrolment (HMRC-MTD-IT)" in {
        when(mockAuthConnector.authorise(ArgumentMatchers.eq(primaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.successful(EmptyRetrieval)
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          defaultAsyncBody { res =>
            res.mtditId shouldBe testMtditid
            res.mtdUserRole shouldBe MTDPrimaryAgent
            res.clientDetails.get shouldBe defaultAuthorisedWithClientDetailsRequest.clientDetails
            res.authUserDetails shouldBe defaultAuthorisedWithClientDetailsRequest.authUserDetails
          }
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      s"the user is an Agent with a secondary delegated enrolment (HMRC-MTD-IT-SUPP)" in {
        when(mockAuthConnector.authorise(ArgumentMatchers.eq(primaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(InsufficientEnrolments("enrolment missing"))
        )

        when(mockAuthConnector.authorise(ArgumentMatchers.eq(secondaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.successful(EmptyRetrieval)
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          defaultAsyncBody { res =>
            res.mtditId shouldBe testMtditid
            res.mtdUserRole shouldBe MTDSupportingAgent
            res.clientDetails.get shouldBe defaultAuthorisedWithClientDetailsRequest.clientDetails
            res.authUserDetails shouldBe defaultAuthorisedWithClientDetailsRequest.authUserDetails
          }
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    "redirect to the ClientRelationshipFailureController" when {

      "the user is an Agent, but has no delegated enrolments" in {
        when(mockAuthConnector.authorise(ArgumentMatchers.eq(primaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(InsufficientEnrolments("enrolment missing"))
        )

        when(mockAuthConnector.authorise(ArgumentMatchers.eq(secondaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(InsufficientEnrolments("enrolment missing"))
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/not-authorised-to-view-client")
      }
    }

    "redirect to NoAssignmentController" when {
      "the user is Agent, but Agent is not in an access group associated with the Client" in {
        when(mockAuthConnector.authorise(ArgumentMatchers.eq(primaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(NoAssignment())
        )
        when(mockAuthConnector.authorise(ArgumentMatchers.eq(secondaryAgentPredicate()), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(NoAssignment())
        )

        val result = authAction.invokeBlock(defaultAuthorisedWithClientDetailsRequest, defaultAsync)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/no-assignment")
      }
    }


    "redirect to Session timed out page" when {
      s"there is an expired bearer token" in {
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(BearerTokenExpired())
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          redirectAsync("/report-quarterly/income-and-expenses/view/session-timeout")
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/session-timeout")
      }
    }

    "redirect to Signin" when {
      s"there is a missing bearer token" in {
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(MissingBearerToken())
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }

    "render the error page" when {
      s"there is an unexpected error" in {
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(EmptyRetrieval))(any(), any())).thenReturn(
          Future.failed(new Exception("error"))
        )

        val result = authAction.invokeBlock(
          defaultAuthorisedWithClientDetailsRequest,
          defaultAsync
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
