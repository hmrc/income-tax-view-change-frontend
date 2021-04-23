/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.predicates

import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.test.Helpers.{redirectLocation, _}
import testUtils.TestSupport

import scala.concurrent.Future


class SessionTimeoutPredicateSpec extends TestSupport {

  def setupResult(): Action[AnyContent] =
    new SessionTimeoutPredicate()(app.injector.instanceOf[MessagesControllerComponents], ec).async {
      implicit request =>
        Future.successful(Ok("success"))
    }

  "The SessionTimeoutPredicate" when {

    "called with an active session" should {

      "return status OK" in {
        def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
      }

    }

    "called with a timed out session" should {

      def result: Future[Result] = setupResult()(fakeRequestWithTimeoutSession)

      "return OK (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the session timeout page" in {
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

  }

}
