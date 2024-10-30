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

import auth.authV2.AuthActionsTestData._
import auth.authV2.actions._
import config.AgentItvcErrorHandler
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import services.SessionDataService
import testOnly.models.SessionDataGetResponse.{SessionDataNotFound, SessionDataUnexpectedResponse}

import scala.concurrent.Future

class RetrieveClientDataSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[SessionDataService].toInstance(mockSessionDataService),
        api.inject.bind[AgentItvcErrorHandler].toInstance(mockAgentErrorHandler)
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: ClientDataRequest[_] => Assertion
                      ): ClientDataRequest[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: ClientDataRequest[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = fakeApplication().injector.instanceOf[RetrieveClientData]

  "refine" when {
    "the session data service returns client details" should {
      "return the expected ClientDataRequest" when {
        "the user has is supporting agent that has been confirmed" in {
            val fakeRequestWithSession = fakeRequestConfirmedClient(isSupportingAgent = true)
            when(mockSessionDataService.getSessionData(any())(any(), any()))
              .thenReturn(Future.successful(Right(sessionGetSuccessResponse)))

            val result = action.invokeBlock(
              fakeRequestWithSession,
              defaultAsyncBody { res =>
                res.isSupportingAgent shouldBe true
                res.confirmed shouldBe true
              })

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }

        "the user has is primary agent that has not been confirmed" in {
          val fakeRequestWithSession = fakeRequestWithClientDetails
          when(mockSessionDataService.getSessionData(any())(any(), any()))
            .thenReturn(Future.successful(Right(sessionGetSuccessResponse)))

          val result = action.invokeBlock(
            fakeRequestWithSession,
            defaultAsyncBody { res =>
              res.isSupportingAgent shouldBe false
              res.confirmed shouldBe false
            })

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }
      }
    }

    "there is no sessionData returned from session data service" should {
      "redirect to the enter clients utr page" in {

        when(mockSessionDataService.getSessionData(any())(any(), any()))
          .thenReturn(Future.successful(Left(SessionDataNotFound("no data"))))

        val result = action.invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents/client-utr")
      }
    }

    "the session data service returns an unexpected response" should {
      "render the internal error page" in {

        when(mockSessionDataService.getSessionData(any())(any(), any()))
          .thenReturn(Future.successful(Left(SessionDataUnexpectedResponse("error"))))

        when(mockAgentErrorHandler.showInternalServerError()(any()))
          .thenReturn(InternalServerError("ERROR PAGE"))

        val result = action.invokeBlock(
          fakeRequestWithActiveSession,
          defaultAsync)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe "ERROR PAGE"
      }
    }
  }
}
