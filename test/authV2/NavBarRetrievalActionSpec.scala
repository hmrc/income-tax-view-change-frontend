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

import auth.MtdItUser
import auth.authV2.actions._
import config.ItvcErrorHandler
import controllers.bta.BtaNavBarController
import forms.utils.SessionKeys
import models.admin.{FeatureSwitch, NavBarFs}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.InternalServerError
import play.api.mvc.request.RequestTarget
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.{Application, Play}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import views.html.navBar.PtaPartial
import authV2.AuthActionsTestData._

import scala.concurrent.Future

class NavBarRetrievalActionSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(app)
    super.afterEach()
  }

  def config: Map[String, Object] = Map(
    "feature-switches.read-from-mongo" -> "true"
  )

  override lazy val app: Application = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[BtaNavBarController].toInstance(mockBtaNavBarController),
        api.inject.bind[PtaPartial].toInstance(mockPtaPartial),
        api.inject.bind[ItvcErrorHandler].toInstance(mockItvcErrorHandler)
      )
      .configure(config)
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUser[_] => Assertion
                      ): MtdItUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUser[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  val featureSwitchesIncludingNavBarEnabled = List(FeatureSwitch(NavBarFs, true))
  lazy val action = app.injector.instanceOf[NavBarRetrievalAction]

  "refine" when {
    "the user is an Agent" should {
      "make no changes and return the request" in {
        val mtdIdUserRequest = getMtdItUser(Agent)
        val result = action.invokeBlock(
          mtdIdUserRequest,
          defaultAsyncBody(_.btaNavPartial shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    List(Individual, Organisation).foreach{affinityGroup =>
      s"the user is an ${affinityGroup.toString}" should {
        "make no changes and return the request" when {
          "the navigation bar is disabled" in {
            val mtdIdUserRequest = getMtdItUser(affinityGroup)
            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsyncBody(_.btaNavPartial shouldBe None))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "return an request containing a pta partial navBar" when {
          val ptaNavBar = Html("<test>PTA</test>")
          "the request has no origin query parameter and a PTA origin in the request session" in {
            val fakeRequest = fakeRequestWithActiveSession.withSession(
              SessionKeys.origin -> "PTA"
            )
            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)
            when(mockPtaPartial.apply()(any(), any(), any()))
              .thenReturn(ptaNavBar)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsyncBody(_.btaNavPartial shouldBe Some(ptaNavBar)))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "return an request containing a bta partial navBar" when {
          val btaNavBar = Html("<test>BTA</test>")
          "the request has no origin query parameter and a BTA origin in the request session" in {
            val fakeRequest = fakeRequestWithActiveSession.withSession(
              SessionKeys.origin -> "BTA"
            )
            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)
            when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any()))
              .thenReturn(Future.successful(Some(btaNavBar)))

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsyncBody(_.btaNavPartial shouldBe Some(btaNavBar)))

            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
        }

        "redirect to taxAccountRouterUrl" when {
          "the request has no origin in the query parameters or session request" in {

            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequestWithActiveSession)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some("http://localhost:9280/account")
          }
        }

        "replace/add the origin from the query parameters to the cookies then redirect to current page" when {
          "the request has a pta origin in the query parameters and no origin session request" in {
            val fakeRequest = fakeRequestWithActiveSession
              .withTarget(RequestTarget("http://test/testing", "/testing", Map(SessionKeys.origin -> Seq("pta"))))

            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some("/testing")
            session(result).get(SessionKeys.origin) shouldBe Some("PTA")
          }

          "the request has a BTA origin in the query parameters and no origin session request" in {
            val fakeRequest = fakeRequestWithActiveSession
              .withTarget(RequestTarget("http://test/testing", "/testing", Map(SessionKeys.origin -> Seq("bta"))))

            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some("/testing")
            session(result).get(SessionKeys.origin) shouldBe Some("BTA")
          }

          "the request has a pta origin in the query parameters and a BTA origin session request" in {
            val fakeRequest = fakeRequestWithActiveSession
              .withTarget(RequestTarget("http://test/testing", "/testing", Map(SessionKeys.origin -> Seq("pta"))))
              .withSession(
                SessionKeys.origin -> "BTA"
              )

            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some("/testing")
            session(result).get(SessionKeys.origin) shouldBe Some("PTA")
          }

          "the request has a bta origin in the query parameters and a PTA origin session request" in {
            val fakeRequest = fakeRequestWithActiveSession
              .withTarget(RequestTarget("http://test/testing", "/testing", Map(SessionKeys.origin -> Seq("bta"))))
              .withSession(
                SessionKeys.origin -> "PTA"
              )

            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsync)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some("/testing")
            session(result).get(SessionKeys.origin) shouldBe Some("BTA")
          }
        }

        "render the itvc internal error page" when {
          "the navigation bar is enabled but bta controller returns None" in {
            val fakeRequest = fakeRequestWithActiveSession.withSession(
              SessionKeys.origin -> "BTA"
            )
            val mtdIdUserRequest = getMtdItUser(affinityGroup, featureSwitches = featureSwitchesIncludingNavBarEnabled)(fakeRequest)

            when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any()))
              .thenReturn(Future.successful(None))
            when(mockItvcErrorHandler.showInternalServerError()(any()))
              .thenReturn(InternalServerError("ERROR PAGE"))

            val result = action.invokeBlock(
              mtdIdUserRequest,
              defaultAsyncBody(_.btaNavPartial shouldBe None))

            status(result) shouldBe INTERNAL_SERVER_ERROR
            contentAsString(result) shouldBe "ERROR PAGE"
          }
        }
      }
    }
  }
}
