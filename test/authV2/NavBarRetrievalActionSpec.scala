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

package authV2

import auth.MtdItUser
import auth.authV2.actions.*
import authV2.AuthActionsTestData.*
import config.ItvcErrorHandler
import controllers.bta.BtaNavBarController
import forms.utils.SessionKeys
import models.admin.{FeatureSwitch, NavBarFs}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.request.RequestTarget
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.*
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import views.html.navBar.PtaPartial

import scala.concurrent.Future

class NavBarRetrievalActionSpec extends AuthActionsSpecHelper {

  private val featureSwitchNavBarEnabled = List(FeatureSwitch(NavBarFs, true))

  def buildApp(useRebrand: Boolean): Application =
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[BtaNavBarController].toInstance(mockBtaNavBarController),
        api.inject.bind[PtaPartial].toInstance(mockPtaPartial),
        api.inject.bind[ItvcErrorHandler].toInstance(mockItvcErrorHandler)
      )
      .configure(
        "feature-switches.read-from-mongo" -> "true",
        "itvc.useRebrand" -> useRebrand.toString
      )
      .build()

  def defaultAsyncBody(assertion: MtdItUser[_] => Assertion): MtdItUser[_] => Future[Result] =
    request => {
      assertion(request)
      Future.successful(Results.Ok("Successful"))
    }

  def defaultAsync: MtdItUser[_] => Future[Result] = _ => Future.successful(Results.Ok("Successful"))

  private def sharedTests(app: Application, rebrand: Boolean): Unit = {

    val action = app.injector.instanceOf[NavBarRetrievalAction]

    "the user is an Agent" should {
      "return the request unchanged" in {
        val mtdReq = getMtdItUser(Agent)
        val result = action.invokeBlock(mtdReq, defaultAsyncBody(_.btaNavPartial shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    List(Individual, Organisation).foreach { affinityGroup =>
      s"the user is a ${affinityGroup.toString}" should {

        "return request unchanged when navbar FS is disabled" in {
          val mtdReq = getMtdItUser(affinityGroup)
          val result = action.invokeBlock(
            mtdReq,
            defaultAsyncBody(_.btaNavPartial shouldBe None)
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Successful"
        }

        if (!rebrand) {

          "return PTA partial when origin in session = PTA" in {
            val ptaHtml = Html("<test>PTA</test>")
            val req = fakeRequestWithActiveSession.withSession(SessionKeys.origin -> "PTA")
            val mtdReq =
              getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            when(mockPtaPartial.apply()(any(), any(), any()))
              .thenReturn(ptaHtml)

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.btaNavPartial shouldBe Some(ptaHtml))
            )

            status(result) shouldBe OK
          }

          "return BTA partial when origin in session = BTA" in {
            val btaHtml = Html("<test>BTA</test>")
            val req = fakeRequestWithActiveSession.withSession(SessionKeys.origin -> "BTA")
            val mtdReq =
              getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any()))
              .thenReturn(Future.successful(btaHtml))

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.btaNavPartial shouldBe Some(btaHtml))
            )

            status(result) shouldBe OK
          }

        }

        if (rebrand) {
          "return PTA ServiceNavigation when origin = PTA" in {
            val req = fakeRequestWithActiveSession.withSession(SessionKeys.origin -> "PTA")
            val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.serviceNavigationPartial shouldBe defined)
            )

            status(result) shouldBe OK
          }

          "return BTA ServiceNavigation when origin = BTA" in {
            val req = fakeRequestWithActiveSession.withSession(SessionKeys.origin -> "BTA")
            val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(req)

            val result = action.invokeBlock(
              mtdReq,
              defaultAsyncBody(_.serviceNavigationPartial shouldBe defined)
            )

            status(result) shouldBe OK
          }
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
              RequestTarget("http://test/testing", "/testing", Map(SessionKeys.origin -> Seq("pta")))
            )

          val mtdReq = getMtdItUser(affinityGroup, featureSwitchNavBarEnabled)(requestWithQuery)

          val result = action.invokeBlock(mtdReq, defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/testing")
          session(result).get(SessionKeys.origin) shouldBe Some("PTA")
        }
      }
    }
  }

  "NavBarRetrievalAction when rebrand = true" when {
    val app = buildApp(useRebrand = true)
    sharedTests(app, rebrand = true)
  }

  "NavBarRetrievalAction when rebrand = false" when {
    val app = buildApp(useRebrand = false)
    sharedTests(app, rebrand = false)
  }
}
