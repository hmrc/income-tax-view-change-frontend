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

package views

import assets.Messages.{Timeout => messages}
import config.MockAppConfig
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SessionTimeoutViewSpec extends PlaySpec with GuiceOneServerPerSuite {

  lazy val page = views.html.timeout.timeout()(FakeRequest(), applicationMessages, MockAppConfig)
  lazy val document = Jsoup.parse(contentAsString(page))

  "The Session timeout view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the H1 '${messages.heading}'" in {
      document.getElementsByTag("H1").text() mustBe messages.heading
    }

    s"have a paragraph" which {

      "has the text" in {
        document.getElementById("sign-in").text() mustBe messages.signIn
      }

      // TODO: Update with the Home Controller route which will re-direct to Sign-In
      "has a link to sign-in page" in {
        document.getElementById("sign-in-link").attr("href") mustBe controllers.routes.HelloWorld.helloWorld().url
      }

    }

  }

}
