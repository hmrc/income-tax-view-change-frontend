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

package utils

import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.routes
import mocks.services.MockSessionDataService
import mocks.services.config.MockAppConfig
import models.sessionData.SessionCookieData
import models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostSuccess}
import org.mockito.Mockito.reset
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK, defaultAwaitTimeout, redirectLocation, status}
import services.SessionDataService
import testConstants.BaseTestConstants.{testFirstName, testMtditid, testNino, testSaUtr, testSecondName}
import testUtils.TestSupport

import scala.concurrent.Future

class SessionCookieUtilSpec extends TestSupport with MockSessionDataService with MockAppConfig {

  val TestSessionCookieUtil: SessionCookieUtil = new SessionCookieUtil {
    override val sessionDataService: SessionDataService = mockSessionDataService
    override val itvcErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override val appConfig: FrontendAppConfig = mockAppConfig
  }

  val testSessionCookieData: SessionCookieData =
    SessionCookieData(mtditid = testMtditid, nino = testNino, utr = testSaUtr, clientFirstName = Some(testFirstName), clientLastName = Some(testSecondName))

  val testCodeBlock: Seq[(String, String)] => Future[Result] = cookies => {
    Future.successful(Redirect(routes.ConfirmClientUTRController.show).addingToSession(cookies: _*))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupMockIsUserSessionApiEnabled()
  }

  "SessionCookieUtil.handleSessionCookies" should {
    "add session cookies to the database" when {
      "the feature switch is enabled" when {
        "the call to the service is successful" in {
          setupMockPostSessionData(Right(SessionDataPostSuccess(OK)))

          val res = TestSessionCookieUtil.handleSessionCookies(testSessionCookieData)(testCodeBlock)

          status(res) shouldBe SEE_OTHER
          redirectLocation(res) shouldBe Some(routes.ConfirmClientUTRController.show.url)
        }
        "redirect to the internal server error page" when {
          "the call to the service is unsuccessful" in {
            setupMockPostSessionData(Left(SessionDataPostFailure(INTERNAL_SERVER_ERROR, "POST to session data service was unsuccessful TEST")))

            val res = TestSessionCookieUtil.handleSessionCookies(testSessionCookieData)(testCodeBlock)

            status(res) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
    "just invoke the code block" when {
      "the feature switch is disabled" in {
        reset(mockAppConfig)
        setupMockIsUserSessionApiDisabled()

        val res = TestSessionCookieUtil.handleSessionCookies(testSessionCookieData)(testCodeBlock)

        status(res) shouldBe SEE_OTHER
        redirectLocation(res) shouldBe Some(routes.ConfirmClientUTRController.show.url)
      }
    }
  }
}
