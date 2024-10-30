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

import auth.MtdItUserOptionNino
import auth.authV2.AuthActionsTestData.getMtdItUserOptionNinoForAuthorise
import auth.authV2.actions.AgentIsPrimaryAction
import org.scalatest.Assertion
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.{contentAsString, status}
import play.api.{Application, Play}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import play.api.test.Helpers._


import scala.concurrent.Future

class AgentIsPrimaryActionSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUserOptionNino[_] => Assertion
                      ): MtdItUserOptionNino[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUserOptionNino[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = fakeApplication().injector.instanceOf[AgentIsPrimaryAction]

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
      "Redirect to the Unathorised page" in {
        val fakeRequest = getMtdItUserOptionNinoForAuthorise(Some(Agent), isSupportingAgent = true)(fakeRequestWithActiveSession)

        val result = action.invokeBlock(
          fakeRequest,
          defaultAsync
        )

        status(result) shouldBe UNAUTHORIZED
        contentAsString(result) shouldBe "new page to go here"

      }
    }

  }

}