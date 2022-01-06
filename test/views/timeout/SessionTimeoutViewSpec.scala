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

package views.timeout

import testConstants.MessagesLookUp.{Timeout => timeoutMessages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.timeout.Timeout

class SessionTimeoutViewSpec extends TestSupport {

  lazy val mockAppConfig = app.injector.instanceOf[FrontendAppConfig]
  lazy val timeoutView = app.injector.instanceOf[Timeout]

  lazy val page = timeoutView()(FakeRequest(),implicitly)
  lazy val document = Jsoup.parse(contentAsString(page))

  "The Session timeout view" should {

    s"have the title '${timeoutMessages.title}'" in {
      document.title() shouldBe timeoutMessages.title
    }

    s"have the H1 '${timeoutMessages.heading}'" in {
      document.getElementsByTag("H1").text() shouldBe timeoutMessages.heading
    }

    s"have a paragraph" which {

      "has the text" in {
        document.getElementById("sign-in").text() shouldBe timeoutMessages.signIn
      }

      "has a link to sign-in page" in {
        document.getElementById("sign-in-link").attr("href") shouldBe controllers.routes.SignInController.signIn().url
      }

    }
  }
}
