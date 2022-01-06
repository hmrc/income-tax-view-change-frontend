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

package views.notEnrolled

import testConstants.MessagesLookUp.{NotEnrolled => notEnrolledMessages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.notEnrolled.NotEnrolled

class NotEnrolledViewSpec extends TestSupport {

  lazy val mockAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val page = app.injector.instanceOf[NotEnrolled]

  lazy val document = Jsoup.parse(contentAsString(page()))

  "The Not Enrolled view" should {

    s"have the title '${notEnrolledMessages.title}'" in {
      document.title() shouldBe notEnrolledMessages.title
    }

    s"have the H1 '${notEnrolledMessages.heading}'" in {
      document.getElementsByTag("H1").text() shouldBe notEnrolledMessages.heading
    }

    s"have a paragraph" which {

      "has the text" in {
        document.getElementById("sign-up").text() shouldBe notEnrolledMessages.signUp
      }

      "has a link to sign-in page" in {
        document.getElementById("sign-up-link").attr("href") shouldBe mockAppConfig.signUpUrl
      }

    }

  }

}
