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

import auth.MtdItUser
import auth.authV2.AuthActionsTestData._
import auth.authV2.actions._
import config.ItvcErrorHandler
import controllers.bta.BtaNavBarController
import org.scalatest.Assertion
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import views.html.navBar.PtaPartial

import scala.concurrent.Future

class NavBarRetrievalActionSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[BtaNavBarController].toInstance(mockBtaNavBarController),
        api.inject.bind[PtaPartial].toInstance(mockPtaPartial),
        api.inject.bind[ItvcErrorHandler].toInstance(mockItvcErrorHandler)
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUser[_] => Assertion
                      ): MtdItUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUser[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = fakeApplication().injector.instanceOf[NavBarRetrievalAction]

  "refine" when {
    "the user is an Agent" should {
      "make no changes and return the request" in {
        val mtdIdUserRequest = getMtdItUser(Agent)
        val result = action.invokeBlock(
          mtdIdUserRequest,
          defaultAsyncBody(_ shouldBe mtdIdUserRequest))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    List(Individual, Organisation).foreach{affinityGroup =>
      s"the navigation bar is disabled and the user is an ${affinityGroup.toString}" should {
        "make no changes and return the request" in {
          val mtdIdUserRequest = getMtdItUser(affinityGroup)
          val result = action.invokeBlock(
            mtdIdUserRequest,
            defaultAsyncBody(_ shouldBe mtdIdUserRequest))

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }
      }
    }
  }
}
