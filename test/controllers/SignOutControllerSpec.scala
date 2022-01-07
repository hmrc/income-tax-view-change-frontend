/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import auth.FrontendAuthorisedFunctions
import config.{FrontendAppConfig, FrontendAuthConnector}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AffinityGroup, MissingBearerToken}

import scala.concurrent.Future

class SignOutControllerSpec extends TestSupport {

  val mockAuthConnector: FrontendAuthConnector = mock[FrontendAuthConnector]

  def mockAuth(authResult: Future[Option[AffinityGroup]]): Any =
    when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup]]]())(any(), any()))
      .thenReturn(authResult)

  object TestSignOutController extends SignOutController(
    app.injector.instanceOf[FrontendAppConfig],
    new FrontendAuthorisedFunctions(mockAuthConnector),
    app.injector.instanceOf[MessagesControllerComponents]
  )

  "navigating to signout page" when {

    "the user is an agent" should {
      lazy val result: Future[Result] = {
        mockAuth(Future.successful(Some(AffinityGroup.Agent)))
        TestSignOutController.signOut(fakeRequestWithActiveSession)
      }

      "return 303" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the correct survey url" in {
        redirectLocation(result) shouldBe Some(appConfig.ggSignOutUrl("ITVCA"))
      }
    }

    "the user is a principal entity" should {
      lazy val result: Future[Result] = {
        mockAuth(Future.successful(Some(AffinityGroup.Individual)))
        TestSignOutController.signOut(fakeRequestWithActiveSession)
      }

      "return 303" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the correct survey url" in {
        redirectLocation(result) shouldBe Some(appConfig.ggSignOutUrl("ITVC"))
      }
    }

    "there is an authorisation exception" should {
      lazy val result: Future[Result] = {
        mockAuth(Future.failed(MissingBearerToken()))
        TestSignOutController.signOut(fakeRequestWithActiveSession)
      }

      "return 303" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the unauthorised sign out URL" in {
        redirectLocation(result) shouldBe Some(appConfig.signInUrl)
      }
    }
  }
}
