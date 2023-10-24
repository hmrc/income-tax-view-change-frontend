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

package views.notEnrolled

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

    s"have the title ${messages("htmlTitle.errorPage", messages("not_enrolled.heading"))}" in {
      document.title() shouldBe messages("htmlTitle.errorPage", messages("not_enrolled.heading"))
    }

    s"have the H1 ${messages("not_enrolled.heading")}" in {
      document.getElementsByTag("H1").text() shouldBe messages("not_enrolled.heading")
    }

    s"have a paragraph" which {

      "has the text" in {
        document.getElementById("sign-up").text() shouldBe  s"${messages("not_enrolled.sign-up")} ${messages("not_enrolled.sign-up.link")} ${messages("pagehelp.opensInNewTabText")}."
      }

      "has a link to sign-in page" in {
        document.getElementById("sign-up-link").attr("href") shouldBe mockAppConfig.signUpUrl
      }

    }

  }

}
