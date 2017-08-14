/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.Messages.{Timeout => messages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport

class SessionTimeoutViewSpec extends TestSupport {

  lazy val mockAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val page = views.html.timeout.timeout()(FakeRequest(), applicationMessages, mockAppConfig)
  lazy val document = Jsoup.parse(contentAsString(page))

  "The Session timeout view" should {

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the H1 '${messages.heading}'" in {
      document.getElementsByTag("H1").text() shouldBe messages.heading
    }

    s"have a paragraph" which {

      "has the text" in {
        document.getElementById("sign-in").text() shouldBe messages.signIn
      }

      "has a link to sign-in page" in {
        document.getElementById("sign-in-link").attr("href") shouldBe controllers.routes.SignInController.signIn().url
      }

    }

  }

}
