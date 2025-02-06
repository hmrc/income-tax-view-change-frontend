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

import auth.authV2.actions._
import org.scalatest.Assertion
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class SessionTimeoutActionSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    new GuiceApplicationBuilder()
      .build()
  }

  def defaultAsyncBody(
      requestTestCase: Request[_] => Assertion
    ): Request[_] => Future[Result] =
    testRequest => {
      requestTestCase(testRequest)
      Future.successful(Results.Ok("Successful"))
    }

  def defaultAsync: Request[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = app.injector.instanceOf[SessionTimeoutAction]

  val fakeRequest = FakeRequest()
    .withHeaders(
      HeaderNames.REFERER -> "/test/url",
      "X-Session-ID"      -> "123456789"
    )
  "refine" should {
    "return the request with additional headers" when {
      "the request is a Gov-test-Scenario" that {
        val fakeGovTestRequest = fakeRequest
          .withSession(
            "Gov-Test-Scenario" -> "testData"
          )
        "contains an auth token and lastRequestTimestamp" in {
          val request = fakeGovTestRequest.withSession(
            SessionKeys.authToken            -> "Bearer Token",
            SessionKeys.lastRequestTimestamp -> "1498236506662"
          )

          val result =
            action.invokeBlock(request, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData")))

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "contains an auth token and no lastRequestTimestamp" in {
          val request = fakeGovTestRequest.withSession(
            SessionKeys.authToken -> "Bearer Token"
          )

          val result =
            action.invokeBlock(request, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData")))

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "does not contain an auth token or lastRequestTimestamp" in {
          val result = action.invokeBlock(
            fakeGovTestRequest,
            defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData"))
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }
      }

      "the request is not a Gov-test-Scenario" that {
        val fakeGovTestRequest = fakeRequest
          .withHeaders("Gov-Test-Scenario" -> "testData")
        "contains an auth token and lastRequestTimestamp" in {
          val request = fakeGovTestRequest.withSession(
            SessionKeys.authToken            -> "Bearer Token",
            SessionKeys.lastRequestTimestamp -> "1498236506662"
          )

          val result =
            action.invokeBlock(request, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData")))

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "contains an auth token and no lastRequestTimestamp" in {
          val request = fakeGovTestRequest.withSession(
            SessionKeys.authToken -> "Bearer Token"
          )

          val result =
            action.invokeBlock(request, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData")))

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "does not contain an auth token or lastRequestTimestamp" in {
          val result = action.invokeBlock(
            fakeGovTestRequest,
            defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("testData"))
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }
      }
    }

    "Redirect to SessionTimeout controller" when {
      "the request is a Gov-test-Scenario" that {
        val fakeGovTestRequest = fakeRequest
          .withSession(
            "Gov-Test-Scenario" -> "testData"
          )
        "has a lastRequestTimestamp but no auth token" in {
          val request = fakeGovTestRequest.withSession(
            SessionKeys.lastRequestTimestamp -> "1498236506662"
          )

          val result = action.invokeBlock(request, defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain("/report-quarterly/income-and-expenses/view/session-timeout")
        }
      }
    }
  }
}
