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

package common.auth.actions

import common.auth.AuthorisedAndEnrolledRequest
import common.auth.actions.AuthActionsTestData.*
import common.enums.MTDSupportingAgent
import org.scalatest.Assertion
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.*

import scala.concurrent.Future

class AgentHasConfirmedClientActionSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    new GuiceApplicationBuilder()
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: AuthorisedAndEnrolledRequest[_] => Assertion
                      ): AuthorisedAndEnrolledRequest[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }


  def defaultAsync: AuthorisedAndEnrolledRequest[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = app.injector.instanceOf[AgentHasConfirmedClientAction]

  "refine" when {
    "Checking if the Agent has been confirmed" should {
      "return a confirmed Agent" in {
        val fakeRequest = defaultAuthorisedAndEnrolledRequest(MTDSupportingAgent, fakeRequestWithActiveSession)

        val result = action.invokeBlock(
          fakeRequest,
          defaultAsync
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }
    "Checking if the Agent has not been confirmed" should {
      "Redirect to Confirm Client UTR page" in {
        val fakeRequest = defaultAuthorisedAndEnrolledRequest(MTDSupportingAgent, fakeRequestWithActiveSession)
          .copy(clientDetails = Some(getAgentClientDetails(false)))

        val result = action.invokeBlock(
          fakeRequest,
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view/agents")

      }
    }
  }

}