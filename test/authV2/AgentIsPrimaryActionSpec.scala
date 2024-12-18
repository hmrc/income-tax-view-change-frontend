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

import auth.MtdItUserOptionNino
import auth.authV2.actions.AgentIsPrimaryAction
import org.scalatest.Assertion
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import authV2.AuthActionsTestData._
import com.vladsch.flexmark.ast.HtmlEntity
import config.AgentItvcErrorHandler
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.mvc.Results.Unauthorized
import play.twirl.api.HtmlFormat

import scala.concurrent.Future

class AgentIsPrimaryActionSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    new GuiceApplicationBuilder()
      .overrides(api.inject.bind[AgentItvcErrorHandler].toInstance(mockAgentErrorHandler))
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUserOptionNino[_] => Assertion
                      ): MtdItUserOptionNino[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUserOptionNino[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = app.injector.instanceOf[AgentIsPrimaryAction]

  "refine" when {
    "Checking if the Agent is primary" should {
      "Return a Primary agent" in {
        val fakeRequest = getMtdItUserOptionNinoForAuthorise(Some(Agent))(fakeRequestWithActiveSession)

        val result = action.invokeBlock(
          fakeRequest,
          defaultAsync
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      "Show the Unauthorised page" when {
        "the user is a secondary agent" in {
          when(mockAgentErrorHandler.supportingAgentUnauthorised()(any()))
            .thenReturn(Unauthorized(HtmlFormat.escape("supporting agent is not authorised")))

          val fakeRequest = getMtdItUserOptionNinoForAuthorise(Some(Agent), isSupportingAgent = true)(fakeRequestWithActiveSession)

          val result = action.invokeBlock(
            fakeRequest,
            defaultAsync
          )

          status(result) shouldBe UNAUTHORIZED
          contentType(result) shouldBe Some(HTML)
          contentAsString(result) should include("supporting agent is not authorised")
        }
      }
    }
  }
}