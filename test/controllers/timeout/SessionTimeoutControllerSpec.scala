/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.timeout

import assets.Messages.{Timeout => messages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import testUtils.TestSupport

class SessionTimeoutControllerSpec extends TestSupport {

  object TestSessionTimeoutController extends SessionTimeoutController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi]
  )

  "Calling the timeout action of the SessionTimeoutController" should {

    lazy val result = TestSessionTimeoutController.timeout(fakeRequestNoSession)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK (200)" in {
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }
  }
}
