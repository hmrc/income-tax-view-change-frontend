/*
 * Copyright 2023 HM Revenue & Customs
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

import common.auth.MtdItUser
import common.auth.actions.AuthActionsTestData.*
import common.models.admin.{FeatureSwitch, NavBarFs}
import common.utils.AuthUtils
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.request.RequestTarget
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}

import scala.concurrent.Future

class NavBarRetrievalActionSpec extends AuthActionsSpecHelper {

  private val featureSwitchNavBarEnabled = List(FeatureSwitch(NavBarFs, true))

  def buildApp(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "feature-switches.read-from-mongo" -> "true"
      )
      .build()

  def defaultAsyncBody(assertion: MtdItUser[_] => Assertion): MtdItUser[_] => Future[Result] =
    request => {
      assertion(request)
      Future.successful(Results.Ok("Successful"))
    }

  def defaultAsync: MtdItUser[_] => Future[Result] = _ => Future.successful(Results.Ok("Successful"))

  private def sharedTests(app: Application): Unit = {

    val action = app.injector.instanceOf[NavBarRetrievalAction]

    "the user is an Agent" should {
      "return the request unchanged" in {
        val mtdReq = getMtdItUser(Agent)
        val result = action.invokeBlock(mtdReq, defaultAsync)

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    List(Individual, Organisation).foreach { affinityGroup =>
      s"the user is a ${affinityGroup.toString}" should {

        "return request unchanged when navbar FS is disabled" in {
          val mtdReq = getMtdItUser(affinityGroup)
          val result = action.invokeBlock(mtdReq, defaultAsync)

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "return PTA ServiceNavigation when origin = PTA" in {
            val req = fakeRequestWithActiveSession.withSession(AuthUtils.ORIGIN -> "PTA")
            val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.serviceNavigationPartial shouldBe defined)
            )

            status(result) shouldBe OK
          }

          "return BTA ServiceNavigation when origin = BTA" in {
            val req = fakeRequestWithActiveSession.withSession(AuthUtils.ORIGIN -> "BTA")
            val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.serviceNavigationPartial shouldBe defined)
            )

            status(result) shouldBe OK
          }

        "return unchanged request when no origin found" in {
          val mtdReq =
            getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(fakeRequestWithActiveSession)

          val result = action.invokeBlock(mtdReq, defaultAsync)

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        "save origin from query params and redirect" in {
          val requestWithQuery =
            fakeRequestWithActiveSession.withTarget(
              RequestTarget("http://test/testing", "/testing", Map(AuthUtils.ORIGIN -> Seq("pta")))
            )

          val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(requestWithQuery)

          val result = action.invokeBlock(mtdReq, defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/testing")
          session(result).get(AuthUtils.ORIGIN) shouldBe Some("PTA")
        }
      }
    }
  }

  "NavBarRetrievalAction" when {
    val app = buildApp()
    sharedTests(app)
  }
}
